package com.betting.session;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    private SessionStore sessionStore = SessionStore.getInstance();

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
        Session session = sessionStore.getOrCreateSession(customerId);

        return session.getKey();
    }

    public boolean isValidSession(String sessionKey) {
        Session session = this.sessionStore.getSession(sessionKey);

        return session != null && !session.isExpired();
    }

    public int getCustomerIdBySession(String sessionKey) {
        Session session = this.sessionStore.getSession(sessionKey);
        if (session == null) {
            return -1;
        }else{
            return session.getCustomerId();
        }
    }

    private void cleanExpiredSessions() {
        this.sessionStore.cleanExpiredSessions();
    }

}
