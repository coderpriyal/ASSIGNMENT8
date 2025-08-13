package com.insurance.claims.service;

import com.insurance.claims.model.Claim;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

public class FraudDetector {
    private final int windowSec;
    private final int threshold;
    private final LoggingService log;
    private final Deque<Long> suspiciousTimes = new ArrayDeque<>();

    public interface Throttle {
        void run();
    }

    public FraudDetector(int windowSec, int threshold, LoggingService log) {
        this.windowSec = windowSec;
        this.threshold = threshold;
        this.log = log;
    }

    public synchronized void observe(Claim c, Throttle throttleCallback) {
        long now = Instant.now().getEpochSecond();
        // slide window
        while (!suspiciousTimes.isEmpty() && now - suspiciousTimes.peekFirst() > windowSec) {
            suspiciousTimes.pollFirst();
        }
        if (c.isSuspicious()) {
            suspiciousTimes.addLast(now);
            log.console("[SUSPICIOUS] " + c);
            if (suspiciousTimes.size() > threshold) {
                throttleCallback.run();
            }
        }
    }
}