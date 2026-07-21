package cup;

import data.ClickstreamSequenceDatabase;
import idlist.JoinableIdList;
import idlist.LocalDUBBitmap;
import idlist.ProjectedIdList;
import idlist.PseudoIdList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import reporting.ResearchOutputWriter;

/**
 * Paper 1 CUP Algorithm: Clickstream pattern mining using original huy_clickstream IDLists.
 * Features:
 *  - IDList projection temporal matching
 *  - DUB (Dynamic intersection Upper Bound) bit-wise prune technique
 */
public class CUPAlgorithm {
    private final double relativeMinSupport;
    private int absoluteMinSupport;
    private final ResearchOutputWriter logger;

    private final List<Integer> frequentItems;
    private final Map<Integer, ProjectedIdList> onePatternIdLists;
    private final Map<Integer, BitSet> onePatternCidBitsets;
    private final Map<List<Integer>, Integer> frequentPatterns;

    private int dubPruneCount = 0;
    private int joinCount = 0;
    private int localBitmapCount = 0;
    private long localBitmapUniverseBits = 0L;
    private int maxLocalBitmapUniverseBits = 0;

    private boolean storePatternsInMemory = true;
    private int frequentPatternCount = 0;
    private final Map<Integer, IntSupportAccumulator> patternLengthProfile = new TreeMap<>();

    public CUPAlgorithm(double relativeMinSupport, ResearchOutputWriter logger) {
        this.relativeMinSupport = relativeMinSupport;
        this.logger = logger;
        this.frequentItems = new ArrayList<>();
        this.onePatternIdLists = new HashMap<>();
        this.onePatternCidBitsets = new HashMap<>();
        this.frequentPatterns = new LinkedHashMap<>();
    }

    public void setStorePatternsInMemory(boolean storePatternsInMemory) {
        this.storePatternsInMemory = storePatternsInMemory;
    }

    private void registerFrequentPattern(List<Integer> pattern, int support) {
        frequentPatternCount++;
        int length = pattern.size();
        patternLengthProfile.computeIfAbsent(length, k -> new IntSupportAccumulator()).add(support);

        if (storePatternsInMemory) {
            frequentPatterns.put(pattern, support);
        } else {
            StringBuilder spmf = new StringBuilder();
            for (int item : pattern) {
                spmf.append(item).append(" -1 ");
            }
            spmf.append("-2 #SUP: ").append(support);
            logger.log("Extracted_Patterns.txt", spmf.toString());
        }
    }

    /**
     * Executes the complete CUP workflow from threshold normalization to result export.
     */
    public void run(ClickstreamSequenceDatabase db) {
        long startTime = System.currentTimeMillis();

        this.absoluteMinSupport = (int) Math.ceil(relativeMinSupport * db.getSequences().size());
        logger.trace("Translating relative threshold " + String.format(Locale.US, "%.2f", relativeMinSupport)
                + " * DB_Size(" + db.getSequences().size() + ") -> Absolute Threshold = " + absoluteMinSupport);

        step1ScanDB(db.getSequences());
        step2BuildIDLists(db.getSequences());
        step3DFSMining();

        long elapsed = System.currentTimeMillis() - startTime;
        exportExtractedPatterns();
        exportPatternLengthProfile();
        exportResearchMetrics(elapsed);
        printFinalResults(elapsed);
        logStatisticsSummary(elapsed);
    }

    /**
     * Scans the sequence database to obtain frequent 1-patterns and their CID bitsets.
     */
    private void step1ScanDB(List<List<Integer>> sequences) {
        logger.traceHeader("STEP 1: SCAN DATABASE - Count 1-patterns");

        Map<Integer, BitSet> itemCids = new HashMap<>();
        for (int cid = 0; cid < sequences.size(); cid++) {
            for (int item : sequences.get(cid)) {
                itemCids.computeIfAbsent(item, k -> new BitSet()).set(cid);
            }
        }

        List<Integer> sortedItems = new ArrayList<>(itemCids.keySet());
        Collections.sort(sortedItems);

        for (int item : sortedItems) {
            BitSet cids = itemCids.get(item);
            int sup = cids.cardinality();
            String status = (sup >= absoluteMinSupport) ? "OK FREQUENT" : "X INFREQUENT";
            logger.trace("  Item " + item + ": CIDs=" + cids + ", support=" + sup + " " + status);
            if (sup >= absoluteMinSupport) {
                frequentItems.add(item);
                onePatternCidBitsets.put(item, (BitSet) cids.clone());
            }
        }
        logger.trace("\n  F1 = " + frequentItems);
    }

    /**
     * Builds pseudo-IDList-compatible structures for frequent 1-patterns.
     */
    private void step2BuildIDLists(List<List<Integer>> sequences) {
        logger.traceHeader("STEP 2: BUILD IDLists (huy_clickstream baseline)");

        Set<Integer> frequentItemSet = new HashSet<>(frequentItems);
        Map<Integer, PseudoIdList> builders = new HashMap<>();
        for (int item : frequentItems) {
            builders.put(item, new PseudoIdList());
        }

        for (int cid = 0; cid < sequences.size(); cid++) {
            List<Integer> seq = sequences.get(cid);
            for (int pos = 0; pos < seq.size(); pos++) {
                int item = seq.get(pos);
                if (frequentItemSet.contains(item)) {
                    builders.get(item).registerBit(0, pos, cid);
                }
            }
        }

        for (int item : frequentItems) {
            onePatternIdLists.put(item, builders.get(item));
        }
    }

    /**
     * Starts DFS exploration from each frequent 1-pattern and materializes frequent candidates.
     */
    private void step3DFSMining() {
        logger.traceHeader("STEP 3: DFS - Explore Prefix equivalence Lattice (BitSets Enabled)");

        if (!storePatternsInMemory) {
            logger.ensureFile("Extracted_Patterns.txt");
        }

        for (int item : frequentItems) {
            int sup = onePatternIdLists.get(item).getSupport();
            registerFrequentPattern(Collections.singletonList(item), sup);
        }

        for (int i = 0; i < frequentItems.size(); i++) {
            int itemA = frequentItems.get(i);
            List<PatternClass> classMembers = new ArrayList<>();

            for (int j = 0; j < frequentItems.size(); j++) {
                int itemB = frequentItems.get(j);
                List<Integer> cand = null;
                if (storePatternsInMemory || logger.isTraceEnabled()) {
                    cand = Arrays.asList(itemA, itemB);
                }

                if (cand != null && storePatternsInMemory && frequentPatterns.containsKey(cand)) {
                    continue;
                }

                if (!dubCheckFast(onePatternCidBitsets.get(itemA), onePatternCidBitsets.get(itemB), cand)) {
                    continue;
                }

                ProjectedIdList joined = joinIdLists(onePatternIdLists.get(itemA), onePatternIdLists.get(itemB));
                if (joined == null) {
                    continue;
                }

                int sup = joined.getSupport();
                if (sup >= absoluteMinSupport) {
                    if (cand == null) {
                        cand = Arrays.asList(itemA, itemB);
                    }
                    registerFrequentPattern(cand, sup);
                    classMembers.add(new PatternClass(
                            cand,
                            joined,
                            createLocalBitmap(onePatternIdLists.get(itemA), joined)));
                }
            }

            if (!classMembers.isEmpty()) {
                dfsExpand(classMembers);
            }
        }
    }

    /**
     * Recursively expands equivalence classes to discover deeper sequential patterns.
     */
    private void dfsExpand(List<PatternClass> classMembers) {
        if (classMembers.isEmpty()) {
            return;
        }

        for (int i = 0; i < classMembers.size(); i++) {
            PatternClass pcA = classMembers.get(i);
            List<PatternClass> newClass = new ArrayList<>();

            for (int j = 0; j < classMembers.size(); j++) {
                PatternClass pcB = classMembers.get(j);

                int lastB = (i == j)
                        ? pcA.pattern.get(pcA.pattern.size() - 1)
                        : pcB.pattern.get(pcB.pattern.size() - 1);

                List<Integer> cand = null;
                if (storePatternsInMemory || logger.isTraceEnabled()) {
                    cand = new ArrayList<>(pcA.pattern);
                    cand.add(lastB);
                }

                if (cand != null && storePatternsInMemory && frequentPatterns.containsKey(cand)) {
                    continue;
                }

                if (!dubCheckLocal(pcA.localIdBitmap, pcB.localIdBitmap, cand)) {
                    continue;
                }

                ProjectedIdList joined = joinIdLists(pcA.idList, pcB.idList);
                if (joined == null) {
                    continue;
                }

                int sup = joined.getSupport();
                if (sup >= absoluteMinSupport) {
                    if (cand == null) {
                        cand = new ArrayList<>(pcA.pattern);
                        cand.add(lastB);
                    }
                    registerFrequentPattern(cand, sup);
                    newClass.add(new PatternClass(
                            cand,
                            joined,
                            createLocalBitmap(pcA.idList, joined)));
                }
            }

            if (!newClass.isEmpty()) {
                dfsExpand(newClass);
            }
        }
    }

    /**
     * Applies DUB pruning by intersecting CID bitsets before expensive temporal joins.
     */
    private boolean dubCheckFast(BitSet bsA, BitSet bsB, List<Integer> debugName) {
        BitSet inter = (BitSet) bsA.clone();
        inter.and(bsB);

        int size = inter.cardinality();
        boolean ok = size >= absoluteMinSupport;

        if (!ok) {
            dubPruneCount++;
            if (logger.isTraceEnabled()) {
                logger.trace("      DUB Fast-Prune: " + debugName + " -> |S?|=" + size + " < " + absoluteMinSupport + " -> PRUNE");
            }
        }
        return ok;
    }

    /**
     * Applies DUB in the local-id universe of the siblings' shared parent.
     * The universe therefore shrinks together with the parent pseudo-IDList.
     */
    private boolean dubCheckLocal(
            LocalDUBBitmap bitmapA,
            LocalDUBBitmap bitmapB,
            List<Integer> debugName) {
        int size = bitmapA.intersectionSupport(bitmapB);
        boolean ok = size >= absoluteMinSupport;
        if (!ok) {
            dubPruneCount++;
            if (logger.isTraceEnabled()) {
                logger.trace("      DUB Local-Prune: " + debugName
                        + " -> |S?|=" + size
                        + " < " + absoluteMinSupport
                        + " (local universe=" + bitmapA.getUniverseSize() + ") -> PRUNE");
            }
        }
        return ok;
    }

    /**
     * Joins two projected IDLists and returns null when the candidate cannot be frequent.
     */
    private ProjectedIdList joinIdLists(
            ProjectedIdList left,
            ProjectedIdList right) {
        joinCount++;
        JoinableIdList raw = left.join(right, false, absoluteMinSupport);
        if (!(raw instanceof ProjectedIdList)) {
            return null;
        }
        ProjectedIdList projection = (ProjectedIdList) raw;
        if (projection.getSupport() < absoluteMinSupport) {
            return null;
        }
        return projection;
    }

    /** Creates and records a DUB bitmap relative to the supplied parent IDList. */
    private LocalDUBBitmap createLocalBitmap(
            ProjectedIdList parent,
            ProjectedIdList child) {
        if (!(parent instanceof PseudoIdList) || !(child instanceof PseudoIdList)) {
            throw new IllegalArgumentException("Local-id DUB requires PseudoIdList");
        }
        LocalDUBBitmap bitmap = LocalDUBBitmap.fromParentAndChild(
                (PseudoIdList) parent,
                (PseudoIdList) child);
        localBitmapCount++;
        localBitmapUniverseBits += bitmap.getUniverseSize();
        maxLocalBitmapUniverseBits = Math.max(maxLocalBitmapUniverseBits, bitmap.getUniverseSize());
        return bitmap;
    }

    /**
     * Exports discovered frequent patterns in SPMF-compatible support format.
     */
    private void exportExtractedPatterns() {
        if (!storePatternsInMemory) {
            // Already exported on-the-fly
            return;
        }
        logger.ensureFile("Extracted_Patterns.txt");
        List<Map.Entry<List<Integer>, Integer>> sorted = new ArrayList<>(frequentPatterns.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<List<Integer>, Integer> e) -> e.getKey().size())
                .thenComparing(e -> e.getKey().toString()));

        for (Map.Entry<List<Integer>, Integer> entry : sorted) {
            StringBuilder spmf = new StringBuilder();
            for (int item : entry.getKey()) {
                spmf.append(item).append(" -1 ");
            }
            spmf.append("-2 #SUP: ").append(entry.getValue());
            logger.log("Extracted_Patterns.txt", spmf.toString());
        }
    }

    /**
     * Exports per-pattern-length support statistics for reproducible quantitative analysis.
     */
    private void exportPatternLengthProfile() {
        logger.log("Pattern_Length_Profile.tsv", "pattern_length\tpattern_count\tmin_support\tmax_support\tavg_support");
        for (Map.Entry<Integer, IntSupportAccumulator> entry : patternLengthProfile.entrySet()) {
            int length = entry.getKey();
            IntSupportAccumulator stats = entry.getValue();
            logger.log("Pattern_Length_Profile.tsv", String.format(Locale.US,
                    "%d\t%d\t%d\t%d\t%.6f",
                    length,
                    stats.count,
                    stats.minSupport,
                    stats.maxSupport,
                    stats.getAverage()));
        }
    }

    /**
     * Exports machine-readable run metrics to support long-term cross-algorithm benchmarking.
     */
    private void exportResearchMetrics(long elapsedMillis) {
        logger.log("Metrics_Research.tsv", "metric\tvalue\tunit");
        logger.log("Metrics_Research.tsv", "algorithm\tCUP\t-");
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "relative_minsup\t%.6f\tratio", relativeMinSupport));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "absolute_minsup\t%d\tsequence", absoluteMinSupport));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "runtime_ms\t%d\tms", elapsedMillis));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "frequent_pattern_count\t%d\tpattern", frequentPatternCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "temporal_join_count\t%d\tjoin", joinCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "dub_prune_count\t%d\tcandidate", dubPruneCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "local_dub_bitmap_count\t%d\tbitmap", localBitmapCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "local_dub_universe_bits_total\t%d\tbit", localBitmapUniverseBits));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "local_dub_universe_bits_max\t%d\tbit", maxLocalBitmapUniverseBits));
    }

    /**
     * Writes deterministic execution statistics for research-grade comparisons.
     */
    private void logStatisticsSummary(long elapsedMillis) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================\n");
        sb.append("   CUP Algorithm Execution Statistics\n");
        sb.append("=========================================================\n");
        sb.append("Execution time (ms): ").append(elapsedMillis).append("\n");
        sb.append("Relative minSup      : ").append(String.format(Locale.US, "%.4f", relativeMinSupport)).append("\n");
        sb.append("Absolute minSup      : ").append(absoluteMinSupport).append("\n");
        sb.append("Total frequent patterns: ").append(frequentPatternCount).append("\n");
        sb.append("Temporal joins executed: ").append(joinCount).append("\n");
        sb.append("BitSet UB prunes       : ").append(dubPruneCount).append("\n");
        sb.append("Local DUB bitmaps      : ").append(localBitmapCount).append("\n");
        sb.append("Max local bitmap bits  : ").append(maxLocalBitmapUniverseBits).append("\n");
        logger.log("Stats_Summary.txt", sb.toString());
    }

    /**
     * Prints a concise execution summary to standard output.
     */
    private void printFinalResults(long elapsedMillis) {
        System.out.println("=========================================================");
        System.out.println("Paper 1 Result Summary: CUP (Pseudo-IDList + DUB)");
        System.out.println("=========================================================");
        System.out.println("Runtime              : " + elapsedMillis + " ms");
        System.out.println("Relative minSup      : " + String.format(Locale.US, "%.4f", relativeMinSupport) + " (" + absoluteMinSupport + " sequences)");
        System.out.println("Total Temporal Joins : " + joinCount);
        System.out.println("UB Pruned Elements   : " + dubPruneCount);
        System.out.println("Local DUB Bitmaps    : " + localBitmapCount);
        System.out.println("Max Local Bitmap Bits: " + maxLocalBitmapUniverseBits);
        System.out.println("Frequent Patterns    : " + frequentPatternCount);
        System.out.println("\nOutput artifact      : " + logger.getOutputDir() + "/Extracted_Patterns.txt");
    }

    private static class PatternClass {
        List<Integer> pattern;
        ProjectedIdList idList;
        LocalDUBBitmap localIdBitmap;

        PatternClass(
                List<Integer> pattern,
                ProjectedIdList idList,
                LocalDUBBitmap localIdBitmap) {
            this.pattern = pattern;
            this.idList = idList;
            this.localIdBitmap = localIdBitmap;
        }
    }

    public Map<List<Integer>, Integer> getFrequentPatternsSnapshot() {
        return new LinkedHashMap<>(frequentPatterns);
    }

    public int getLocalBitmapCount() {
        return localBitmapCount;
    }

    public int getMaxLocalBitmapUniverseBits() {
        return maxLocalBitmapUniverseBits;
    }

    /**
     * Accumulates integer support statistics without boxing overhead.
     */
    private static final class IntSupportAccumulator {
        private int count = 0;
        private int minSupport = Integer.MAX_VALUE;
        private int maxSupport = Integer.MIN_VALUE;
        private long sumSupport = 0L;

        private void add(int support) {
            count++;
            minSupport = Math.min(minSupport, support);
            maxSupport = Math.max(maxSupport, support);
            sumSupport += support;
        }

        private double getAverage() {
            return count == 0 ? 0.0 : ((double) sumSupport) / count;
        }
    }
}
