package com.galactique.gestionecole.learning.api.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.learning.api.dto.LearningAttachmentResponse;
import com.galactique.gestionecole.learning.api.dto.LearningContentPageResponse;
import com.galactique.gestionecole.learning.api.dto.LearningContentRequest;
import com.galactique.gestionecole.learning.api.dto.LearningContentResponse;
import com.galactique.gestionecole.learning.application.LearningContentApplicationService;
import com.galactique.gestionecole.learning.domain.LearningContentType;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/learning-content")
public class LearningContentController {
	private final LearningContentApplicationService learningContentApplicationService;

	public LearningContentController(LearningContentApplicationService learningContentApplicationService) {
		this.learningContentApplicationService = learningContentApplicationService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('can_learning_content_read')")
	public LearningContentPageResponse list(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId,
		@RequestParam(required = false) LearningContentType type,
		@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
		Authentication authentication) {
		return LearningContentPageResponse.from(learningContentApplicationService.listForStaff(
			establishmentId, academicYearId, schoolClassId, type, page, size, toActor(authentication)));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('can_learning_content_create')")
	public org.springframework.http.ResponseEntity<LearningContentResponse> create(
		@PathVariable UUID establishmentId, @PathVariable UUID academicYearId, @PathVariable UUID schoolClassId,
		@Valid @RequestBody LearningContentRequest request, Authentication authentication) {
		return org.springframework.http.ResponseEntity.status(201).body(LearningContentResponse.from(
			learningContentApplicationService.create(establishmentId, academicYearId, schoolClassId,
				request.toCommand(), toActor(authentication))));
	}

	@GetMapping("/{contentId}")
	@PreAuthorize("hasAuthority('can_learning_content_read')")
	public LearningContentResponse get(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID contentId, Authentication authentication) {
		return LearningContentResponse.from(learningContentApplicationService.getForStaff(
			establishmentId, academicYearId, schoolClassId, contentId, toActor(authentication)));
	}

	@PutMapping("/{contentId}")
	@PreAuthorize("hasAuthority('can_learning_content_update')")
	public LearningContentResponse update(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID contentId,
		@Valid @RequestBody LearningContentRequest request, Authentication authentication) {
		return LearningContentResponse.from(learningContentApplicationService.update(establishmentId,
			academicYearId, schoolClassId, contentId, request.toCommand(), toActor(authentication)));
	}

	@PostMapping("/{contentId}/publish")
	@PreAuthorize("hasAuthority('can_learning_content_publish')")
	public LearningContentResponse publish(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID contentId, Authentication authentication) {
		return LearningContentResponse.from(learningContentApplicationService.publish(establishmentId,
			academicYearId, schoolClassId, contentId, toActor(authentication)));
	}

	@PostMapping("/{contentId}/archive")
	@PreAuthorize("hasAuthority('can_learning_content_archive')")
	public LearningContentResponse archive(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID contentId, Authentication authentication) {
		return LearningContentResponse.from(learningContentApplicationService.archive(establishmentId,
			academicYearId, schoolClassId, contentId, toActor(authentication)));
	}

	@PostMapping(value = "/{contentId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('can_learning_content_update')")
	public org.springframework.http.ResponseEntity<LearningAttachmentResponse> upload(
		@PathVariable UUID establishmentId, @PathVariable UUID academicYearId, @PathVariable UUID schoolClassId,
		@PathVariable UUID contentId, @RequestParam("file") MultipartFile file, Authentication authentication)
		throws IOException {
		return org.springframework.http.ResponseEntity.status(201).body(LearningAttachmentResponse.from(
			learningContentApplicationService.upload(establishmentId, academicYearId, schoolClassId,
				contentId, file.getBytes(), file.getOriginalFilename(), file.getContentType(), toActor(authentication))));
	}

	@GetMapping("/{contentId}/attachments")
	@PreAuthorize("hasAuthority('can_learning_content_read')")
	public List<LearningAttachmentResponse> attachments(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId, @PathVariable UUID contentId,
		Authentication authentication) {
		return learningContentApplicationService.listAttachmentsForStaff(establishmentId, academicYearId,
			schoolClassId, contentId, toActor(authentication)).stream().map(LearningAttachmentResponse::from).toList();
	}

	@GetMapping("/{contentId}/attachments/{attachmentId}")
	@PreAuthorize("hasAuthority('can_learning_content_read')")
	public ResponseEntity<InputStreamResource> download(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId, @PathVariable UUID contentId,
		@PathVariable UUID attachmentId, Authentication authentication) {
		var download = learningContentApplicationService.downloadForStaff(establishmentId, academicYearId,
			schoolClassId, contentId, attachmentId, toActor(authentication));
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.metadata().mediaType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
				.filename(download.metadata().fileName(), StandardCharsets.UTF_8).build().toString())
			.contentLength(download.metadata().size()).body(new InputStreamResource(download.stream()));
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
