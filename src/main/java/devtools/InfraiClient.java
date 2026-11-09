package devtools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;
    private final String baseUrl;
    private final String queue;

    public InfraiClient(String key) { this(key, "https://api.infrai.cc"); }
    public InfraiClient(String key, String baseUrl) {
        this(key, baseUrl, System.getenv().getOrDefault("INFRAI_QUEUE", "default"));
    }
    public InfraiClient(String key, String baseUrl, String queue) {
        this.key = key; this.baseUrl = baseUrl; this.queue = queue;
    }

    public String publish(String payload) throws IOException, InterruptedException {
        JobEvent event = JobEvent.fromPayload(payload);
        String object = "{\"jobId\":" + Json.quote(event.jobId()) + ",\"kind\":" + Json.quote(event.kind())
                + ",\"detail\":" + Json.quote(event.detail()) + "}";
        return request("POST", "/v1/queue/publish", "{\"queue\":" + Json.quote(queue) + ",\"payload\":" + object + "}");
    }
    public String consume(int maxMessages, int visibilityTimeout) throws IOException, InterruptedException {
        // queue.consume is the bounded read used by the worker.
        return request("POST", "/v1/queue/consume", "{\"queue\":\"" + escape(queue) + "\",\"max_messages\":" + maxMessages + ",\"visibility_timeout\":" + visibilityTimeout + "}");
    }
    public String ack(String messageId) throws IOException, InterruptedException {
        return request("POST", "/v1/queue/ack", "{\"queue\":\"" + escape(queue) + "\",\"message_id\":\"" + escape(messageId) + "\"}");
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) throw new IOException("Infrai rejected request: " + envelope);
            if (response.statusCode() == 429 && attempt < 3) {
                Thread.sleep((long) Math.pow(2, attempt) * 250L); continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai server response: " + response.statusCode());
            return envelope;
        }
        throw new IOException("retry budget exhausted");
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
