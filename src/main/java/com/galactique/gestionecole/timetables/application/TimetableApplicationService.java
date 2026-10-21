package com.galactique.gestionecole.timetables.application;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassSubjectRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.schoolclasses.application.SchoolClassOperationalityService;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.timetables.domain.NonWorkingDay;
import com.galactique.gestionecole.timetables.domain.TimetableEntry;
import com.galactique.gestionecole.timetables.domain.TimetableRepository;

@Service
public class TimetableApplicationService {
	private static final Comparator<TimetableEntry> ORDER = Comparator.comparing(TimetableEntry::dayOfWeek)
		.thenComparing(TimetableEntry::startTime).thenComparing(TimetableEntry::id);
	private final TimetableRepository repository;
	private final UserAccountRepository accounts;
	private final EmployeeRepository employees;
	private final EstablishmentRepository schools;
	private final AcademicYearRepository years;
	private final SchoolClassRepository classes;
	private final ClassroomRepository classrooms;
	private final SubjectRepository subjects;
	private final ClassSubjectRepository assignments;
	private final LicenseApplicationService licenses;
	private final SchoolClassOperationalityService schoolClassOperationalityService;

	public TimetableApplicationService(TimetableRepository repository, UserAccountRepository accounts,
		EmployeeRepository employees, EstablishmentRepository schools, AcademicYearRepository years,
		SchoolClassRepository classes, ClassroomRepository classrooms, SubjectRepository subjects,
		ClassSubjectRepository assignments, LicenseApplicationService licenses,
		SchoolClassOperationalityService schoolClassOperationalityService) {
		this.repository = repository; this.accounts = accounts; this.employees = employees; this.schools = schools;
		this.years = years; this.classes = classes; this.classrooms = classrooms; this.subjects = subjects;
		this.assignments = assignments; this.licenses = licenses;
		this.schoolClassOperationalityService = schoolClassOperationalityService;
	}

	@Transactional(readOnly = true)
	public List<TimetableEntry> list(UUID school, UUID year, UUID schoolClass, UUID classroom, DayOfWeek day, String actor) {
		requireScope(school, actor, false);
		context(school, year, schoolClass, classroom, false);
		return entries(school, classroom).stream().filter(entry -> day == null || entry.dayOfWeek() == day).toList();
	}

	@Transactional(readOnly = true)
	public TimetableEntry get(UUID school, UUID year, UUID schoolClass, UUID classroom, UUID entryId, String actor) {
		return list(school, year, schoolClass, classroom, null, actor).stream().filter(entry -> entry.id().equals(entryId))
			.findFirst().orElseThrow(TimetableNotFoundException::new);
	}

	@Transactional
	public TimetableEntry save(UUID school, UUID year, UUID schoolClass, UUID classroom, UUID entryId,
		TimetableEntryCommand command, String actor) {
		requireScope(school, actor, true);
		AcademicYear academicYear = context(school, year, schoolClass, classroom, true);
		var all = repository.findActiveByEstablishment(school);
		TimetableEntry current = entryId == null ? null : find(all, classroom, entryId);
		var candidate = candidate(school, year, schoolClass, classroom, command, academicYear, current, null, actor);
		ensureNoConflict(candidate, all.stream().filter(e -> !e.id().equals(candidate.id())).toList());
		return repository.save(candidate);
	}

	@Transactional
	public void archive(UUID school, UUID year, UUID schoolClass, UUID classroom, UUID entryId, String actor) {
		requireScope(school, actor, true);
		context(school, year, schoolClass, classroom, true);
		repository.archive(find(repository.findActiveByEstablishment(school), classroom, entryId), actor);
	}

	@Transactional
	public List<TimetableEntry> copy(UUID school, UUID year, UUID schoolClass, UUID sourceRoom,
		CopyTimetableCommand command, String actor) {
		requireScope(school, actor, true);
		context(school, year, schoolClass, sourceRoom, false);
		if (sourceRoom.equals(command.targetClassroomId())) {
			throw new TimetableValidationException("Source and target classrooms must be different");
		}
		var target = classrooms.findInYear(school, year, command.targetClassroomId()).orElseThrow(TimetableNotFoundException::new);
		java.util.stream.Stream.of(schoolClass, target.schoolClassId()).distinct().sorted().forEach(id ->
			classes.findForUpdate(id, school, year).orElseThrow(TimetableNotFoundException::new));
		context(school, year, schoolClass, sourceRoom, false);
		AcademicYear academicYear = context(school, year, target.schoolClassId(), target.id(), false);
		var all = repository.findActiveByEstablishment(school);
		var source = all.stream().filter(e -> e.classroomId().equals(sourceRoom)).sorted(ORDER).toList();
		if (source.isEmpty()) { throw new TimetableValidationException("The source timetable is empty"); }
		var sourceTeachers = source.stream().map(TimetableEntry::teacherId).filter(java.util.Objects::nonNull).toList();
		if (!sourceTeachers.containsAll(command.teacherMappings().keySet())) {
			throw new TimetableValidationException("Every teacher mapping must reference a teacher from the source timetable");
		}
		var targetEntries = all.stream().filter(e -> e.classroomId().equals(target.id())).toList();
		List<TimetableEntry> reserved = new ArrayList<>(all.stream()
			.filter(e -> !command.replaceExisting() || !e.classroomId().equals(target.id())).toList());
		List<TimetableEntry> copies = new ArrayList<>();
		for (TimetableEntry entry : source) {
			UUID teacher = entry.teacherId() == null ? null : command.teacherMappings().getOrDefault(entry.teacherId(),
				command.copyTeachers() ? entry.teacherId() : null);
			var input = new TimetableEntryCommand(entry.subjectId(), teacher, entry.dayOfWeek(), entry.startTime(), entry.endTime(),
				command.validFrom() == null ? entry.validFrom() : command.validFrom(),
				command.validUntil() == null ? entry.validUntil() : command.validUntil(), command.copyRoomCodes() ? entry.roomCode() : null);
			TimetableEntry candidate = candidate(school, year, target.schoolClassId(), target.id(), input, academicYear, null, entry.id(), actor);
			ensureNoConflict(candidate, reserved);
			reserved.add(candidate); copies.add(candidate);
		}
		// Validate the complete copy before replacing anything in the destination.
		if (command.replaceExisting()) { targetEntries.forEach(entry -> repository.archive(entry, actor)); }
		copies.forEach(repository::save);
		return List.copyOf(copies);
	}

	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public DaySchedule day(UUID school, UUID year, UUID schoolClass, UUID classroom, LocalDate date, String actor) {
		requireScope(school, actor, false);
		AcademicYear academicYear = context(school, year, schoolClass, classroom, false);
		validateDate(date, academicYear);
		return buildDay(date, academicYear, entries(school, classroom), repository.findNonWorkingDays(school, year));
	}

	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public WeekSchedule week(UUID school, UUID year, UUID schoolClass, UUID classroom, LocalDate date, String actor) {
		requireScope(school, actor, false);
		AcademicYear academicYear = context(school, year, schoolClass, classroom, false);
		validateDate(date, academicYear);
		LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		var entries = entries(school, classroom);
		var daysOff = repository.findNonWorkingDays(school, year);
		return new WeekSchedule(start, start.plusDays(6), start.datesUntil(start.plusDays(7))
			.map(day -> buildDay(day, academicYear, entries, daysOff)).toList());
	}

	@Transactional(readOnly = true)
	public List<NonWorkingDay> nonWorkingDays(UUID school, UUID year, String actor) {
		requireScope(school, actor, false);
		year(school, year);
		return repository.findNonWorkingDays(school, year);
	}

	@Transactional
	public List<NonWorkingDay> replaceNonWorkingDays(UUID school, UUID year, List<NonWorkingDay> days, String actor) {
		requireScope(school, actor, true);
		AcademicYear academicYear = year(school, year);
		HashSet<LocalDate> seen = new HashSet<>();
		for (NonWorkingDay day : days) {
			validateDate(day.date(), academicYear);
			if (!seen.add(day.date()) || day.name() == null || day.name().isBlank() || day.name().strip().length() > 100) {
				throw new TimetableValidationException("Non-working days require unique dates and a name of at most 100 characters");
			}
		}
		var normalized = days.stream().map(day -> new NonWorkingDay(day.date(), day.name().strip()))
			.sorted(Comparator.comparing(NonWorkingDay::date)).toList();
		repository.replaceNonWorkingDays(school, year, normalized, actor);
		return normalized;
	}

	private TimetableEntry candidate(UUID school, UUID year, UUID schoolClass, UUID classroom, TimetableEntryCommand c,
		AcademicYear academicYear, TimetableEntry current, UUID copiedFrom, String actor) {
		if (c.dayOfWeek() == null || c.startTime() == null || c.endTime() == null || !c.endTime().isAfter(c.startTime())
			|| c.startTime().getSecond() != 0 || c.endTime().getSecond() != 0 || c.startTime().getNano() != 0 || c.endTime().getNano() != 0) {
			throw new TimetableValidationException("Start/end times must be whole minutes within the same day, with end after start");
		}
		LocalDate from = c.validFrom() == null ? academicYear.startsOn() : c.validFrom();
		LocalDate until = c.validUntil() == null ? academicYear.endsOn() : c.validUntil();
		validateDate(from, academicYear); validateDate(until, academicYear);
		if (until.isBefore(from) || from.with(TemporalAdjusters.nextOrSame(c.dayOfWeek())).isAfter(until)) {
			throw new TimetableValidationException("The date range must include an occurrence of the selected weekday");
		}
		subjects.findByIdAndEstablishmentId(c.subjectId(), school).filter(s -> s.enabled()).orElseThrow(TimetableNotFoundException::new);
		assignments.findByEstablishmentIdAndClassIdAndSubjectId(school, schoolClass, c.subjectId())
			.filter(a -> a.enabled()).orElseThrow(TimetableNotFoundException::new);
		if (c.teacherId() != null) {
			employees.findById(c.teacherId()).filter(e -> e.enabled() && e.establishmentId().equals(school))
				.orElseThrow(TimetableNotFoundException::new);
		}
		String room = c.roomCode() == null || c.roomCode().isBlank() ? null : c.roomCode().strip().toUpperCase(Locale.ROOT);
		if (room != null && room.length() > 40) { throw new TimetableValidationException("roomCode must not exceed 40 characters"); }
		Instant now = Instant.now();
		return new TimetableEntry(current == null ? UUID.randomUUID() : current.id(), school, year, schoolClass, classroom,
			c.subjectId(), c.teacherId(), c.dayOfWeek(), c.startTime(), c.endTime(), from, until, room, true,
			current == null ? copiedFrom : current.copiedFromId(), current == null ? actor : current.createdBy(), actor,
			current == null ? now : current.createdAt(), now);
	}

	private void requireScope(UUID school, String actor, boolean lock) {
		var account = accounts.findByUsername(actor).filter(a -> a.enabled() && !a.locked()).orElseThrow(TimetableAccessDeniedException::new);
		var employee = employees.findByUserAccountIdAndEstablishmentId(account.id(), school).filter(e -> e.enabled() && account.id().equals(e.userAccountId()))
			.orElseThrow(TimetableAccessDeniedException::new);
		if (!employee.establishmentId().equals(school)) { throw new TimetableNotFoundException(); }
		var establishment = (lock ? schools.findByIdForUpdate(school) : schools.findById(school))
			.filter(s -> s.enabled()).orElseThrow(TimetableNotFoundException::new);
		if (!licenses.canAccessFeature(establishment.id(), LicenseFeature.SCHOOL_MANAGEMENT, Instant.now())) {
			throw new TimetableAccessDeniedException();
		}
	}

	private AcademicYear context(UUID school, UUID year, UUID schoolClass, UUID classroom, boolean lock) {
		AcademicYear academicYear = year(school, year);
		try {
			schoolClassOperationalityService.requireActiveClassroom(school, year, schoolClass);
		} catch (com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException exception) {
			throw new TimetableNotFoundException();
		}
		(lock ? classes.findForUpdate(schoolClass, school, year)
			: classes.findByIdAndEstablishmentIdAndAcademicYearId(schoolClass, school, year))
			.orElseThrow(TimetableNotFoundException::new);
		classrooms.findById(school, year, schoolClass, classroom).orElseThrow(TimetableNotFoundException::new);
		return academicYear;
	}
	private AcademicYear year(UUID school, UUID year) {
		return years.findByIdAndEstablishmentId(year, school).filter(y -> y.enabled()).orElseThrow(TimetableNotFoundException::new);
	}
	private List<TimetableEntry> entries(UUID school, UUID classroom) {
		return repository.findActiveByEstablishment(school).stream().filter(e -> e.classroomId().equals(classroom)).sorted(ORDER).toList();
	}
	private TimetableEntry find(List<TimetableEntry> entries, UUID classroom, UUID id) {
		return entries.stream().filter(e -> e.classroomId().equals(classroom) && e.id().equals(id)).findFirst()
			.orElseThrow(TimetableNotFoundException::new);
	}
	private void ensureNoConflict(TimetableEntry candidate, List<TimetableEntry> reserved) {
		for (TimetableEntry existing : reserved) {
			if (candidate.conflictsWith(existing)) { throw new TimetableConflictException(existing.id()); }
		}
	}
	private void validateDate(LocalDate date, AcademicYear year) {
		if (date == null || date.isBefore(year.startsOn()) || date.isAfter(year.endsOn())) {
			throw new TimetableValidationException("Date must fall within the academic year");
		}
	}
	private DaySchedule buildDay(LocalDate date, AcademicYear year, List<TimetableEntry> entries, List<NonWorkingDay> daysOff) {
		boolean within = !date.isBefore(year.startsOn()) && !date.isAfter(year.endsOn());
		String closed = daysOff.stream().filter(d -> d.date().equals(date)).map(NonWorkingDay::name).findFirst().orElse(null);
		return new DaySchedule(date, within, closed, !within || closed != null ? List.of()
			: entries.stream().filter(e -> e.occursOn(date)).sorted(Comparator.comparing(TimetableEntry::startTime)).toList());
	}
	public record DaySchedule(LocalDate date, boolean withinAcademicYear, String nonWorkingDay, List<TimetableEntry> entries) { }
	public record WeekSchedule(LocalDate startsOn, LocalDate endsOn, List<DaySchedule> days) { }
}
