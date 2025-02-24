package com.betting.stake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StakeManager {

    private static Logger logger = LoggerFactory.getLogger(StakeManager.class.getName());

    // Map<BetOfferId, Map<CustomerId, MaxStake>>
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> highStakes = new ConcurrentHashMap<>();

    // Map<BetOfferId, Map<"customerId, int[Stake]">>, just for keep track of all
    // stakes
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, int[]>> betOfferToStakes = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    // Singleton pattern
    private static StakeManager instance = new StakeManager();

    private static int ORIGINAL_STAKE_SIZE = 10;

    private StakeManager() {
        // clean high stakes every minute to reduce memory consumption
        cleaner.scheduleAtFixedRate(this::cleanHighStakes, 1, 1, TimeUnit.MINUTES);
    }

    public static StakeManager getInstance() {
        return instance;
    }

    public void recordStake(int betOfferId, int customerId, int stake) {

        if (customerId == -1) {
            return;
        }

        highStakes.compute(betOfferId, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<>();
            }
            // Store only the maximum stake per customer
            v.merge(customerId, stake, Math::max);
            return v;
        });

        // we store all stakes int int[] other then List<Integer> to reduce memory
        // consumption
        betOfferToStakes.compute(betOfferId, (k, v) -> {
            if (v == null) {
                v = new ConcurrentHashMap<Integer, int[]>();
                int[] stakes = new int[ORIGINAL_STAKE_SIZE];
                stakes[0] = stake;
                v.put(customerId, stakes);
            } else {
                int[] stakes = v.get(customerId);
                if (stakes == null) {
                    stakes = new int[ORIGINAL_STAKE_SIZE];
                    stakes[0] = stake;
                    v.put(customerId, stakes);
                } else {
                    boolean isFull = true;
                    for (int i = 0; i < stakes.length; i++) {
                        if (stakes[i] == 0) {
                            stakes[i] = stake;
                            isFull = false;
                            break;
                        }
                    }
                    if (isFull) {
                        int[] newStakes = new int[stakes.length + ORIGINAL_STAKE_SIZE];
                        System.arraycopy(stakes, 0, newStakes, 0, stakes.length);
                        newStakes[stakes.length] = stake;
                        v.put(customerId, newStakes);
                    }
                }
            }
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

    /**
     * Clean high stakes every minute to reduce memory consumption
     * Remove all stakes except top 20
     */
    private void cleanHighStakes() {

        highStakes.entrySet().stream().parallel().forEach(entry -> {

            int betOfferId = entry.getKey();
            int oriSize = entry.getValue().size();

            ConcurrentHashMap<Integer, Integer> stakes = entry.getValue();
            if (stakes.size() > 20) {
                stakes.entrySet().stream().sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                        .skip(20)// required to keep top 20
                        .forEach(e -> stakes.remove(e.getKey()));
            }

            int cleanedSize = stakes.size();
            logger.info("Cleaned high stakes for bet offer {}: {} -> {}", betOfferId, oriSize, cleanedSize);
        });
    }
}