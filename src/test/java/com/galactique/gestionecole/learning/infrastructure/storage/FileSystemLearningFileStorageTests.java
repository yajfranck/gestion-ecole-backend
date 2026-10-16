package com.galactique.gestionecole.learning.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.galactique.gestionecole.learning.application.LearningContentValidationException;
import com.galactique.gestionecole.learning.infrastructure.storage.FileSystemLearningFileStorage;

class FileSystemLearningFileStorageTests {
	@TempDir
	Path temporaryDirectory;

	@Test
	void storesFilesPrivatelyByGeneratedKeyAndDeletesThem() throws Exception {
		FileSystemLearningFileStorage storage = new FileSystemLearningFileStorage(temporaryDirectory.toString());
		byte[] content = "private course material".getBytes(java.nio.charset.StandardCharsets.UTF_8);

		var stored = storage.store(content, "../lesson.txt", "text/plain", 1024);

		assertTrue(stored.storageKey().matches("[0-9a-f-]{36}\\.txt"));
		assertTrue(Files.exists(temporaryDirectory.resolve(stored.storageKey())));
		try (var input = storage.open(stored.storageKey())) {
			assertArrayEquals(content, input.readAllBytes());
		}
		storage.delete(stored.storageKey());
		assertTrue(storage.listStorageKeys().isEmpty());
	}

	@Test
	void rejectsOversizedAndMismatchedFileTypes() {
		FileSystemLearningFileStorage storage = new FileSystemLearningFileStorage(temporaryDirectory.toString());

		assertThrows(LearningContentValidationException.class,
			() -> storage.store(new byte[5], "lesson.pdf", "application/pdf", 4));
		assertThrows(LearningContentValidationException.class,
			() -> storage.store(new byte[] {1}, "lesson.pdf", "text/plain", 4));
	}
}
