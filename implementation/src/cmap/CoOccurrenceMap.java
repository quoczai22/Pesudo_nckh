package cmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database-level co-occurrence map used as an upper-bound pruning structure.
 * The after-map stores how many sequences contain itemB after itemA at least once.
 */
public final class CoOccurrenceMap {
    private final Map<Integer, Map<Integer, Integer>> afterSupport;

    private CoOccurrenceMap(Map<Integer, Map<Integer, Integer>> afterSupport) {
        this.afterSupport = afterSupport;
    }

    /**
     * Builds the sequential co-occurrence map for a flattened clickstream database.
     * Each pair is counted at most once per sequence, matching support semantics.
     */
    public static CoOccurrenceMap buildAfterMap(
            List<List<Integer>> sequences,
            Set<Integer> frequentItems) {
        Map<Integer, Map<Integer, Integer>> afterSupport = new HashMap<>();

        for (List<Integer> sequence : sequences) {
            Set<Long> seenPairsInSequence = new HashSet<>();
            Set<Integer> seenLeftItems = new HashSet<>();

            for (int i = 0; i < sequence.size(); i++) {
                int itemA = sequence.get(i);
                if (!frequentItems.contains(itemA) || seenLeftItems.contains(itemA)) {
                    continue;
                }

                Set<Integer> seenRightItems = new HashSet<>();
                for (int j = i + 1; j < sequence.size(); j++) {
                    int itemB = sequence.get(j);
                    if (!frequentItems.contains(itemB) || seenRightItems.contains(itemB)) {
                        continue;
                    }

                    long pairKey = pairKey(itemA, itemB);
                    if (seenPairsInSequence.add(pairKey)) {
                        Map<Integer, Integer> itemAMap =
                                afterSupport.computeIfAbsent(itemA, k -> new HashMap<>());
                        itemAMap.put(itemB, itemAMap.getOrDefault(itemB, 0) + 1);
                    }
                    seenRightItems.add(itemB);
                }
                seenLeftItems.add(itemA);
            }
        }

        return new CoOccurrenceMap(afterSupport);
    }

    public int getAfterSupport(int itemA, int itemB) {
        Map<Integer, Integer> itemAMap = afterSupport.get(itemA);
        if (itemAMap == null) {
            return 0;
        }
        return itemAMap.getOrDefault(itemB, 0);
    }

    private static long pairKey(int itemA, int itemB) {
        return (((long) itemA) << 32) ^ (itemB & 0xffffffffL);
    }
}
