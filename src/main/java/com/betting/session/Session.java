package com.betting.session;

import java.util.concurrent.TimeUnit;

import com.betting.Application;

public class Session {
    private final String key;
    private final long expirationTime;
    private int customerId;

    Session(String key) {
        this.key = key;
        this.expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Application.SESSION_TIMEOUT_MINUTES);
    }

    Session(String key, int customerId) {
        this.key = key;
        this.expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Application.SESSION_TIMEOUT_MINUTES);
        this.customerId = customerId;
    }

    boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getKey() {
        return key;
    }

    public int getCustomerId() {
        return customerId;
    }

    
}
