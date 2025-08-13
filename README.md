🚀 Insurance Claim Recovery System
A high-performance, fault-tolerant Java system that recovers and processes pending insurance claims from a CSV file with strict ordering, concurrency, retry, logging, and fraud monitoring.

🧩 Key Features
✅ Parallel processing with a configurable worker pool
✅ Per-policy FIFO ordering (no two claims of same policy processed together)
✅ Priority preemption (URGENT claims jump the queue)
✅ Idempotency: each ClaimID processed exactly once
✅ Retry mechanism for transient failures with timeout support
✅ Fraud detection and auto-throttling with sliding window
✅ Thread-safe, atomic logging (audit.log)
✅ Final reporting (summary.txt) with accurate counts

🏗️ Design Decisions
🔁 Per-Policy Serial Processing
We maintain a synchronized queue per PolicyNumber.

Claims are processed in arrival order per policy using fine-grained locking.

This ensures thread safety and preserves claim order without global locking.

🔐 Deadlock Avoidance
We avoid deadlocks by acquiring locks in a consistent global order (alphabetical PolicyNumber).

No nested locking across queues; retry re-insertion is always safe.

🚨 Priority Handling
A custom PriorityBlockingQueue favors URGENT claims globally.

Normal claims are not starved — we apply FIFO after urgent claims are cleared.

Ensures fairness without blocking urgent claims.

♻️ Idempotency
A ConcurrentHashMap tracks processed ClaimIDs.

Claims with the same ID (due to retry or duplicate input) are ignored on reprocessing.

🔍 Fraud Detection & Throttling
Claims with ClaimType == "Accident" and ClaimAmount ≥ ₹400,000 are flagged.

A sliding window of 30 seconds checks frequency of such claims.

If 5+ suspicious claims are seen, intake pauses for 2 seconds to simulate investigation.

⚙️ Configurable Parameters
Defined in config.properties:

Key	Description	Default
worker.count	Number of parallel worker threads	8
backlog.capacity	Max queue size before intake pauses	100
retry.limit	Max retry attempts for transient fails	3
external.timeout.ms	Max wait time for external check (ms)	3000
fraud.window.sec	Sliding window for fraud (in seconds)	30
fraud.threshold	Suspicious claim threshold	5
