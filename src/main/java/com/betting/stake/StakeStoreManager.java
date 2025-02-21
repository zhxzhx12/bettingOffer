package com.betting.stake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.betting.session.SessionManager;

public class StakeStoreManager {
    // Map<BetOfferId, Map<CustomerId, MaxStake>>
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> highStakes = new ConcurrentHashMap<>();

    // Map<BetOfferId, List<"customerId:Stake">>, just for keep track of all stakes
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<String>> betOfferToStakes = new ConcurrentHashMap<>();

    // Singleton pattern
    private static StakeStoreManager instance = new StakeStoreManager();

    private StakeStoreManager() {
    }

    public static StakeStoreManager getInstance() {
        return instance;
    }

    public void recordStake(int betOfferId, String sessionKey, int stake) {

        int customerId = getCustomerIdFromSession(sessionKey);

        highStakes.compute(betOfferId, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            // Store only the maximum stake per customer
            v.merge(customerId, stake, Math::max);
            return v;
        });

        betOfferToStakes.compute(betOfferId, (k, v) -> {
            if (v == null) {
                v = new CopyOnWriteArrayList<String>();
            }
            v.add(customerId + ":" + stake);
            return v;
        });
    }

    public String getHighStakes(int betOfferId) {
        ConcurrentHashMap<Integer, Integer> offerStakes = highStakes.get(betOfferId);
        if (offerStakes == null || offerStakes.isEmpty()) {
            return "";
        }

        // Sort entries by stake descending, then take top 20
        return offerStakes.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "," + b)// csv format
                .orElse("");
    }

    private int getCustomerIdFromSession(String sessionKey) {
        return SessionManager.getInstance().getCustomerIdBySession(sessionKey);
    }
}