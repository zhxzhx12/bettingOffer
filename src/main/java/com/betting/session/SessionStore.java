package com.betting.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * we store the index of the session in sessionkey to reduce memory consumption
 */
public class SessionStore {

   private static Logger logger = LoggerFactory.getLogger(SessionStore.class);

    private static int ORIGINAL_SESSION_SIZE = 10;

    private ReentrantLock sessionEditLock = new ReentrantLock();

    // Singleton pattern
    private static SessionStore instance = new SessionStore();

    private List<Session> sessions = new ArrayList<Session>(ORIGINAL_SESSION_SIZE);

    private SessionStore() {
    }

    public static SessionStore getInstance() {
        return instance;
    }

    public Session getOrCreateSession(int customerId) {

        Optional<Session> session4Customer = sessions.stream().parallel()
                .filter(session -> session != null && session.getCustomerId() == customerId && !session.isExpired())
                .findFirst();

        if (session4Customer.isPresent()) {
            return session4Customer.get();
        }

        sessionEditLock.lock();
        try {
            int index = getFistFreeIndex();
            Session newSession = new Session(generateSessionKey(index), customerId);
            if (index == sessions.size()) {
                sessions.add(newSession);
            } else {
                sessions.set(index, newSession);
            }
            return newSession;
        } finally {
            sessionEditLock.unlock();
        }
    }

    public Session getSession(String sessionKey) {
        String[] parts = sessionKey.split("-");

        if (parts.length == 2) {
            try {
                int index = Integer.parseInt(parts[1]);
                if (index < sessions.size()) {
                    Session session = sessions.get(index);
                    if (session != null && !session.isExpired()) {
                        return session;
                    } else {
                        logger.error("Session expired or is not exsited: {}", sessionKey);
                    }
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid session key: {}", sessionKey);
            }
        }
        return null;
    }

    private String generateSessionKey(int index) {
        // we store the index of the session in sessionkey to facilitate the search
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "-" + String.valueOf(index);
    }

    private int getFistFreeIndex() {
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i) == null || sessions.get(i).isExpired()) {
                return i;
            }
        }
        return sessions.size();
    }

    /**
     * Remove some expired sessions from the list to reduce memory consumption
     * It's a heavy operation and will lock the list
     */
    public void cleanExpiredSessions() {
        sessionEditLock.lock();
        try {
            if (sessions.isEmpty() && sessions.size() <= ORIGINAL_SESSION_SIZE) {
                return;
            }

            // if more than half of the sessions are expired, we clean some of them .
            // we keep half of sessions.size() "free" session to reduce the cost of resizing the list
            int originalSize = sessions.size();
            int keepFreeCount = sessions.size() / 2;
            
            sessions.stream().parallel().filter(session -> session == null || session.isExpired())
                    .skip(keepFreeCount)
                    .forEach(session -> {
                        sessions.remove(session);
                    });
            logger.info("Clean expired sessions, original size: {}, current size: {}", originalSize, sessions.size());

        } finally {
            sessionEditLock.unlock();
        }
    }
}
