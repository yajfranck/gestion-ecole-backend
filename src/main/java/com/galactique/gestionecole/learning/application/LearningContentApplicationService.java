package com.galactique.gestionecole.learning.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.learning.domain.LearningAttachment;
import com.galactique.gestionecole.learning.domain.LearningAttachmentRepository;
import com.galactique.gestionecole.learning.domain.LearningContent;
import com.galactique.gestionecole.learning.domain.LearningContentDraft;
import com.galactique.gestionecole.learning.domain.LearningContentPage;
import com.galactique.gestionecole.learning.domain.LearningContentRepository;
import com.galactique.gestionecole.learning.domain.LearningContentStatus;
import com.galactique.gestionecole.learning.domain.LearningContentType;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCalendarApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException;

@Service
public class LearningContentApplicationService {
	private final LearningContentRepository contentRepository;
	private final LearningAttachmentRepository attachmentRepository;
	private final LearningFileStorage fileStorage;
	private final EmployeeApplicationService employeeApplicationService;
	private final SchoolCalendarApplicationService schoolCalendarApplicationService;
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;
	private final LicenseApplicationService licenseApplicationService;
	private final long maxFileSize;

	public LearningContentApplicationService(LearningContentRepository contentRepository,
		LearningAttachmentRepository attachmentRepository, LearningFileStorage fileStorage,
		EmployeeApplicationService employeeApplicationService,
		SchoolCalendarApplicationService schoolCalendarApplicationService,
		SchoolCatalogApplicationService schoolCatalogApplicationService,
		LicenseApplicationService licenseApplicationService,
		@Value("${app.learning.max-file-size-bytes:10485760}") long maxFileSize) {
		this.contentRepository = contentRepository;
		this.attachmentRepository = attachmentRepository;
		this.fileStorage = fileStorage;
		this.employeeApplicationService = employeeApplicationService;
		this.schoolCalendarApplicationService = schoolCalendarApplicationService;
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
		this.licenseApplicationService = licenseApplicationService;
		this.maxFileSize = maxFileSize;
	}

	@Transactional(readOnly = true)
	public LearningContentPage listForStaff(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		LearningContentType type, int page, int size, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		validateActiveContext(establishmentId, academicYearId, schoolClassId, actor);
		validatePage(page, size);
		return contentRepository.findPage(establishmentId, academicYearId, schoolClassId, type, false, page, size);
	}

	@Transactional(readOnly = true)
	public LearningContentPage listPublishedForGuardian(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, LearningContentType type, int page, int size) {
		requireFeature(establishmentId);
		validatePage(page, size);
		return contentRepository.findPage(establishmentId, academicYearId, schoolClassId, type, true, page, size);
	}

	@Transactional
	public LearningContent create(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		LearningContentCommand command, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		validateActiveContext(establishmentId, academicYearId, schoolClassId, actor);
		String title = required(command.title(), "title", 160);
		String description = optional(command.description(), "description", 2000);
		String body = optional(command.body(), "body", 100000);
		if (command.type() == null) {
			throw new LearningContentValidationException("type is required");
		}
		if (command.type() == LearningContentType.LESSON && command.dueDate() != null) {
			throw new LearningContentValidationException("Lessons cannot have a due date");
		}
		validateSubject(establishmentId, academicYearId, schoolClassId, command.subjectId(), actor);
		try {
			return contentRepository.save(new LearningContentDraft(establishmentId, academicYearId,
				schoolClassId, command.subjectId(), command.type(), title, description, body,
				command.dueDate(), actor.username()));
		} catch (DataIntegrityViolationException exception) {
			throw new LearningContentConflictException();
		}
	}

	@Transactional(readOnly = true)
	public LearningContent getForStaff(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		validateActiveContext(establishmentId, academicYearId, schoolClassId, actor);
		return findContent(establishmentId, academicYearId, schoolClassId, contentId)
			.filter(content -> content.status() != LearningContentStatus.ARCHIVED)
			.orElseThrow(LearningContentNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public LearningContent getPublishedForGuardian(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId) {
		requireFeature(establishmentId);
		return findContent(establishmentId, academicYearId, schoolClassId, contentId)
			.filter(content -> content.status() == LearningContentStatus.PUBLISHED)
			.orElseThrow(LearningContentNotFoundException::new);
	}

	@Transactional
	public LearningContent update(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID contentId, LearningContentCommand command, EmployeeActor actor) {
		LearningContent current = getForStaff(establishmentId, academicYearId, schoolClassId, contentId, actor);
		if (current.status() == LearningContentStatus.ARCHIVED) {
			throw new LearningContentNotFoundException();
		}
		if (command.type() != current.type()) {
			throw new LearningContentValidationException("Content type cannot be changed");
		}
		String title = required(command.title(), "title", 160);
		String description = optional(command.description(), "description", 2000);
		String body = optional(command.body(), "body", 100000);
		if (command.type() == LearningContentType.LESSON && command.dueDate() != null) {
			throw new LearningContentValidationException("Lessons cannot have a due date");
		}
		validateSubject(establishmentId, academicYearId, schoolClassId, command.subjectId(), actor);
		try {
			return contentRepository.update(current, title, description, body,
				command.subjectId(), command.dueDate(), actor.username());
		} catch (DataIntegrityViolationException exception) {
			throw new LearningContentConflictException();
		}
	}

	@Transactional
	public LearningContent publish(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID contentId, EmployeeActor actor) {
		LearningContent current = getForStaff(establishmentId, academicYearId, schoolClassId, contentId, actor);
		if (current.status() != LearningContentStatus.DRAFT) {
			throw new LearningContentConflictException();
		}
		return contentRepository.publish(current, actor.username());
	}

	@Transactional
	public LearningContent archive(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID contentId, EmployeeActor actor) {
		LearningContent current = getForStaff(establishmentId, academicYearId, schoolClassId, contentId, actor);
		if (current.status() == LearningContentStatus.ARCHIVED) {
			throw new LearningContentNotFoundException();
		}
		return contentRepository.archive(current, actor.username());
	}

	@Transactional
	public LearningAttachment upload(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID contentId, byte[] bytes, String fileName, String mediaType, EmployeeActor actor) {
		LearningContent content = getForStaff(establishmentId, academicYearId, schoolClassId, contentId, actor);
		if (content.status() != LearningContentStatus.DRAFT) {
			throw new LearningContentConflictException();
		}
		LearningFileStorage.StoredLearningFile stored = fileStorage.store(bytes, fileName, mediaType, maxFileSize);
		try {
			return attachmentRepository.save(contentId, stored.storageKey(), stored.fileName(),
				stored.mediaType(), stored.size(), actor.username());
		} catch (RuntimeException exception) {
			fileStorage.delete(stored.storageKey());
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<LearningAttachment> listAttachmentsForStaff(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId, EmployeeActor actor) {
		LearningContent content = getForStaff(establishmentId, academicYearId, schoolClassId, contentId, actor);
		return attachmentRepository.findAllByContentId(content.id());
	}

	@Transactional(readOnly = true)
	public List<LearningAttachment> listAttachmentsForGuardian(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId) {
		LearningContent content = getPublishedForGuardian(establishmentId, academicYearId, schoolClassId, contentId);
		return attachmentRepository.findAllByContentId(content.id());
	}

	@Transactional(readOnly = true)
	public AttachmentDownload downloadForStaff(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId, UUID attachmentId, EmployeeActor actor) {
		LearningContent content = getForStaff(establishmentId, academicYearId, schoolClassId, contentId, actor);
		return openAttachment(content.id(), attachmentId);
	}

	@Transactional(readOnly = true)
	public AttachmentDownload downloadForGuardian(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId, UUID attachmentId) {
		LearningContent content = getPublishedForGuardian(establishmentId, academicYearId, schoolClassId, contentId);
		return openAttachment(content.id(), attachmentId);
	}

	private AttachmentDownload openAttachment(UUID contentId, UUID attachmentId) {
		LearningAttachment attachment = attachmentRepository.findByIdAndContentId(attachmentId, contentId)
			.orElseThrow(LearningContentNotFoundException::new);
		try {
			return new AttachmentDownload(attachment, fileStorage.open(attachment.storageKey()));
		} catch (IOException exception) {
			throw new LearningContentNotFoundException();
		}
	}

	private java.util.Optional<LearningContent> findContent(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId) {
		return contentRepository.find(establishmentId, academicYearId, schoolClassId, contentId);
	}

	private void validateActiveContext(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		EmployeeActor actor) {
		requireFeature(establishmentId);
		try {
			schoolCalendarApplicationService.findYear(establishmentId, academicYearId, actor);
			schoolCatalogApplicationService.findClass(establishmentId, academicYearId, schoolClassId, actor);
		} catch (AcademicCalendarNotFoundException | SchoolCatalogNotFoundException exception) {
			throw new LearningContentNotFoundException();
		} catch (AcademicCalendarAccessDeniedException | SchoolCatalogAccessDeniedException exception) {
			throw new LearningContentAccessDeniedException();
		}
	}

	private void requireFeature(UUID establishmentId) {
		if (!licenseApplicationService.hasFeature(establishmentId, LicenseFeature.LEARNING_CONTENT,
			java.time.Instant.now())) {
			throw new LearningContentAccessDeniedException();
		}
	}

	private void validateSubject(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID subjectId, EmployeeActor actor) {
		if (subjectId == null) {
			return;
		}
		boolean assigned = schoolCatalogApplicationService.findClassSubjects(
			establishmentId, academicYearId, schoolClassId, actor).stream()
			.anyMatch(assignment -> assignment.subjectId().equals(subjectId) && assignment.enabled());
		if (!assigned) {
			throw new LearningContentValidationException("Subject must be assigned to the requested class");
		}
	}

	private void requireScope(UUID establishmentId, EmployeeActor actor) {
		try {
			boolean accessible = employeeApplicationService.findEstablishments(actor).stream()
				.anyMatch(establishment -> establishment.enabled() && establishment.id().equals(establishmentId));
			if (!accessible) {
				throw new LearningContentNotFoundException();
			}
		} catch (org.springframework.security.access.AccessDeniedException exception) {
			throw new LearningContentAccessDeniedException();
		}
	}

	private String required(String value, String field, int maxLength) {
		if (value == null || value.isBlank() || value.strip().length() > maxLength) {
			throw new LearningContentValidationException(field + " is required and must not exceed " + maxLength + " characters");
		}
		return value.strip();
	}

	private String optional(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (value.strip().length() > maxLength) {
			throw new LearningContentValidationException(field + " must not exceed " + maxLength + " characters");
		}
		return value.strip();
	}

	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new LearningContentValidationException("page must be non-negative and size must be between 1 and 100");
		}
	}

	public record AttachmentDownload(LearningAttachment metadata, InputStream stream) {
	}
}
