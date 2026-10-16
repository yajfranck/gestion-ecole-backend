package com.galactique.gestionecole.learning.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.galactique.gestionecole.learning.application.LearningContentValidationException;
import com.galactique.gestionecole.learning.application.LearningFileStorage;

@Component
public class FileSystemLearningFileStorage implements LearningFileStorage {
	private static final Pattern STORAGE_KEY = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{1,8}");
	private static final Map<String, String> ALLOWED_TYPES = allowedTypes();
	private final Path root;

	public FileSystemLearningFileStorage(
		@Value("${app.learning.storage-root:${java.io.tmpdir}/gestion-ecole/learning-private}") String root) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@Override
	public StoredLearningFile store(byte[] bytes, String originalName, String mediaType, long maxBytes) {
		if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
			throw new LearningContentValidationException("Attachment must be non-empty and within the configured size limit");
		}
		String fileName = safeFileName(originalName);
		String extension = extension(fileName);
		String allowedMediaType = ALLOWED_TYPES.get(extension);
		if (allowedMediaType == null || !allowedMediaType.equalsIgnoreCase(mediaType == null ? "" : mediaType.strip())) {
			throw new LearningContentValidationException("Attachment type is not allowed");
		}

		String storageKey = UUID.randomUUID() + "." + extension;
		Path target = resolveKey(storageKey);
		Path temporary = null;
		try {
			Files.createDirectories(root);
			temporary = Files.createTempFile(root, "upload-", ".tmp");
			Files.write(temporary, bytes);
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, target);
			}
			return new StoredLearningFile(storageKey, fileName, allowedMediaType, bytes.length);
		} catch (IOException exception) {
			if (temporary != null) {
				try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
			}
			throw new LearningStorageException("Unable to store attachment", exception);
		}
	}

	@Override
	public InputStream open(String storageKey) throws IOException {
		return Files.newInputStream(resolveKey(storageKey));
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolveKey(storageKey));
		} catch (IOException exception) {
			throw new LearningStorageException("Unable to delete attachment", exception);
		}
	}

	@Override
	public Set<String> listStorageKeys() {
		if (!Files.isDirectory(root)) {
			return Set.of();
		}
		try (var files = Files.list(root)) {
			Set<String> keys = new HashSet<>();
			files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
				.filter(name -> STORAGE_KEY.matcher(name).matches()).forEach(keys::add);
			return Set.copyOf(keys);
		} catch (IOException exception) {
			throw new LearningStorageException("Unable to list stored attachments", exception);
		}
	}

	@Override
	public void deleteOrphans(Set<String> knownStorageKeys) {
		if (!Files.isDirectory(root)) {
			return;
		}
		try (var files = Files.list(root)) {
			for (Path file : files.filter(Files::isRegularFile).toList()) {
				String name = file.getFileName().toString();
				if (name.startsWith("upload-") && name.endsWith(".tmp")
					|| STORAGE_KEY.matcher(name).matches() && !knownStorageKeys.contains(name)) {
					Files.deleteIfExists(file);
				}
			}
		} catch (IOException exception) {
			throw new LearningStorageException("Unable to clean orphaned attachments", exception);
		}
	}

	private Path resolveKey(String storageKey) {
		if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
			throw new LearningContentValidationException("Invalid storage key");
		}
		Path resolved = root.resolve(storageKey).normalize();
		if (!resolved.getParent().equals(root)) {
			throw new LearningContentValidationException("Invalid storage key");
		}
		return resolved;
	}

	private String safeFileName(String originalName) {
		if (originalName == null || originalName.isBlank() || originalName.length() > 255
			|| originalName.chars().anyMatch(Character::isISOControl)) {
			throw new LearningContentValidationException("Attachment file name is invalid");
		}
		String normalizedSeparators = originalName.strip().replace('\\', '/');
		String fileName = normalizedSeparators.substring(normalizedSeparators.lastIndexOf('/') + 1);
		if (fileName.isBlank() || fileName.equals(".") || fileName.equals("..")) {
			throw new LearningContentValidationException("Attachment file name is invalid");
		}
		return fileName;
	}

	private String extension(String fileName) {
		int separator = fileName.lastIndexOf('.');
		if (separator <= 0 || separator == fileName.length() - 1) {
			return "";
		}
		return fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
	}

	private static Map<String, String> allowedTypes() {
		Map<String, String> types = new HashMap<>();
		types.put("pdf", "application/pdf");
		types.put("png", "image/png");
		types.put("jpg", "image/jpeg");
		types.put("jpeg", "image/jpeg");
		types.put("webp", "image/webp");
		types.put("txt", "text/plain");
		types.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		types.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		return Map.copyOf(types);
	}
}
