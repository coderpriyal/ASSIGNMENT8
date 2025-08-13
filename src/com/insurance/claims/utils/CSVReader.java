package com.insurance.claims.utils;

import com.insurance.claims.model.Claim;
import com.insurance.claims.model.ClaimType;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static List<Claim> read(File file) throws IOException {
        List<Claim> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length < 6) continue;
                String id = p[0].trim();
                String policy = p[1].trim();
                int amount = Integer.parseInt(p[2].trim());
                ClaimType type = ClaimType.from(p[3].trim());
                LocalDateTime ldt = LocalDateTime.parse(p[4].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                Instant ts = ldt.atZone(ZoneId.systemDefault()).toInstant();
                boolean urgent = p[5].trim().equalsIgnoreCase("URGENT");
                out.add(new Claim(id, policy, amount, type, ts, urgent));
            }
        }
        return out;
    }
}