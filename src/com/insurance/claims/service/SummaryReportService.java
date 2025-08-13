package com.insurance.claims.service;

import com.insurance.claims.model.Claim;
import com.insurance.claims.model.ClaimStatus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class SummaryReportService {
    private final Map<String, Boolean> uniqueClaims = new ConcurrentHashMap<>();
    private final Map<ClaimStatus, AtomicInteger> statusCounts = new EnumMap<>(ClaimStatus.class);
    private final LongAdder totalApprovedAmount = new LongAdder();
    private final LongAdder totalAttempts = new LongAdder();

    public SummaryReportService() {
        for (ClaimStatus s : ClaimStatus.values()) statusCounts.put(s, new AtomicInteger());
    }

    public void onFinal(Claim c) {
        uniqueClaims.putIfAbsent(c.claimId, true);
        statusCounts.get(c.status).incrementAndGet();
        totalAttempts.add(c.attempt.get());
        if (c.status == ClaimStatus.APPROVED) {
            totalApprovedAmount.add(c.amount);
        }
    }

    public void writeSummary(File out, long wallMs) throws IOException {
        try (FileWriter fw = new FileWriter(out, false)) {
            int unique = uniqueClaims.size();
            fw.write("Total unique claims processed: " + unique + "\n");
            fw.write("Approved: " + statusCounts.get(ClaimStatus.APPROVED).get() + "\n");
            fw.write("Escalated: " + statusCounts.get(ClaimStatus.ESCALATED).get() + "\n");
            fw.write("Rejected: " + statusCounts.get(ClaimStatus.REJECTED).get() + "\n");
            fw.write("Permanent Failures: " + statusCounts.get(ClaimStatus.FAILED_PERMANENT).get() + "\n");
            fw.write("Total amount paid: " + totalApprovedAmount.sum() + "\n");
            double avgAttempts = unique == 0 ? 0.0 : (double) totalAttempts.sum() / unique;
            fw.write("Average attempts per claim: " + String.format("%.2f", avgAttempts) + "\n");
            fw.write("Total wall-clock time (ms): " + wallMs + "\n");
        }
    }
}