package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.licenses.domain.LicenseEvent;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseOffer;
import com.galactique.gestionecole.licenses.domain.LicenseRepository;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;

@Repository
public class JpaLicenseRepositoryAdapter implements LicenseRepository {
	private final LicenseOfferJpaRepository offerRepository;
	private final PlatformLicenseJpaRepository licenseRepository;
	private final LicenseEventJpaRepository eventRepository;

	public JpaLicenseRepositoryAdapter(LicenseOfferJpaRepository offerRepository,
		PlatformLicenseJpaRepository licenseRepository, LicenseEventJpaRepository eventRepository) {
		this.offerRepository = offerRepository;
		this.licenseRepository = licenseRepository;
		this.eventRepository = eventRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LicenseOffer> findOffers() {
		return offerRepository.findAllByEnabledTrueOrderByCodeAsc().stream()
			.map(LicenseOfferEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LicenseOffer> findEnabledOffer(UUID offerId) {
		return offerRepository.findByIdAndEnabledTrue(offerId).map(LicenseOfferEntity::toDomain);
	}

	@Override
	@Transactional
	public LicenseOffer createOffer(String code, String name, Set<LicenseFeature> features) {
		return offerRepository.saveAndFlush(new LicenseOfferEntity(code, name, features)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlatformLicense> findLicenses(UUID establishmentId) {
		return licenseRepository.findAllByEstablishmentIdOrderByValidFromDesc(establishmentId).stream()
			.map(PlatformLicenseEntity::toDomain).toList();
	}

	@Override
	@Transactional
	public List<PlatformLicense> findLicensesForUpdate(UUID establishmentId) {
		return licenseRepository.findAllForUpdate(establishmentId).stream()
			.map(PlatformLicenseEntity::toDomain).toList();
	}

	@Override
	@Transactional
	public PlatformLicense createLicense(UUID establishmentId, LicenseOffer offer,
		Instant from, Instant until, String actor) {
		LicenseOfferEntity offerEntity = offerRepository.findByIdAndEnabledTrue(offer.id()).orElseThrow();
		return licenseRepository.saveAndFlush(new PlatformLicenseEntity(
			establishmentId, offerEntity, from, until, actor)).toDomain();
	}

	@Override
	@Transactional
	public PlatformLicense endLicense(PlatformLicense license, Instant validUntil, String actor,
		String action, String details) {
		PlatformLicenseEntity entity = licenseRepository.findByIdAndEstablishmentId(
			license.id(), license.establishmentId()).orElseThrow();
		entity.setValidUntil(validUntil);
		PlatformLicense updated = entity.toDomain();
		saveEvent(updated, action, actor, details);
		return updated;
	}

	@Override
	@Transactional
	public PlatformLicense suspendLicense(PlatformLicense license, String actor) {
		PlatformLicenseEntity entity = licenseRepository.findByIdAndEstablishmentId(
			license.id(), license.establishmentId()).orElseThrow();
		entity.suspend();
		PlatformLicense updated = entity.toDomain();
		saveEvent(updated, "SUSPENDED", actor, null);
		return updated;
	}

	@Override
	@Transactional
	public PlatformLicense reactivateLicense(PlatformLicense license, String actor) {
		PlatformLicenseEntity entity = licenseRepository.findByIdAndEstablishmentId(
			license.id(), license.establishmentId()).orElseThrow();
		entity.reactivate();
		PlatformLicense updated = entity.toDomain();
		saveEvent(updated, "REACTIVATED", actor, null);
		return updated;
	}

	@Override
	@Transactional
	public void recordEvent(PlatformLicense license, String action, String actor, String details) {
		saveEvent(license, action, actor, details);
	}

	private void saveEvent(PlatformLicense license, String action, String actor, String details) {
		eventRepository.save(new LicenseEventEntity(license.id(), action, actor, details));
	}

	@Override
	@Transactional(readOnly = true)
	public List<LicenseEvent> findEvents(Long licenseId) {
		return eventRepository.findAllByLicenseIdOrderByCreatedAtAsc(licenseId).stream()
			.map(LicenseEventEntity::toDomain).toList();
	}
}
