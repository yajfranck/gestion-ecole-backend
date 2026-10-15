package com.galactique.gestionecole.establishments.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablishmentJpaRepository extends JpaRepository<EstablishmentEntity, UUID> {
}