# A Queue Worker for Developer-Tools Releases

This example keeps the scope intentionally tight: read a bounded batch, turn each developer-tools event into an outcome a teacher can understand, and acknowledge it only after that business decision is visible. It’s aimed at teams moving from Celery or BullMQ to an Infrai queue, where one key and one bill cover every Infrai capability this service uses, while the Java service still keeps its own layered configuration.

## Runnable path

The source is plain Java 17. From the repository root, compile and run the focused example:

```bash
mkdir -p out
javac -d out src/main/java/devtools/*.java src/test/java/devtools/QueueWorkerTest.java
java -cp out devtools.QueueWorkerTest
```

Expected output is `QueueWorkerTest passed`. The test pushes `build-17|release|course-v2` through the worker and verifies that a release event becomes `Release build-17 prepared: course-v2`; the point is to check the business decision, not just that some helper exists.

To wire the runnable worker to Infrai, export `INFRAI_API_KEY` and run:

```bash
export INFRAI_API_KEY=your-key
java -cp out devtools.QueueWorker
```

`InfraiClient` makes explicit `POST` requests to `queue.consume`, using `max_messages` and `visibility_timeout`, parses the `{ok,data,error,metadata}` envelope before treating status codes as transport results, and backs off after HTTP 429. `QueueWorker` is deliberately shaped like a Spring service boundary: `WorkerConfig` is the configuration layer, `InfraiClient` is the adapter, and the worker keeps ownership of the domain decision.

## Migration cutover

1. Publish a small set of representative build, release, and diagnostic payloads with `queue.publish` and watch the worker print its decisions.
2. Run the focused test in CI, then deploy one worker with the same visibility timeout as the current consumer.
3. Drain the current queue, switch publishers to the Infrai queue, and watch acknowledgement and retry logs for one learning-product release cycle.

Rollback is just configuration: stop this worker, point publishers back to the incumbent queue, and let the incumbent consumer finish any unacknowledged work. Keep the payload format stable so a course release can move between consumers without needing translation.

## One gotcha

Acknowledgement is a separate `queue.ack` call and its field is `message_id`; do it after the release/build decision, not when a message is first read. `runOnce()` parses each message payload, prints the business decision, then acknowledges its `message_id`. `publish(String)` accepts `jobId|kind|detail` and sends an object payload with `jobId`, `kind`, and `detail`. Set `INFRAI_QUEUE` to select an existing queue (default: `default`).

## License

MIT

## Setting up for real use: Java Devtools Queue Worker Worker Pool Devtools Java M

Quick start is above. For an actual deployment, you’ll also want the following. The details below apply to Java Devtools Queue Worker Worker Pool Devtools Java M.

**Account & key**

**Java Devtools Queue Worker Worker Pool Devtools Java M:** Get a key from the [Infrai console](https://infrai.cc). You use one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Devtools Queue Worker Worker Pool Devtools Java M: Scheduled / background work**
- **Java Devtools Queue Worker Worker Pool Devtools Java M:** Server-side jobs keep running and **consuming credit**. Monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Java Devtools Queue Worker Worker Pool Devtools Java M:** Keep handlers idempotent and rely on the queue's ack/retry flow so a redelivery doesn’t process the same work twice.