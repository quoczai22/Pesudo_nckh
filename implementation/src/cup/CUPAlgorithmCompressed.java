package cup;

import cmap.CoOccurrenceMap;
import data.ClickstreamSequenceDatabase;
import idlist.JoinableIdList;
import idlist.CompressedDUBBitmap;
import idlist.ProjectedIdList;
import idlist.PseudoIdList;
import idlist.IntArrayBuffer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Optimized version of Paper 1 CUP Algorithm using compressed bitsets (CompressedDUBBitmap).
 */
public class CUPAlgorithmCompressed {
    private final double relativeMinSupport;
    private int absoluteMinSupport;
    private final ResearchOutputWriter logger;

    private final List<Integer> frequentItems;
    private final Map<Integer, ProjectedIdList> onePatternIdLists;
    private final Map<Integer, int[]> onePatternCidArrays;
    private final Map<List<Integer>, Integer> frequentPatterns;

    private int dubPruneCount = 0;
    private int cmapPruneCount = 0;
    private int joinCount = 0;
    private int localBitmapCount = 0;
    private long localBitmapUniverseBits = 0L;
    private int maxLocalBitmapUniverseBits = 0;
    private long lastProgressPrint = 0L;
    private long cmapBuildTimeMillis = 0L;
    private double maxMemoryMb = 0.0;

    private boolean storePatternsInMemory = true;
    private boolean writePatternsToFile = true;
    private boolean useCmap = false;
    private CoOccurrenceMap coOccurrenceMap = null;
    private int frequentPatternCount = 0;
    private final Map<Integer, IntSupportAccumulator> patternLengthProfile = new TreeMap<>();

    public CUPAlgorithmCompressed(double relativeMinSupport, ResearchOutputWriter logger) {
        this.relativeMinSupport = relativeMinSupport;
        this.logger = logger;
        this.frequentItems = new ArrayList<>();
        this.onePatternIdLists = new HashMap<>();
        this.onePatternCidArrays = new HashMap<>();
        this.frequentPatterns = new LinkedHashMap<>();
    }

    public void setStorePatternsInMemory(boolean storePatternsInMemory) {
        this.storePatternsInMemory = storePatternsInMemory;
    }

    public void setUseCmap(boolean useCmap) {
        this.useCmap = useCmap;
    }

    public void setWritePatternsToFile(boolean writePatternsToFile) {
        this.writePatternsToFile = writePatternsToFile;
    }

    private void registerFrequentPattern(List<Integer> pattern, int support) {
        frequentPatternCount++;
        int length = pattern.size();
        patternLengthProfile.computeIfAbsent(length, k -> new IntSupportAccumulator()).add(support);

        if (storePatternsInMemory) {
            frequentPatterns.put(pattern, support);
        } else if (writePatternsToFile) {
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
        if (useCmap) {
            long cmapStart = System.currentTimeMillis();
            coOccurrenceMap = CoOccurrenceMap.buildAfterMap(db.getSequences(), new HashSet<>(frequentItems));
            cmapBuildTimeMillis = System.currentTimeMillis() - cmapStart;
            logger.trace("CMAP built in " + cmapBuildTimeMillis + " ms");
        }
        step3DFSMining();

        long elapsed = System.currentTimeMillis() - startTime;
        maxMemoryMb = readPeakHeapMemoryMb();
        exportExtractedPatterns();
        exportPatternLengthProfile();
        exportResearchMetrics(elapsed);
        printFinalResults(elapsed);
        logStatisticsSummary(elapsed);
    }

    /**
     * Scans the sequence database to obtain frequent 1-patterns and their CID arrays.
     */
    private void step1ScanDB(List<List<Integer>> sequences) {
        logger.traceHeader("STEP 1: SCAN DATABASE - Count 1-patterns");

        Map<Integer, IntArrayBuffer> itemCidBuffers = new HashMap<>();
        for (int cid = 0; cid < sequences.size(); cid++) {
            for (int item : sequences.get(cid)) {
                IntArrayBuffer buf = itemCidBuffers.computeIfAbsent(item, k -> new IntArrayBuffer());
                if (buf.isEmpty() || buf.get(buf.size() - 1) != cid) {
                    buf.add(cid);
                }
            }
        }

        List<Integer> sortedItems = new ArrayList<>(itemCidBuffers.keySet());
        Collections.sort(sortedItems);

        for (int item : sortedItems) {
            IntArrayBuffer buf = itemCidBuffers.get(item);
            int sup = buf.size();
            String status = (sup >= absoluteMinSupport) ? "OK FREQUENT" : "X INFREQUENT";
            logger.trace("  Item " + item + ": CIDsCount=" + sup + " " + status);
            if (sup >= absoluteMinSupport) {
                frequentItems.add(item);
                // Convert buffer to primitive array
                int[] cidArray = new int[buf.size()];
                for (int i = 0; i < cidArray.length; i++) {
                    cidArray[i] = buf.get(i);
                }
                onePatternCidArrays.put(item, cidArray);
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

        if (!storePatternsInMemory && writePatternsToFile) {
            logger.ensureFile("Extracted_Patterns.txt");
        }

        for (int item : frequentItems) {
            int sup = onePatternIdLists.get(item).getSupport();
            registerFrequentPattern(Collections.singletonList(item), sup);
        }

        lastProgressPrint = System.currentTimeMillis();
        for (int i = 0; i < frequentItems.size(); i++) {
            int itemA = frequentItems.get(i);
            
            if (System.currentTimeMillis() - lastProgressPrint > 4000) {
                System.out.println("-> DFS progress: item " + (i + 1) + "/" + frequentItems.size() +
                        " | Patterns found: " + frequentPatternCount +
                        " | Joins: " + joinCount + " | UB prunes: " + dubPruneCount
                        + " | CMAP prunes: " + cmapPruneCount);
                lastProgressPrint = System.currentTimeMillis();
            }
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

                if (!cmapAllows(itemA, itemB, cand)) {
                    continue;
                }

                if (!dubCheckFast(onePatternCidArrays.get(itemA), onePatternCidArrays.get(itemB), cand)) {
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
            
            if (System.currentTimeMillis() - lastProgressPrint > 4000) {
                System.out.println("-> DFS progress: Patterns found: " + frequentPatternCount +
                        " | Joins: " + joinCount + " | UB prunes: " + dubPruneCount
                        + " | CMAP prunes: " + cmapPruneCount);
                lastProgressPrint = System.currentTimeMillis();
            }
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

                int lastA = pcA.pattern.get(pcA.pattern.size() - 1);
                if (!cmapAllows(lastA, lastB, cand)) {
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
     * Applies DUB pruning by intersecting CID arrays using linear merge-intersection.
     */
    private boolean dubCheckFast(int[] a, int[] b, List<Integer> debugName) {
        int count = 0;
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                count++;
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }

        boolean ok = count >= absoluteMinSupport;
        if (!ok) {
            dubPruneCount++;
            if (logger.isTraceEnabled()) {
                logger.trace("      DUB Fast-Prune: " + debugName + " -> |S?|=" + count
                        + " < " + absoluteMinSupport + " -> PRUNE");
            }
        }
        return ok;
    }

    /**
     * Uses CMAP as a database-level upper bound before materializing a temporal join.
     * If itemB does not appear after itemA in enough sequences, no extension ending
     * with itemB can reach the minimum support.
     */
    private boolean cmapAllows(int itemA, int itemB, List<Integer> debugName) {
        if (!useCmap || coOccurrenceMap == null) {
            return true;
        }
        int upperBound = coOccurrenceMap.getAfterSupport(itemA, itemB);
        boolean ok = upperBound >= absoluteMinSupport;
        if (!ok) {
            cmapPruneCount++;
            if (logger.isTraceEnabled()) {
                logger.trace("      CMAP-Prune: " + debugName
                        + " -> upperBound=" + upperBound
                        + " < " + absoluteMinSupport + " -> PRUNE");
            }
        }
        return ok;
    }

    /**
     * Applies DUB in the local-id universe of the siblings' shared parent.
     */
    private boolean dubCheckLocal(
            CompressedDUBBitmap bitmapA,
            CompressedDUBBitmap bitmapB,
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
     * Joins two projected IDLists.
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

    /** Creates and records a CompressedDUBBitmap. */
    private CompressedDUBBitmap createLocalBitmap(
            ProjectedIdList parent,
            ProjectedIdList child) {
        if (!(parent instanceof PseudoIdList) || !(child instanceof PseudoIdList)) {
            throw new IllegalArgumentException("Local-id DUB requires PseudoIdList");
        }
        PseudoIdList pseudoParent = (PseudoIdList) parent;
        PseudoIdList pseudoChild = (PseudoIdList) child;
        CompressedDUBBitmap bitmap = pseudoChild.hasProjectedLocalUcbFor(pseudoParent)
                ? CompressedDUBBitmap.fromProjectedLocalUcb(pseudoParent, pseudoChild)
                : CompressedDUBBitmap.fromParentAndChild(pseudoParent, pseudoChild);
        localBitmapCount++;
        localBitmapUniverseBits += bitmap.getUniverseSize();
        maxLocalBitmapUniverseBits = Math.max(maxLocalBitmapUniverseBits, bitmap.getUniverseSize());
        return bitmap;
    }

    private void exportExtractedPatterns() {
        if (!storePatternsInMemory) {
            // Already exported on-the-fly
            return;
        }
        if (!writePatternsToFile) {
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

    private void exportResearchMetrics(long elapsedMillis) {
        logger.log("Metrics_Research.tsv", "metric\tvalue\tunit");
        logger.log("Metrics_Research.tsv", "algorithm\t"
                + (useCmap ? "CUP-Compressed+CMAP" : "CUP-Compressed") + "\t-");
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "relative_minsup\t%.6f\tratio", relativeMinSupport));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "absolute_minsup\t%d\tsequence", absoluteMinSupport));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "runtime_ms\t%d\tms", elapsedMillis));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "max_memory_mb\t%.6f\tMB", maxMemoryMb));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "cmap_enabled\t%s\tboolean", useCmap));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "patterns_written\t%s\tboolean", writePatternsToFile));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "cmap_build_time_ms\t%d\tms", cmapBuildTimeMillis));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "cmap_prune_count\t%d\tcandidate", cmapPruneCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "frequent_pattern_count\t%d\tpattern", frequentPatternCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "temporal_join_count\t%d\tjoin", joinCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "dub_prune_count\t%d\tcandidate", dubPruneCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "local_dub_bitmap_count\t%d\tbitmap", localBitmapCount));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "local_dub_universe_bits_total\t%d\tbit", localBitmapUniverseBits));
        logger.log("Metrics_Research.tsv", String.format(Locale.US, "local_dub_universe_bits_max\t%d\tbit", maxLocalBitmapUniverseBits));
    }

    private void logStatisticsSummary(long elapsedMillis) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================\n");
        sb.append("   ").append(useCmap ? "CUP-Compressed+CMAP" : "CUP-Compressed")
                .append(" Algorithm Execution Statistics\n");
        sb.append("=========================================================\n");
        sb.append("Execution time (ms): ").append(elapsedMillis).append("\n");
        sb.append("Max memory (MB)    : ").append(String.format(Locale.US, "%.6f", maxMemoryMb)).append("\n");
        sb.append("Relative minSup      : ").append(String.format(Locale.US, "%.4f", relativeMinSupport)).append("\n");
        sb.append("Absolute minSup      : ").append(absoluteMinSupport).append("\n");
        sb.append("CMAP enabled         : ").append(useCmap).append("\n");
        sb.append("CMAP build time (ms) : ").append(cmapBuildTimeMillis).append("\n");
        sb.append("CMAP prunes          : ").append(cmapPruneCount).append("\n");
        sb.append("Total frequent patterns: ").append(frequentPatternCount).append("\n");
        sb.append("Temporal joins executed: ").append(joinCount).append("\n");
        sb.append("BitSet UB prunes       : ").append(dubPruneCount).append("\n");
        sb.append("Local DUB bitmaps      : ").append(localBitmapCount).append("\n");
        sb.append("Max local bitmap bits  : ").append(maxLocalBitmapUniverseBits).append("\n");
        logger.log("Stats_Summary.txt", sb.toString());
    }

    private void printFinalResults(long elapsedMillis) {
        System.out.println("=========================================================");
        System.out.println("Result Summary: "
                + (useCmap ? "CUP-Compressed+CMAP" : "CUP-Compressed")
                + " (Pseudo-IDList + Compressed DUB)");
        System.out.println("=========================================================");
        System.out.println("Runtime              : " + elapsedMillis + " ms");
        System.out.println("Max Memory           : " + String.format(Locale.US, "%.6f", maxMemoryMb) + " MB");
        System.out.println("Relative minSup      : " + String.format(Locale.US, "%.4f", relativeMinSupport) + " (" + absoluteMinSupport + " sequences)");
        System.out.println("CMAP Enabled         : " + useCmap);
        System.out.println("CMAP Build Time      : " + cmapBuildTimeMillis + " ms");
        System.out.println("CMAP Pruned Elements : " + cmapPruneCount);
        System.out.println("Total Temporal Joins : " + joinCount);
        System.out.println("UB Pruned Elements   : " + dubPruneCount);
        System.out.println("Local DUB Bitmaps    : " + localBitmapCount);
        System.out.println("Max Local Bitmap Bits: " + maxLocalBitmapUniverseBits);
        System.out.println("Frequent Patterns    : " + frequentPatternCount);
        if (writePatternsToFile) {
            System.out.println("\nOutput artifact      : " + logger.getOutputDir() + "/Extracted_Patterns.txt");
        } else {
            System.out.println("\nOutput artifact      : pattern file disabled for benchmark; metrics still exported");
        }
    }

    /**
     * Reads the JVM peak heap usage reported by memory pools.
     */
    private double readPeakHeapMemoryMb() {
        long totalPeakBytes = 0L;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.getPeakUsage() != null) {
                totalPeakBytes += pool.getPeakUsage().getUsed();
            }
        }
        return totalPeakBytes / 1024.0 / 1024.0;
    }
    private static class PatternClass {
        List<Integer> pattern;
        ProjectedIdList idList;
        CompressedDUBBitmap localIdBitmap;

        PatternClass(
                List<Integer> pattern,
                ProjectedIdList idList,
                CompressedDUBBitmap localIdBitmap) {
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


