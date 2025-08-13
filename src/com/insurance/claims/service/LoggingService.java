package com.insurance.claims.service;

import com.insurance.claims.model.Claim;
import com.insurance.claims.model.ClaimStatus;
import com.insurance.claims.audit.AuditLogger;

import java.io.File;

public class LoggingService {
    private final AuditLogger audit;

    public LoggingService(File file) {
        this.audit = new AuditLogger(file);
    }

    public void audit(Claim c, ClaimStatus prev, ClaimStatus now, int attempt) {
        String line = System.currentTimeMillis() + "," +
                Thread.currentThread().getName() + "," +
                c.claimId + "," + c.policyNumber + "," +
                prev + "->" + now + "," + "attempt=" + attempt;
        audit.appendLine(line);
        synchronized (c.history) {
            c.history.add(line);
        }
    }

    public void console(String msg) {
        synchronized (System.out) { System.out.println(msg); }
    }

    public void close() { audit.close(); }
}