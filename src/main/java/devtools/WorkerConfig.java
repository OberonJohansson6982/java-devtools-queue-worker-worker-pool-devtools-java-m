package devtools;

public record WorkerConfig(String apiKey, int maxMessages, int visibilityTimeoutSeconds, int maxAttempts) {
    public static WorkerConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new WorkerConfig(key, 5, 30, 3);
    }
}
