package com.betting.session;

import java.util.concurrent.TimeUnit;

import com.betting.Application;

public class Session {
    private final String key;
    private final long expirationTime;

    Session(String key) {
        this.key = key;
        this.expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Application.SESSION_TIMEOUT_MINUTES);
    }

    boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getKey() {
        return key;
    }

    
}
