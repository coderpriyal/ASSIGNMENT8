package com.insurance.claims.queue;

import com.insurance.claims.config.AppConfig;
import com.insurance.claims.model.Claim;
import com.insurance.claims.service.FraudDetector;
import com.insurance.claims.service.LoggingService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class ClaimQueueManager {

    private final Map<String, PolicyState> policyQueues = new ConcurrentHashMap<>();
    private final Set<String> seenClaimIds = new ConcurrentSkipListSet<>(); // idempotency
    private final AppConfig config;
    private final FraudDetector fraudDetector;
    private final LoggingService log;

    private volatile boolean intakeFinished = false;
    private volatile boolean intakePaused = false;

    public ClaimQueueManager(AppConfig config, FraudDetector fraudDetector, LoggingService logger) {
        this.config = config;
        this.fraudDetector = fraudDetector;
        this.log = logger;
    }

    public void ingest(Collection<Claim> claims) {
        for (Claim c : claims) {
            // Fraud detection (streaming)
            fraudDetector.observe(c, () -> pauseIntakeForThrottle());

            if (!seenClaimIds.add(c.claimId)) {
                // duplicate in CSV, ignore (idempotent)
                continue;
            }
            // throttle intake (pause but don't crash)
            while (intakePaused) {
                sleep(200);
            }
            policyQueues.computeIfAbsent(c.policyNumber, k -> new PolicyState()).fifo.addLast(c);
        }
    }

    public synchronized void pauseIntakeForThrottle() {
        if (intakePaused) return;
        intakePaused = true;
        log.console("[THROTTLE] Suspicious spike detected -> pausing intake for 2s");
        new Thread(() -> {
            sleep(2000);
            intakePaused = false;
            log.console("[THROTTLE] Intake resumed");
        }, "intake-throttle").start();
    }

    public Map<String, PolicyState> viewPolicyQueues() { return policyQueues; }

    public void finishIntake() { intakeFinished = true; }

    public boolean isIntakeFinished() { return intakeFinished; }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}