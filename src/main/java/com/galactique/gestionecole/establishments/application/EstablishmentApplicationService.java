package com.galactique.gestionecole.establishments.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.application.EmployeeNotFoundException;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;

@Service
public class EstablishmentApplicationService {
	private final EstablishmentRepository establishmentRepository;
	private final EstablishmentEmployeeDeactivation employeeDeactivation;
	private final EmployeeApplicationService employeeApplicationService;

	public EstablishmentApplicationService(
		EstablishmentRepository establishmentRepository,
		EstablishmentEmployeeDeactivation employeeDeactivation,
		EmployeeApplicationService employeeApplicationService) {
		this.establishmentRepository = establishmentRepository;
		this.employeeDeactivation = employeeDeactivation;
		this.employeeApplicationService = employeeApplicationService;
	}

	@Transactional
	public Establishment create(String code, String name) {
		return establishmentRepository.save(code.strip().toUpperCase(Locale.ROOT), name.strip());
	}

	@Transactional(readOnly = true)
	public List<Establishment> findAll() {
		return establishmentRepository.findAll();
	}

	@Transactional
	public Establishment lockForLicenseManagement(UUID establishmentId) {
		return establishmentRepository.findByIdForUpdate(establishmentId)
			.orElseThrow(EstablishmentNotFoundException::new);
	}

	@Transactional
	public Establishment update(UUID establishmentId, UpdateEstablishmentCommand command,
		String username, boolean globalAdministrator) {
		Establishment current = establishmentRepository.findById(establishmentId)
			.filter(Establishment::enabled)
			.orElseThrow(EstablishmentNotFoundException::new);
		if (!globalAdministrator) {
			try {
				var employee = employeeApplicationService.findCurrent(username);
				if (employee.userAccountId() == null || !employee.establishmentAdmin()
					|| !establishmentId.equals(employee.establishmentId())) {
					throw new EmployeeAccessDeniedException();
				}
			} catch (EmployeeNotFoundException exception) {
				throw new EmployeeAccessDeniedException();
			}
		}

		Establishment updated = new Establishment(current.id(),
			command.code() == null ? current.code() : command.code(),
			command.name() == null ? current.name() : command.name(),
			current.enabled(), current.createdAt(), current.updatedAt());
		try {
			return establishmentRepository.update(establishmentId, updated.code(), updated.name());
		} catch (DataIntegrityViolationException exception) {
			throw new EstablishmentConflictException();
		}
	}

	@Transactional
	public void deactivate(UUID id) {
		Establishment establishment = establishmentRepository.findById(id)
			.orElseThrow(EstablishmentNotFoundException::new);
		if (establishment.enabled()) {
			establishmentRepository.disable(id);
			employeeDeactivation.disableEmployeeForEstablishment(id);
		}
	}
}