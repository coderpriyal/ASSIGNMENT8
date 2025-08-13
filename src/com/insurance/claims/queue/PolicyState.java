package com.insurance.claims.queue;

import com.insurance.claims.model.Claim;

import java.util.ArrayDeque;
import java.util.Deque;

class PolicyState {
    final Deque<Claim> fifo = new ArrayDeque<>();
    boolean inFlight = false;
}