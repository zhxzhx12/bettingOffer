package com.betting.stake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.betting.session.SessionManager;

public class StakeStore {
    // Map<BetOfferId, Map<CustomerId, MaxStake>>
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> stakes = new ConcurrentHashMap<>();
    
    public void recordStake(int betOfferId, String sessionKey, int stake) {
        // SessionManager tracks customer ID in its session map
        stakes.compute(betOfferId, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            // Store only the maximum stake per customer
            v.merge(getCustomerIdFromSession(sessionKey), stake, Math::max);
            return v;
        });
    }
    
    public String getHighStakes(int betOfferId) {
        ConcurrentHashMap<Integer, Integer> offerStakes = stakes.get(betOfferId);
        if (offerStakes == null || offerStakes.isEmpty()) {
            return "";
        }
        
        // Sort entries by stake descending, then take top 20
        return offerStakes.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(20)
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + "," + b)//csv format
            .orElse("");
    }
    
    private int getCustomerIdFromSession(String sessionKey) {
        return SessionManager.getInstance().getCustomerIdBySession(sessionKey);
    }
}