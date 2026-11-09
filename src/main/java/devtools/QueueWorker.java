package devtools;

public final class QueueWorker {
    private final InfraiClient client;
    private final WorkerConfig config;
    public QueueWorker(InfraiClient client, WorkerConfig config) { this.client = client; this.config = config; }

    public String handle(JobEvent event) {
        return switch (event.kind()) {
            case "build" -> "Build " + event.jobId() + " recorded: " + event.detail();
            case "release" -> "Release " + event.jobId() + " prepared: " + event.detail();
            case "diagnostic" -> "Diagnostic for " + event.jobId() + ": " + event.detail();
            default -> "Ignored " + event.jobId() + ": unknown event";
        };
    }

    public void runOnce() throws Exception {
        String envelope = client.consume(config.maxMessages(), config.visibilityTimeoutSeconds());
        var root = (java.util.Map<?, ?>) Json.parse(envelope);
        if (!Boolean.TRUE.equals(root.get("ok"))) throw new IllegalStateException("Consume failed: " + envelope);
        var data = (java.util.Map<?, ?>) root.get("data");
        var messages = (java.util.List<?>) data.get("items");
        for (Object item : messages) {
            var message = (java.util.Map<?, ?>) item;
            String id = (String) message.get("message_id");
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing message_id");
            Object payload = message.get("payload");
            JobEvent event;
            if (payload instanceof String text) event = JobEvent.fromPayload(text);
            else {
                var fields = (java.util.Map<?, ?>) payload;
                event = new JobEvent(required(fields, "jobId"), required(fields, "kind"), required(fields, "detail"));
            }
            System.out.println(handle(event));
            client.ack(id);
        }
    }
    private static String required(java.util.Map<?, ?> fields, String name) {
        if (!(fields.get(name) instanceof String value)) throw new IllegalArgumentException("Missing event " + name);
        return value;
    }
    public static void main(String[] args) throws Exception {
        WorkerConfig config = WorkerConfig.fromEnvironment();
        new QueueWorker(new InfraiClient(config.apiKey()), config).runOnce();
    }
}
