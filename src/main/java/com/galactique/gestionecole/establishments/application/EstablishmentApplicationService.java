package com.galactique.gestionecole.establishments.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.security.application.CallerScopeService;

@Service
public class EstablishmentApplicationService {
	private final CallerScopeService callerScope;
	private final EstablishmentRepository establishmentRepository;
	private final EstablishmentEmployeeDeactivation employeeDeactivation;

	public EstablishmentApplicationService(
		EstablishmentRepository establishmentRepository,
		EstablishmentEmployeeDeactivation employeeDeactivation, CallerScopeService callerScope) {
		this.callerScope = callerScope;
		this.establishmentRepository = establishmentRepository;
		this.employeeDeactivation = employeeDeactivation;
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
		String username) {
		Establishment current = establishmentRepository.findById(establishmentId)
			.filter(Establishment::enabled)
			.orElseThrow(EstablishmentNotFoundException::new);
		callerScope.requireEstablishment(username, establishmentId);

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
