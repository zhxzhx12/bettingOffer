package com.betting.session;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SessionManager {
    // Customer ID to Session map
    private final ConcurrentHashMap<Integer, Session> customerToSession = new ConcurrentHashMap<>();
    // Session key to Customer ID reverse map
   private final ConcurrentHashMap<String, Integer> sessionToCustomer = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    // Singleton pattern
    private static final SessionManager instance = new SessionManager();

    private SessionManager() {
        // clean expired sessions every minute to reduce memory consumption
        cleaner.scheduleAtFixedRate(this::cleanExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }

    public static SessionManager getInstance() {
        return instance;
    }

    public String getOrCreateSession(int customerId) {
        AtomicReference<String> sessionKey = new AtomicReference<>();// for visibility

        customerToSession.compute(customerId, (k, v) -> {
            if (v == null || v.isExpired()) {
                String newKey = generateSessionKey();
                sessionKey.set(newKey);
                sessionToCustomer.put(newKey, customerId);
                return new Session(newKey);
            }
            sessionKey.set(v.getKey());
            return v;
        });

        return sessionKey.get();
    }

    public boolean isValidSession(String sessionKey) {
        Integer customerId = sessionToCustomer.get(sessionKey);
        if (customerId == null)
            return false;

        Session session = customerToSession.get(customerId);

        return session != null && !session.isExpired() && session.getKey().equals(sessionKey);
    }

    public int getCustomerIdBySession(String sessionKey) {
        Integer customerId = sessionToCustomer.get(sessionKey);
        return customerId != null ? customerId : -1;
    }

    private String generateSessionKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void cleanExpiredSessions() {
        // Remove expired sessions from both maps
        customerToSession.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                sessionToCustomer.remove(entry.getValue().getKey());
                return true;
            }
            return false;
        });
    }

}
