package com.insurance.claims.queue;

import com.insurance.claims.model.Claim;
import com.insurance.claims.service.LoggingService;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

public class PolicyDispatcher implements Runnable {

    private final ClaimQueueManager manager;
    private final BlockingQueue<Claim> globalBacklog;
    private final LoggingService log;
    private volatile boolean running = true;

    public PolicyDispatcher(ClaimQueueManager manager, BlockingQueue<Claim> globalBacklog, LoggingService log) {
        this.manager = Objects.requireNonNull(manager);
        this.globalBacklog = globalBacklog;
        this.log = log;
    }

    @Override
    public void run() {
        while (running) {
            boolean emitted = false;
            for (Map.Entry<String, PolicyState> e : manager.viewPolicyQueues().entrySet()) {
                PolicyState st = e.getValue();
                synchronized (st) {
                    if (!st.inFlight && !st.fifo.isEmpty()) {
                        Claim head = st.fifo.peekFirst();
                        try {
                            globalBacklog.put(head); // bounded by priority queue capacity (constructor size not enforced), but OK for PQ
                            st.inFlight = true;
                            emitted = true;
                        } catch (InterruptedException ignored) {}
                    }
                }
            }
            if (!emitted) {
                // Check if work is completely done
                if (manager.isIntakeFinished() && isAllEmpty()) break;
                sleep(30);
            }
        }
        log.console("[DISPATCH] Stopped");
    }

    public void markPolicyDone(Claim claim) {
        PolicyState st = manager.viewPolicyQueues().get(claim.policyNumber);
        if (st == null) return;
        synchronized (st) {
            // remove the head we dispatched earlier
            if (!st.fifo.isEmpty() && st.fifo.peekFirst() == claim) {
                st.fifo.pollFirst();
            }
            st.inFlight = false;
        }
    }

    public void requeueSameHead(Claim claim) {
        // keep the same head; just mark not inFlight so it can be tried again
        PolicyState st = manager.viewPolicyQueues().get(claim.policyNumber);
        if (st == null) return;
        synchronized (st) {
            st.inFlight = false;
        }
    }

    public boolean isAllEmpty() {
        for (PolicyState st : manager.viewPolicyQueues().values()) {
            synchronized (st) {
                if (st.inFlight || !st.fifo.isEmpty()) return false;
            }
        }
        return true;
    }

    public void waitUntilEmpty() {
        while (!isAllEmpty() || !manager.isIntakeFinished()) {
            sleep(50);
        }
    }

    public void stop() { running = false; }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}