package com.galactique.gestionecole.employees.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.EmployeeInvitation;
import com.galactique.gestionecole.employees.domain.EmployeeInvitationRepository;

@Repository
public class JpaEmployeeInvitationRepositoryAdapter implements EmployeeInvitationRepository {
	private final EmployeeInvitationJpaRepository jpaRepository;

	public JpaEmployeeInvitationRepositoryAdapter(EmployeeInvitationJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public EmployeeInvitation save(EmployeeInvitation invitation) {
		return toDomain(jpaRepository.save(new EmployeeInvitationEntity(invitation)));
	}

	@Override
	@Transactional
	public Optional<EmployeeInvitation> findByTokenHashForUpdate(String tokenHash) {
		return jpaRepository.findByTokenHashForUpdate(tokenHash).map(this::toDomain);
	}

	@Override
	@Transactional
	public void markAccepted(EmployeeInvitation invitation) {
		EmployeeInvitationEntity entity = jpaRepository.findById(invitation.id()).orElseThrow();
		entity.markAccepted(invitation.acceptedAt());
	}

	private EmployeeInvitation toDomain(EmployeeInvitationEntity entity) {
		return new EmployeeInvitation(entity.getId(), entity.getEmployeeId(), entity.getTokenHash(),
			entity.getCreatedAt(), entity.getExpiresAt(), entity.getAcceptedAt());
	}
}