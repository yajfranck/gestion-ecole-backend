package com.galactique.gestionecole.learning.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.galactique.gestionecole.learning.domain.LearningAttachmentRepository;
import com.galactique.gestionecole.learning.domain.LearningContentRepository;

@Component
@ConditionalOnProperty(name = "app.learning.cleanup-on-startup", havingValue = "true")
public class LearningStorageCleanupRunner implements ApplicationRunner {
	private static final Logger logger = LoggerFactory.getLogger(LearningStorageCleanupRunner.class);

	private final LearningAttachmentRepository attachmentRepository;
	private final FileSystemLearningFileStorage fileStorage;

	public LearningStorageCleanupRunner(LearningAttachmentRepository attachmentRepository,
		FileSystemLearningFileStorage fileStorage) {
		this.attachmentRepository = attachmentRepository;
		this.fileStorage = fileStorage;
	}

	@Override
	public void run(ApplicationArguments arguments) {
		try {
			fileStorage.deleteOrphans(attachmentRepository.findAllStorageKeys());
		} catch (LearningStorageException exception) {
			logger.warn("Unable to clean orphaned learning attachments", exception);
		}
	}
}
