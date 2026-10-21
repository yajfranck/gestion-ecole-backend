package com.galactique.gestionecole.rollover.application;

import static com.galactique.gestionecole.rollover.application.RolloverException.Reason.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.rollover.domain.*;
import com.galactique.gestionecole.schoolclasses.domain.*;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.tuition.domain.*;

@Service
public class SchoolYearRolloverService {
	private final SchoolYearRolloverRepository repository;
	private final SchoolClassRepository classes;
	private final ClassroomRepository classrooms;
	private final SubjectRepository subjects;
	private final ClassSubjectRepository assignments;
	private final TuitionRepository tuition;
	private final UserAccountRepository accounts;
	private final EmployeeRepository employees;
	private final EstablishmentRepository schools;
	private final LicenseApplicationService licenses;

	public SchoolYearRolloverService(SchoolYearRolloverRepository repository, SchoolClassRepository classes,
		ClassroomRepository classrooms, SubjectRepository subjects, ClassSubjectRepository assignments,
		TuitionRepository tuition, UserAccountRepository accounts, EmployeeRepository employees,
		EstablishmentRepository schools, LicenseApplicationService licenses) {
		this.repository = repository; this.classes = classes; this.classrooms = classrooms; this.subjects = subjects;
		this.assignments = assignments; this.tuition = tuition; this.accounts = accounts;
		this.employees = employees; this.schools = schools; this.licenses = licenses;
	}

	@Transactional(readOnly = true)
	public Preview preview(UUID school, UUID targetYear, String actor) {
		requireScope(school, actor);
		var context = context(school, targetYear);
		boolean finance = licenses.canAccessFeature(school, LicenseFeature.TUITION, Instant.now());
		var rooms = context.source() == null ? List.<RoomConfiguration>of() : repository.findClassrooms(school, context.source().id())
			.stream().map(room -> new RoomConfiguration(room, activeSubjects(school, room.schoolClass().id()))).toList();
		var fees = !finance || context.source() == null ? List.<TuitionFee>of() : configurationFees(school, context.source().id());
		return new Preview(context.target(), context.source(), rooms, finance, fees,
			finance ? tuition.findFeeTypes(school) : List.of(), finance ? tuition.findPaymentMethods(school) : List.of());
	}

	@Transactional
	public Result copy(UUID school, UUID targetYear, RolloverCommand command, String actor) {
		requireScope(school, actor);
		validate(command);
		var context = context(school, targetYear);
		if (context.source() == null) { throw error(NOT_FOUND, "No previous academic year is available"); }
		if (!context.source().id().equals(command.sourceAcademicYearId())) {
			throw error(INVALID, "sourceAcademicYearId must identify the previous available academic year");
		}
		repository.lockYears(school, context.source().id(), targetYear);
		// Recheck dates and enabled state after acquiring the shared year locks.
		context = context(school, targetYear);
		if (context.source() == null || !context.source().id().equals(command.sourceAcademicYearId())) {
			throw error(CONFLICT, "The previous academic year has changed; reload the configuration");
		}
		UUID sourceYear = context.source().id();
		Set<UUID> requested = new HashSet<>(command.classroomIds());
		var sourceRooms = repository.findClassrooms(school, sourceYear).stream()
			.filter(room -> requested.contains(room.classroom().id())).toList();
		if (sourceRooms.size() != requested.size()) { throw error(NOT_FOUND, "A selected classroom does not belong to the source year"); }
		var sourceClasses = new LinkedHashMap<UUID, SchoolClass>();
		sourceRooms.forEach(room -> sourceClasses.put(room.schoolClass().id(), room.schoolClass()));
		var selectedFees = configurationFees(school, sourceYear).stream().filter(fee ->
			fee.schoolClassIds().isEmpty() ? command.includeAnnualFees()
				: fee.schoolClassIds().stream().anyMatch(sourceClasses::containsKey)).toList();
		if (!selectedFees.stream().map(TuitionFee::id).toList().containsAll(command.feeOverrides().keySet())) {
			throw error(INVALID, "feeOverrides must reference configuration fees included in this selection");
		}
		if (!selectedFees.isEmpty() && !licenses.canAccessFeature(school, LicenseFeature.TUITION, Instant.now())) {
			throw error(FORBIDDEN, "TUITION entitlement is required to copy financial configuration");
		}
		var classLinks = repository.classLinks(school, sourceYear, targetYear);
		var roomLinks = repository.classroomLinks(school, sourceYear, targetYear);
		var feeLinks = repository.feeLinks(school, sourceYear, targetYear);
		var targetRooms = repository.findClassrooms(school, targetYear);
		Map<UUID, SchoolClass> targetClasses = new LinkedHashMap<>();
		List<Link> copiedClasses = new ArrayList<>();
		for (var source : sourceClasses.values()) {
			UUID linked = classLinks.get(source.id());
			SchoolClass target = linked == null ? classes.findAllByEstablishmentIdAndAcademicYearId(school, targetYear).stream()
				.filter(c -> c.code().equals(source.code())).findFirst().orElse(null)
				: classes.findByIdAndEstablishmentIdAndAcademicYearId(linked, school, targetYear)
					.orElseThrow(() -> error(CONFLICT, "A previously copied class is archived"));
			if (linked == null && target != null && (!target.name().equals(source.name()) || !Objects.equals(target.series(), source.series()))) {
				throw error(CONFLICT, "An incompatible class already uses code " + source.code());
			}
			boolean created = target == null;
			if (created) {
				if (classes.existsByAcademicYearIdAndCode(targetYear, source.code())) {
					throw error(CONFLICT, "An archived class already uses code " + source.code());
				}
				target = classes.save(school, targetYear, source.code(), source.name(), source.series());
			}
			targetClasses.put(source.id(), target);
			copiedClasses.add(new Link(source.id(), target.id(), created));
		}
		// All mutations involving existing classes acquire their locks in the same order.
		targetClasses.values().stream().map(SchoolClass::id).distinct().sorted().forEach(id ->
			classes.findForUpdate(id, school, targetYear).orElseThrow(() -> error(CONFLICT, "A destination class is archived")));
		for (var entry : targetClasses.entrySet()) {
			if (!classLinks.containsKey(entry.getKey())) {
				repository.linkClass(school, sourceYear, targetYear, entry.getKey(), entry.getValue().id(), actor);
			}
		}
		List<Link> copiedRooms = new ArrayList<>();
		for (var configuration : sourceRooms) {
			var source = configuration.classroom();
			UUID targetClass = targetClasses.get(source.schoolClassId()).id();
			UUID linked = roomLinks.get(source.id());
			Classroom target = linked == null ? targetRooms.stream().map(SourceClassroom::classroom)
				.filter(r -> r.schoolClassId().equals(targetClass) && r.code().equals(source.code())).findFirst().orElse(null)
				: classrooms.findById(school, targetYear, targetClass, linked)
					.orElseThrow(() -> error(CONFLICT, "A previously copied classroom is archived"));
			if (target != null && (!target.enabled() || (linked == null && (!target.name().equals(source.name())
				|| target.maxStudents() != source.maxStudents())))) {
				throw error(CONFLICT, "An incompatible or archived classroom already uses code " + source.code());
			}
			boolean created = target == null;
			if (created) {
				target = classrooms.save(school, targetYear, targetClass, source.code(), source.name(), source.maxStudents());
			}
			if (linked == null) { repository.linkClassroom(source, target, actor); }
			copiedRooms.add(new Link(source.id(), target.id(), created));
		}
		int subjectCount = 0;
		Set<UUID> skipped = new LinkedHashSet<>();
		for (var entry : targetClasses.entrySet()) {
			for (var assignment : assignments.findAllByEstablishmentIdAndClassId(school, entry.getKey())) {
				if (!assignment.enabled()) { continue; }
				var subject = subjects.findByIdAndEstablishmentId(assignment.subjectId(), school).filter(Subject::enabled);
				if (subject.isEmpty()) { skipped.add(assignment.subjectId()); continue; }
				var current = assignments.findByEstablishmentIdAndClassIdAndSubjectId(school, entry.getValue().id(), assignment.subjectId());
				if (current.isEmpty()) { assignments.save(school, entry.getValue().id(), assignment.subjectId()); subjectCount++; }
				else if (!current.get().enabled()) { assignments.enable(current.get().id(), school); subjectCount++; }
			}
		}
		List<Link> copiedFees = new ArrayList<>();
		for (var source : selectedFees) {
			var override = command.feeOverrides().get(source.id());
			LocalDate dueDate = override != null && override.dueDate() != null ? override.dueDate()
				: source.dueDate().plusYears(context.target().startsOn().getYear() - context.source().startsOn().getYear());
			if (dueDate.isBefore(context.target().startsOn()) || dueDate.isAfter(context.target().endsOn())) {
				throw error(INVALID, "Provide a feeOverrides dueDate within the target year for fee " + source.id());
			}
			var type = feeType(school, source, override);
			List<UUID> classIds = source.schoolClassIds().stream().filter(targetClasses::containsKey)
				.map(id -> targetClasses.get(id).id()).distinct().toList();
			UUID linked = feeLinks.get(source.id());
			TuitionFee target = linked == null ? tuition.findFees(school, targetYear).stream()
				.filter(f -> f.name().equals(source.name())).findFirst().orElse(null)
				: tuition.findFee(linked, school, targetYear).orElseThrow(() -> error(CONFLICT, "A previously copied fee is unavailable"));
			if (target != null && (!target.enabled() || target.studentId() != null
				|| target.schoolClassIds().isEmpty() != source.schoolClassIds().isEmpty()
				|| target.amount().compareTo(source.amount()) != 0 || !target.currency().equals(source.currency())
				|| !target.dueDate().equals(dueDate) || !Objects.equals(target.feeTypeId(), type.id()))) {
				throw error(CONFLICT, "An incompatible fee already uses name " + source.name());
			}
			boolean created = target == null;
			if (created) {
				// Save definitions directly: no student charge or payment may be created by a rollover.
				target = tuition.saveFee(new TuitionFeeDraft(school, targetYear, type.id(), type.code(), type.name(), source.name(),
					source.amount(), source.currency(), dueDate, classIds, null));
			} else if (!classIds.isEmpty() && !target.schoolClassIds().containsAll(classIds)) {
				target = tuition.extendFeeClasses(target.id(), school, targetYear, classIds);
			}
			if (linked == null) { repository.linkFee(school, sourceYear, targetYear, source.id(), target.id(), actor); }
			copiedFees.add(new Link(source.id(), target.id(), created));
		}
		return new Result(sourceYear, targetYear, copiedClasses, copiedRooms, subjectCount, List.copyOf(skipped), copiedFees, actor);
	}

	private List<Subject> activeSubjects(UUID school, UUID schoolClass) {
		return assignments.findAllByEstablishmentIdAndClassId(school, schoolClass).stream().filter(SchoolClassSubject::enabled)
			.map(a -> subjects.findByIdAndEstablishmentId(a.subjectId(), school).filter(Subject::enabled))
			.flatMap(Optional::stream).toList();
	}
	private List<TuitionFee> configurationFees(UUID school, UUID year) {
		return tuition.findFees(school, year).stream().filter(f -> f.enabled() && f.studentId() == null).toList();
	}
	private TuitionFeeType feeType(UUID school, TuitionFee source, RolloverCommand.FeeOverride override) {
		UUID id = override != null && override.feeTypeId() != null ? override.feeTypeId() : source.feeTypeId();
		if (id != null) {
			return tuition.findFeeType(id, school).filter(TuitionFeeType::enabled)
				.orElseThrow(() -> error(INVALID, "Provide an active feeTypeId from this establishment for fee " + source.id()));
		}
		return tuition.findFeeTypes(school).stream().filter(t -> t.code().equals(source.feeTypeCode())).findFirst()
			.orElseThrow(() -> error(INVALID, "Provide an active feeTypeId for legacy fee " + source.id()));
	}
	private void validate(RolloverCommand command) {
		if (command == null || command.sourceAcademicYearId() == null || command.classroomIds() == null
			|| command.classroomIds().isEmpty() || command.classroomIds().size() > 200
			|| command.classroomIds().stream().anyMatch(Objects::isNull) || new HashSet<>(command.classroomIds()).size() != command.classroomIds().size()
			|| command.feeOverrides() == null || command.feeOverrides().size() > 200
			|| command.feeOverrides().keySet().stream().anyMatch(Objects::isNull) || command.feeOverrides().values().stream().anyMatch(Objects::isNull)) {
			throw error(INVALID, "A source year, 1 to 200 distinct classrooms and valid fee overrides are required");
		}
	}
	private Context context(UUID school, UUID targetYear) {
		var years = repository.findYears(school);
		var target = years.stream().filter(y -> y.id().equals(targetYear) && y.enabled()).findFirst()
			.orElseThrow(() -> error(NOT_FOUND, "Active target academic year not found"));
		var source = years.stream().filter(y -> y.endsOn().isBefore(target.startsOn()))
			.max(Comparator.comparing(AcademicYear::endsOn).thenComparing(AcademicYear::startsOn).thenComparing(AcademicYear::id)).orElse(null);
		return new Context(target, source);
	}
	private void requireScope(UUID school, String actor) {
		var account = accounts.findByUsername(actor).filter(a -> a.enabled() && !a.locked())
			.orElseThrow(() -> error(FORBIDDEN, "Active staff account required"));
		var employee = employees.findByUserAccountIdAndEstablishmentId(account.id(), school).filter(e -> e.enabled() && account.id().equals(e.userAccountId()))
			.orElseThrow(() -> error(FORBIDDEN, "Active staff account required"));
		if (!employee.establishmentId().equals(school) || schools.findById(school).filter(s -> s.enabled()).isEmpty()) {
			throw error(NOT_FOUND, "Establishment not found");
		}
		if (!licenses.canAccessFeature(school, LicenseFeature.SCHOOL_MANAGEMENT, Instant.now())) {
			throw error(FORBIDDEN, "SCHOOL_MANAGEMENT entitlement required");
		}
	}
	private RolloverException error(RolloverException.Reason reason, String message) { return new RolloverException(reason, message); }
	private record Context(AcademicYear target, AcademicYear source) { }
	public record RoomConfiguration(SourceClassroom source, List<Subject> subjects) { }
	public record Preview(AcademicYear targetYear, AcademicYear sourceYear, List<RoomConfiguration> classrooms,
		boolean financeAvailable, List<TuitionFee> fees, List<TuitionFeeType> feeTypes, List<TuitionPaymentMethod> paymentMethods) { }
	public record Link(UUID sourceId, UUID targetId, boolean created) { }
	public record Result(UUID sourceAcademicYearId, UUID targetAcademicYearId, List<Link> classes, List<Link> classrooms,
		int subjectAssignmentsCreated, List<UUID> skippedSubjectIds, List<Link> fees, String createdBy) { }
}
