package com.insurance.claims.config;

import java.util.Objects;

public class AppConfig {
    private final int workerCount;              // default 8
    private final int backlogCapacity;          // default 100
    private final long externalTimeoutMs;       // default 800
    private final int retryLimit;               // default 3
    private final int suspiciousWindowSeconds;  // default 30
    private final int suspiciousThreshold;      // default 5

    public AppConfig(int workerCount, int backlogCapacity, long externalTimeoutMs,
                     int retryLimit, int suspiciousWindowSeconds, int suspiciousThreshold) {
        this.workerCount = workerCount;
        this.backlogCapacity = backlogCapacity;
        this.externalTimeoutMs = externalTimeoutMs;
        this.retryLimit = retryLimit;
        this.suspiciousWindowSeconds = suspiciousWindowSeconds;
        this.suspiciousThreshold = suspiciousThreshold;
    }

    public static AppConfig fromArgs(String[] args) {
        int workers = 8, backlog = 100, retry = 3, sw = 30, st = 5;
        long timeout = 800;
        for (String a : args) {
            String[] kv = a.split("=");
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "--workers" -> workers = Integer.parseInt(kv[1]);
                case "--backlog" -> backlog = Integer.parseInt(kv[1]);
                case "--timeoutMs" -> timeout = Long.parseLong(kv[1]);
                case "--retries" -> retry = Integer.parseInt(kv[1]);
                case "--susWindowSec" -> sw = Integer.parseInt(kv[1]);
                case "--susThreshold" -> st = Integer.parseInt(kv[1]);
            }
        }
        return new AppConfig(workers, backlog, timeout, retry, sw, st);
    }

    public int getWorkerCount() { return workerCount; }
    public int getBacklogCapacity() { return backlogCapacity; }
    public long getExternalTimeoutMs() { return externalTimeoutMs; }
    public int getRetryLimit() { return retryLimit; }
    public int getSuspiciousWindowSeconds() { return suspiciousWindowSeconds; }
    public int getSuspiciousThreshold() { return suspiciousThreshold; }

    @Override public String toString() {
        return "workers=" + workerCount + ", backlog=" + backlogCapacity +
                ", timeoutMs=" + externalTimeoutMs + ", retries=" + retryLimit +
                ", susWindow=" + suspiciousWindowSeconds + "s, susThreshold=" + suspiciousThreshold;
    }

    @Override public int hashCode() { return Objects.hash(workerCount, backlogCapacity, externalTimeoutMs, retryLimit); }
}