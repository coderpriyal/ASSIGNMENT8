package com.insurance.claims.service;

import com.insurance.claims.config.AppConfig;
import com.insurance.claims.model.Claim;
import com.insurance.claims.model.ClaimStatus;
import com.insurance.claims.queue.ClaimQueueManager;
import com.insurance.claims.queue.PolicyDispatcher;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClaimProcessor implements Runnable {

    private final BlockingQueue<Claim> backlog;
    private final ClaimQueueManager queueManager;
    private final PolicyDispatcher dispatcher;
    private final ExternalClaimChecker checker;
    private final LoggingService log;
    private final SummaryReportService summary;
    private final AppConfig config;

    public ClaimProcessor(BlockingQueue<Claim> backlog,
                          ClaimQueueManager queueManager,
                          PolicyDispatcher dispatcher,
                          ExternalClaimChecker checker,
                          LoggingService log,
                          SummaryReportService summary,
                          AppConfig config) {
        this.backlog = Objects.requireNonNull(backlog);
        this.queueManager = queueManager;
        this.dispatcher = dispatcher;
        this.checker = checker;
        this.log = log;
        this.summary = summary;
        this.config = config;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Claim claim;
            try {
                claim = backlog.poll(300, TimeUnit.MILLISECONDS);
                if (claim == null) continue;
            } catch (InterruptedException e) { break; }

            processOnce(claim);
        }
    }

    private void processOnce(Claim claim) {
        ClaimStatus prev = claim.status;
        claim.status = ClaimStatus.IN_PROGRESS;
        log.audit(claim, prev, claim.status, claim.attempt.get());

        // External check with timeout & retries (bounded and per-policy order preserved via dispatcher)
        ExternalClaimChecker.Result res = checker.check();
        sleep(res.latencyMs);

        boolean timeout = res.timedOut(config.getExternalTimeoutMs());
        boolean transientFail = res.outcome == ExternalClaimChecker.Outcome.TRANSIENT_FAIL;

        if (timeout || transientFail) {
            int attempt = claim.attempt.incrementAndGet();
            if (attempt <= config.getRetryLimit()) {
                // re-queue same policy head (no jumping ahead)
                prev = claim.status;
                claim.status = ClaimStatus.NEW;
                log.audit(claim, prev, claim.status, attempt);
                dispatcher.requeueSameHead(claim);
                return;
            } else {
                prev = claim.status;
                claim.status = ClaimStatus.REJECTED; // exceeded retries -> rejected
                log.audit(claim, prev, claim.status, claim.attempt.get());
                summary.onFinal(claim);
                dispatcher.markPolicyDone(claim);
                return;
            }
        }

        switch (res.outcome) {
            case SUCCESS_APPROVE -> {
                prev = claim.status;
                claim.status = ClaimStatus.APPROVED;
                log.audit(claim, prev, claim.status, claim.attempt.get());
            }
            case SUCCESS_ESCALATE -> {
                prev = claim.status;
                claim.status = ClaimStatus.ESCALATED;
                log.audit(claim, prev, claim.status, claim.attempt.get());
            }
            case PERMANENT_FAIL -> {
                prev = claim.status;
                claim.status = ClaimStatus.FAILED_PERMANENT;
                log.audit(claim, prev, claim.status, claim.attempt.get());
            }
            default -> {}
        }

        summary.onFinal(claim);
        dispatcher.markPolicyDone(claim);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}