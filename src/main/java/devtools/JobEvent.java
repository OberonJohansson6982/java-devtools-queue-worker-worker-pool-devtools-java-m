package devtools;

public record JobEvent(String jobId, String kind, String detail) {
    public static JobEvent fromPayload(String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) throw new IllegalArgumentException("payload must be jobId|kind|detail");
        return new JobEvent(parts[0], parts[1], parts[2]);
    }
}
