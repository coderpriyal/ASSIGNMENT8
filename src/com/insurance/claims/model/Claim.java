package com.insurance.claims.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Claim implements Comparable<Claim> {
    public final String claimId;
    public final String policyNumber;
    public final int amount;
    public final ClaimType type;
    public final Instant timestamp;
    public final boolean urgent;

    // bookkeeping
    public volatile ClaimStatus status = ClaimStatus.NEW;
    public final AtomicInteger attempt = new AtomicInteger(0);
    public final List<String> history = new ArrayList<>();

    public Claim(String claimId, String policyNumber, int amount, ClaimType type,
                 Instant timestamp, boolean urgent) {
        this.claimId = claimId;
        this.policyNumber = policyNumber;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.urgent = urgent;
    }

    @Override
    public int compareTo(Claim o) {
        // Priority: URGENT first, then earlier timestamp, then CSV order via claimId fallback
        int p = Boolean.compare(o.urgent, this.urgent);
        if (p != 0) return p;
        int t = this.timestamp.compareTo(o.timestamp);
        if (t != 0) return t;
        return this.claimId.compareTo(o.claimId);
    }

    @Override public String toString() {
        return claimId + "/" + policyNumber + "/" + (urgent ? "URGENT" : "NORMAL") + "/" + type + "/" + amount;
    }

    public boolean isSuspicious() {
        // Example rule: Accident and amount >= 400000
        return type == ClaimType.Accident && amount >= 400000;
    }
}