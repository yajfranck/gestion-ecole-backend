package com.galactique.gestionecole.schoolclasses.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.schoolclasses.domain.AcademicPeriod;
import com.galactique.gestionecole.schoolclasses.domain.AcademicPeriodRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;

@Service
public class SchoolCalendarApplicationService {
	private final AcademicYearRepository academicYearRepository;
	private final AcademicPeriodRepository academicPeriodRepository;
	private final EmployeeApplicationService employeeApplicationService;

	public SchoolCalendarApplicationService(
		AcademicYearRepository academicYearRepository,
		AcademicPeriodRepository academicPeriodRepository,
		EmployeeApplicationService employeeApplicationService) {
		this.academicYearRepository = academicYearRepository;
		this.academicPeriodRepository = academicPeriodRepository;
		this.employeeApplicationService = employeeApplicationService;
	}

	@Transactional(readOnly = true)
	public List<AcademicYear> findYears(UUID establishmentId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		return academicYearRepository.findAllByEstablishmentId(establishmentId).stream()
			.filter(AcademicYear::enabled)
			.toList();
	}

	@Transactional
	public AcademicYear createYear(UUID establishmentId, CreateAcademicYearCommand command, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		if (academicYearRepository.existsByEstablishmentIdAndName(establishmentId, command.name().strip())) {
			throw new AcademicCalendarConflictException();
		}
		try {
			return academicYearRepository.save(establishmentId, command.name(), command.startsOn(), command.endsOn());
		} catch (DataIntegrityViolationException exception) {
			throw new AcademicCalendarConflictException();
		}
	}

	@Transactional(readOnly = true)
	public AcademicYear findYear(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		return findEnabledYear(academicYearId, establishmentId);
	}

	@Transactional
	public AcademicYear updateYear(UUID establishmentId, UUID academicYearId,
		UpdateAcademicYearCommand command, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		AcademicYear current = findEnabledYear(academicYearId, establishmentId);
		String name = command.name() == null ? current.name() : command.name().strip();
		LocalDate startsOn = command.startsOn() == null ? current.startsOn() : command.startsOn();
		LocalDate endsOn = command.endsOn() == null ? current.endsOn() : command.endsOn();
		if (academicYearRepository.existsByEstablishmentIdAndNameNot(establishmentId, name, academicYearId)) {
			throw new AcademicCalendarConflictException();
		}
		for (AcademicPeriod period : academicPeriodRepository.findAllByAcademicYearId(academicYearId)) {
			if (period.enabled() && !isWithin(period.startsOn(), period.endsOn(), startsOn, endsOn)) {
				throw new IllegalArgumentException("Academic year dates must contain all active periods");
			}
		}
		try {
			return academicYearRepository.update(academicYearId, establishmentId, name, startsOn, endsOn);
		} catch (DataIntegrityViolationException exception) {
			throw new AcademicCalendarConflictException();
		}
	}

	@Transactional
	public void disableYear(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		findEnabledYear(academicYearId, establishmentId);
		academicYearRepository.disable(academicYearId, establishmentId);
	}

	@Transactional(readOnly = true)
	public List<AcademicPeriod> findPeriods(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		findEnabledYear(academicYearId, establishmentId);
		return academicPeriodRepository.findAllByAcademicYearId(academicYearId).stream()
			.filter(AcademicPeriod::enabled)
			.toList();
	}

	@Transactional
	public AcademicPeriod createPeriod(UUID establishmentId, UUID academicYearId,
		CreateAcademicPeriodCommand command, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		AcademicYear academicYear = findEnabledYear(academicYearId, establishmentId);
		validatePeriodDates(command.startsOn(), command.endsOn(), academicYear, academicYearId, null);
		if (academicPeriodRepository.existsByAcademicYearIdAndName(academicYearId, command.name().strip())) {
			throw new AcademicCalendarConflictException();
		}
		try {
			return academicPeriodRepository.save(academicYearId, command.name(), command.startsOn(), command.endsOn());
		} catch (DataIntegrityViolationException exception) {
			throw new AcademicCalendarConflictException();
		}
	}

	@Transactional
	public AcademicPeriod updatePeriod(UUID establishmentId, UUID academicYearId, UUID periodId,
		UpdateAcademicPeriodCommand command, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		AcademicYear academicYear = findEnabledYear(academicYearId, establishmentId);
		AcademicPeriod current = academicPeriodRepository.findByIdAndAcademicYearId(periodId, academicYearId)
			.filter(AcademicPeriod::enabled)
			.orElseThrow(AcademicCalendarNotFoundException::new);
		String name = command.name() == null ? current.name() : command.name().strip();
		LocalDate startsOn = command.startsOn() == null ? current.startsOn() : command.startsOn();
		LocalDate endsOn = command.endsOn() == null ? current.endsOn() : command.endsOn();
		validatePeriodDates(startsOn, endsOn, academicYear, academicYearId, periodId);
		if (academicPeriodRepository.existsByAcademicYearIdAndNameNot(academicYearId, name, periodId)) {
			throw new AcademicCalendarConflictException();
		}
		try {
			return academicPeriodRepository.update(periodId, academicYearId, name, startsOn, endsOn);
		} catch (DataIntegrityViolationException exception) {
			throw new AcademicCalendarConflictException();
		}
	}

	@Transactional
	public void disablePeriod(UUID establishmentId, UUID academicYearId, UUID periodId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		findEnabledYear(academicYearId, establishmentId);
		academicPeriodRepository.findByIdAndAcademicYearId(periodId, academicYearId)
			.filter(AcademicPeriod::enabled)
			.orElseThrow(AcademicCalendarNotFoundException::new);
		academicPeriodRepository.disable(periodId, academicYearId);
	}

	private AcademicYear findEnabledYear(UUID academicYearId, UUID establishmentId) {
		return academicYearRepository.findByIdAndEstablishmentId(academicYearId, establishmentId)
			.filter(AcademicYear::enabled)
			.orElseThrow(AcademicCalendarNotFoundException::new);
	}

	private void validatePeriodDates(LocalDate startsOn, LocalDate endsOn, AcademicYear year,
		UUID academicYearId, UUID excludedPeriodId) {
		if (startsOn == null || endsOn == null || !isWithin(startsOn, endsOn, year.startsOn(), year.endsOn())) {
			throw new IllegalArgumentException("Academic period dates must be within its academic year");
		}
		for (AcademicPeriod existing : academicPeriodRepository.findAllByAcademicYearId(academicYearId)) {
			if (!existing.enabled() || existing.id().equals(excludedPeriodId)) {
				continue;
			}
			boolean overlaps = !endsOn.isBefore(existing.startsOn()) && !startsOn.isAfter(existing.endsOn());
			if (overlaps) {
				throw new AcademicCalendarConflictException();
			}
		}
	}

	private boolean isWithin(LocalDate start, LocalDate end, LocalDate outerStart, LocalDate outerEnd) {
		return !end.isBefore(start) && !start.isBefore(outerStart) && !end.isAfter(outerEnd);
	}

	private Establishment requireAccessibleEstablishment(UUID establishmentId, EmployeeActor actor) {
		List<Establishment> establishments;
		try {
			establishments = employeeApplicationService.findEstablishments(actor);
		} catch (EmployeeAccessDeniedException exception) {
			throw new AcademicCalendarAccessDeniedException();
		}
		return establishments.stream()
			.filter(Establishment::enabled)
			.filter(establishment -> establishment.id().equals(establishmentId))
			.findFirst()
			.orElseThrow(AcademicCalendarNotFoundException::new);
	}
}