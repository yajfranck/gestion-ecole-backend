package com.galactique.gestionecole.establishments.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;

@Service
public class EstablishmentApplicationService {
	private final EstablishmentRepository establishmentRepository;
	private final EstablishmentEmployeeDeactivation employeeDeactivation;

	public EstablishmentApplicationService(
		EstablishmentRepository establishmentRepository,
		EstablishmentEmployeeDeactivation employeeDeactivation) {
		this.establishmentRepository = establishmentRepository;
		this.employeeDeactivation = employeeDeactivation;
	}

	@Transactional
	public Establishment create(String code, String name) {
		return establishmentRepository.save(code, name);
	}

	@Transactional(readOnly = true)
	public List<Establishment> findAll() {
		return establishmentRepository.findAll();
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