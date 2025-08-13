package com.insurance.claims.model;

public enum ClaimType {
    Accident, Theft, Health, Fire;

    public static ClaimType from(String s) {
        return ClaimType.valueOf(s.trim());
    }
}