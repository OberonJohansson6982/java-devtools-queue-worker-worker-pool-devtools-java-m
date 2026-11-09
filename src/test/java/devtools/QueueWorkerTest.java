package devtools;

public final class QueueWorkerTest {
    public static void main(String[] args) throws Exception {
        WorkerConfig config = new WorkerConfig("test", 5, 30, 3);
        QueueWorker worker = new QueueWorker(null, config);
        String result = worker.handle(JobEvent.fromPayload("build-17|release|course-v2"));
        if (!result.equals("Release build-17 prepared: course-v2")) throw new AssertionError(result);
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var calls = new java.util.ArrayList<String>();
        server.createContext("/v1/queue/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            var body = (java.util.Map<?, ?>) Json.parse(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            calls.add(path);
            if (path.endsWith("publish")) {
                if (!(body.get("payload") instanceof java.util.Map<?, ?> event) || !"course-v2".equals(event.get("detail"))) throw new AssertionError(body);
            }
            if (path.endsWith("ack") && !"msg-1".equals(body.get("message_id"))) throw new AssertionError(body);
            String response = path.endsWith("consume")
                    ? "{\"ok\":true,\"data\":{\"items\":[{\"message_id\":\"msg-1\",\"payload\":{\"jobId\":\"build-17\",\"kind\":\"release\",\"detail\":\"course-v2\"}}]}}"
                    : "{\"ok\":true,\"data\":{}}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var client = new InfraiClient("test", "http://127.0.0.1:" + server.getAddress().getPort(), "test-queue");
            client.publish("build-17|release|course-v2");
            new QueueWorker(client, config).runOnce();
            if (!calls.equals(java.util.List.of("/v1/queue/publish", "/v1/queue/consume", "/v1/queue/ack"))) throw new AssertionError(calls);
        } finally { server.stop(0); }
        System.out.println("QueueWorkerTest passed");
    }
}
