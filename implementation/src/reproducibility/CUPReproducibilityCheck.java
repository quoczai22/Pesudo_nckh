package reproducibility;

import cup.CUPAlgorithm;
import data.ClickstreamSequenceDatabase;
import idlist.LocalDUBBitmap;
import idlist.PseudoIdList;
import reporting.ResearchOutputWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain-Java correctness checks; no external test framework is required. */
public final class CUPReproducibilityCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: CUPReproducibilityCheck <paper_example.txt> <output-dir>");
        }
        testLocalBitmapShrinks();
        testPaperExample(args[0], args[1]);
        System.out.println("CUPReproducibilityCheck: PASS");
    }

    private static void testLocalBitmapShrinks() {
        PseudoIdList parent = idList(0, 1, 2, 3);
        PseudoIdList childA = idList(0, 1, 3);
        PseudoIdList childB = idList(0, 1, 2, 3);

        LocalDUBBitmap a = LocalDUBBitmap.fromParentAndChild(parent, childA);
        LocalDUBBitmap b = LocalDUBBitmap.fromParentAndChild(parent, childB);
        assertEquals(4, a.getUniverseSize(), "level-2 local universe");
        assertEquals(3, a.intersectionSupport(b), "DUB intersection support");

        PseudoIdList grandChild = idList(0, 3);
        LocalDUBBitmap shrunk = LocalDUBBitmap.fromParentAndChild(childA, grandChild);
        assertEquals(3, shrunk.getUniverseSize(), "deeper local universe must shrink with parent");
        assertEquals(2, shrunk.cardinality(), "grandchild support bitmap");

        LocalDUBBitmap unrelated = LocalDUBBitmap.fromParentAndChild(idList(4, 5, 6, 7), idList(4, 5, 7));
        try {
            a.intersectionSupport(unrelated);
            throw new AssertionError("Bitmaps from different parents must not be intersected");
        } catch (IllegalArgumentException expected) {
            // Expected: equal lengths do not imply an equal local-id mapping.
        }
    }

    private static void testPaperExample(String dataset, String outputDir) throws Exception {
        new File(outputDir).mkdirs();
        ResearchOutputWriter logger = new ResearchOutputWriter(outputDir);
        CUPAlgorithm algorithm;
        try {
            ClickstreamSequenceDatabase database = new ClickstreamSequenceDatabase();
            database.loadFile(dataset, logger);
            algorithm = new CUPAlgorithm(0.6, logger);
            algorithm.run(database);
        } finally {
            logger.close();
        }

        Map<List<Integer>, Integer> expected = new LinkedHashMap<>();
        add(expected, 4, 1);
        add(expected, 5, 2);
        add(expected, 3, 3);
        add(expected, 3, 4);
        add(expected, 3, 1, 1);
        add(expected, 4, 1, 2);
        add(expected, 3, 1, 4);
        add(expected, 4, 2, 2);
        add(expected, 3, 2, 3);
        add(expected, 3, 2, 4);
        add(expected, 3, 3, 2);
        add(expected, 3, 4, 1);
        add(expected, 3, 4, 2);
        add(expected, 3, 1, 1, 2);
        add(expected, 3, 2, 3, 2);

        Map<List<Integer>, Integer> actual = algorithm.getFrequentPatternsSnapshot();
        if (!expected.equals(actual)) {
            throw new AssertionError("Exact pattern/support mismatch. Expected=" + expected + ", actual=" + actual);
        }
        if (algorithm.getLocalBitmapCount() <= 0) {
            throw new AssertionError("DFS did not create local-id DUB bitmaps");
        }
        if (algorithm.getMaxLocalBitmapUniverseBits() > 5) {
            throw new AssertionError("Local bitmap unexpectedly exceeded database sequence count");
        }
    }

    private static PseudoIdList idList(int... sequenceIds) {
        PseudoIdList list = new PseudoIdList();
        for (int sequenceId : sequenceIds) {
            list.registerBit(0, 1, sequenceId);
        }
        return list;
    }

    private static void add(Map<List<Integer>, Integer> patterns, int support, Integer... items) {
        patterns.put(new ArrayList<>(Arrays.asList(items)), support);
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
