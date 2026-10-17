package com.galactique.gestionecole.timetables.application;

import java.util.UUID;

public class TimetableConflictException extends RuntimeException {
	private final UUID conflictingEntryId;
	public TimetableConflictException(UUID conflictingEntryId) { this.conflictingEntryId = conflictingEntryId; }
	public UUID conflictingEntryId() { return conflictingEntryId; }
}
