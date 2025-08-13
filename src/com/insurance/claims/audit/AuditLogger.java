package com.insurance.claims.audit;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AuditLogger
{
    private final PrintWriter pw;
    public AuditLogger(File file) {
        try {
            this.pw = new PrintWriter(new FileWriter(file, true), true);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open audit log", e);
        }
    }

    public void appendLine(String line) {
        synchronized (pw) {
            pw.println(line); // atomic full-line write; no interleaving
        }
    }

    public void close() { synchronized (pw) { pw.flush(); pw.close(); } }
}
