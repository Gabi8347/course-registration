package edu.svv.registration;

/** Outcome of a registration attempt: REGISTERED, or REJECTED with one reason. */
public final class RegistrationResult {

    public enum Status { REGISTERED, REJECTED }

    private final Status status;
    private final RejectionReason reason;

    private RegistrationResult(Status status, RejectionReason reason) {
        this.status = status;
        this.reason = reason;
    }

    public static RegistrationResult registered() {
        return new RegistrationResult(Status.REGISTERED, null);
    }

    public static RegistrationResult rejected(RejectionReason reason) {
        return new RegistrationResult(Status.REJECTED, reason);
    }

    public Status getStatus() { return status; }

    /** Null when the registration was accepted. */
    public RejectionReason getReason() { return reason; }

    public boolean isRegistered() { return status == Status.REGISTERED; }

    @Override
    public String toString() {
        return isRegistered() ? "REGISTERED" : "REJECTED (" + reason + ")";
    }
}
