package com.insurance.claims.service;

import com.insurance.claims.config.AppConfig;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ExternalClaimChecker {
    private final AppConfig config;
    private final Random rnd = new Random();

    public ExternalClaimChecker(AppConfig config) {
        this.config = config;
    }

    public Result check() {
        // Simulate variable latency and outcomes
        int latency = 100 + rnd.nextInt(1000); // 100..1100 ms
        Outcome outcome;
        int p = rnd.nextInt(100);
        if (p < 65) outcome = Outcome.SUCCESS_APPROVE;          // 65%
        else if (p < 75) outcome = Outcome.SUCCESS_ESCALATE;    // 10%
        else if (p < 92) outcome = Outcome.TRANSIENT_FAIL;      // 17%
        else outcome = Outcome.PERMANENT_FAIL;                  // 8%
        return new Result(latency, outcome);
    }

    public static class Result {
        public final int latencyMs;
        public final Outcome outcome;
        public Result(int latencyMs, Outcome outcome) {
            this.latencyMs = latencyMs; this.outcome = outcome;
        }
        public boolean timedOut(long timeoutMs) {
            return latencyMs > timeoutMs;
        }
    }

    public enum Outcome {
        SUCCESS_APPROVE, SUCCESS_ESCALATE, TRANSIENT_FAIL, PERMANENT_FAIL
    }
}