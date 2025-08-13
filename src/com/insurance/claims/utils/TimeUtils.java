package com.insurance.claims.utils;

public class TimeUtils {
    public static void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}