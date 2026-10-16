package com.galactique.gestionecole.learning.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public interface LearningFileStorage {
	StoredLearningFile store(byte[] bytes, String originalName, String mediaType, long maxBytes);

	InputStream open(String storageKey) throws IOException;

	void delete(String storageKey);

	Set<String> listStorageKeys();

	void deleteOrphans(Set<String> knownStorageKeys);

	record StoredLearningFile(String storageKey, String fileName, String mediaType, long size) {
	}
}
