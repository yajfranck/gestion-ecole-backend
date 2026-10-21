package com.galactique.gestionecole.guardians.api.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.guardians.api.dto.GuardianAttendanceHistoryResponse;
import com.galactique.gestionecole.guardians.api.dto.GuardianChildSummaryResponse;
import com.galactique.gestionecole.guardians.api.dto.GuardianPaymentResponse;
import com.galactique.gestionecole.guardians.api.dto.GuardianTuitionFeeResponse;
import com.galactique.gestionecole.guardians.application.GuardianChildPortalApplicationService;
import com.galactique.gestionecole.learning.api.dto.LearningAttachmentResponse;
import com.galactique.gestionecole.learning.api.dto.LearningContentPageResponse;
import com.galactique.gestionecole.learning.api.dto.LearningContentResponse;
import com.galactique.gestionecole.learning.domain.LearningContentType;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/guardians/me/children/{studentId}/school-years/{academicYearId}")
public class GuardianChildPortalController {
	private final GuardianChildPortalApplicationService guardianChildPortalApplicationService;

	public GuardianChildPortalController(GuardianChildPortalApplicationService guardianChildPortalApplicationService) {
		this.guardianChildPortalApplicationService = guardianChildPortalApplicationService;
	}

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public GuardianChildSummaryResponse getChildSummary(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return GuardianChildSummaryResponse.from(guardianChildPortalApplicationService.findSummary(
			authentication.getName(), establishmentId, studentId, academicYearId));
	}

	@GetMapping("/tuition-fees")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public List<GuardianTuitionFeeResponse> getChildFees(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return guardianChildPortalApplicationService.findFees(authentication.getName(), establishmentId, studentId, academicYearId)
			.stream().map(GuardianTuitionFeeResponse::from).toList();
	}

	@GetMapping("/tuition-fees/{chargeId}/payments")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public List<GuardianPaymentResponse> getChargePayments(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, @PathVariable UUID chargeId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		Authentication authentication) {
		return guardianChildPortalApplicationService.findPayments(
			authentication.getName(), establishmentId, studentId, academicYearId, chargeId, from, to)
			.stream().map(GuardianPaymentResponse::from).toList();
	}

	@GetMapping("/attendance")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public GuardianAttendanceHistoryResponse getChildAttendance(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		Authentication authentication) {
		return GuardianAttendanceHistoryResponse.from(guardianChildPortalApplicationService.findAttendance(
			authentication.getName(), establishmentId, studentId, academicYearId, from, to));
	}

	@GetMapping("/learning-content")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public LearningContentPageResponse getLearningContent(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, @RequestParam(required = false) LearningContentType type,
		@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
		Authentication authentication) {
		return LearningContentPageResponse.from(guardianChildPortalApplicationService.findLearningContent(
			authentication.getName(), establishmentId, studentId, academicYearId, type, page, size));
	}

	@GetMapping("/learning-content/{contentId}")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public LearningContentResponse getLearningContent(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, @PathVariable UUID contentId, Authentication authentication) {
		return LearningContentResponse.from(guardianChildPortalApplicationService.getLearningContent(
			authentication.getName(), establishmentId, studentId, academicYearId, contentId));
	}

	@GetMapping("/learning-content/{contentId}/attachments")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public List<LearningAttachmentResponse> getLearningAttachments(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, @PathVariable UUID contentId, Authentication authentication) {
		return guardianChildPortalApplicationService.findLearningAttachments(authentication.getName(), establishmentId,
			studentId, academicYearId, contentId).stream().map(LearningAttachmentResponse::from).toList();
	}

	@GetMapping("/learning-content/{contentId}/attachments/{attachmentId}")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public ResponseEntity<InputStreamResource> downloadLearningAttachment(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID academicYearId, @PathVariable UUID contentId, @PathVariable UUID attachmentId,
		Authentication authentication) {
		var download = guardianChildPortalApplicationService.downloadLearningAttachment(authentication.getName(), establishmentId,
			studentId, academicYearId, contentId, attachmentId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.metadata().mediaType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
				.filename(download.metadata().fileName(), StandardCharsets.UTF_8).build().toString())
			.contentLength(download.metadata().size()).body(new InputStreamResource(download.stream()));
	}
}
