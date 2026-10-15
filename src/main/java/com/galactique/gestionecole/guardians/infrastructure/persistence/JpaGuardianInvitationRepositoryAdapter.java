package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.guardians.domain.GuardianInvitation;
import com.galactique.gestionecole.guardians.domain.GuardianInvitationRepository;

@Repository
public class JpaGuardianInvitationRepositoryAdapter implements GuardianInvitationRepository {
	private final GuardianInvitationJpaRepository jpaRepository;

	public JpaGuardianInvitationRepositoryAdapter(GuardianInvitationJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public GuardianInvitation save(GuardianInvitation invitation) {
		return jpaRepository.save(new GuardianInvitationEntity(invitation)).toDomain();
	}

	@Override
	@Transactional
	public Optional<GuardianInvitation> findByTokenHashForUpdate(String tokenHash) {
		return jpaRepository.findByTokenHashForUpdate(tokenHash).map(invitation -> invitation.toDomain());
	}

	@Override
	@Transactional
	public void markAccepted(GuardianInvitation invitation) {
		jpaRepository.findById(invitation.id()).orElseThrow().markAccepted(invitation.acceptedAt());
	}
}