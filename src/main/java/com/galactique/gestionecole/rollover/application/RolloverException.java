package com.galactique.gestionecole.rollover.application;

public class RolloverException extends RuntimeException {
    public enum Reason { INVALID, NOT_FOUND, FORBIDDEN, CONFLICT }
    private final Reason reason;
    public RolloverException(Reason reason, String message) { super(message); this.reason = reason; }
    public Reason reason() { return reason; }
}
