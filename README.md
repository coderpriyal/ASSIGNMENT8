Insurance Claim Recovery System

Overview
This is a multi-threaded Java program that processes insurance claims concurrently while ensuring per-policy order, idempotency, retry handling, fraud monitoring, and accurate final reporting.

Key Features

Per-Policy Serial Processing: Claims for the same policy are processed sequentially using a dedicated queue and synchronized lock per policy. This ensures FIFO processing and thread safety.

Priority Preemption: URGENT claims are placed in a global priority queue that always prefers urgent over normal claims. To avoid starvation, normal claims are also dequeued in FIFO order if no urgent claims are pending.

Idempotency: A thread-safe ConcurrentHashMap tracks processed ClaimIDs. Duplicate or retried claims are skipped if already processed.

Fraud Detection & Throttling: A background monitor pauses intake if >5 suspicious claims (Accident & amount ≥ 400,000) are seen in a 30s sliding window. This simulates a fraud investigation.

External Check with Timeout & Retry: Claims interact with an external check simulated to randomly pass/fail. Timeouts and transient failures are retried up to R times. Requeued claims retain their policy order.

Deadlock Avoidance: Locks are acquired using a consistent global lock ordering by sorting policy IDs before acquisition. This avoids circular waits.

Logging & Summary: All claim transitions are logged to audit.log atomically per line. A final summary.txt is generated with stats and wall-clock timing.

Configuration
Editable via config.properties:

worker.count=8

backlog.capacity=100

retry.limit=3

external.timeout.ms=3000

fraud.window.sec=30

fraud.threshold=5

How We Ensure:

Per-Policy Ordering:
Each policy gets its own queue (Map<Policy, Queue>) and synchronized lock. Claims are dequeued in submission order, and only one worker can handle a claim per policy at a time.

Deadlock Avoidance:
Locks are always acquired in lexicographical order of policy IDs. This consistent locking order prevents deadlock during concurrent queue access.

Priority Preemption & Starvation Avoidance:
Claims are stored in a PriorityBlockingQueue that prioritizes urgent claims. Normal claims are dequeued fairly after urgent ones are drained. This avoids starvation via time-based fair polling.

Idempotency:
Processed claims are stored in a ConcurrentHashMap. Duplicate ClaimIDs are filtered before processing begins.

Trade-offs:

Slightly higher memory usage due to per-policy queues and duplicate tracking.

Some increased complexity in managing retry re-queuing per policy while preserving order.
