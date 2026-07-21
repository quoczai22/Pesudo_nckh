package main;

import cup.CUPAlgorithm;
import data.ClickstreamSequenceDatabase;
import java.io.IOException;
import reporting.ResearchOutputWriter;

/**
 * Main Entry Point for Paper 1 (CUP Algorithm).
 * Parses Input argument relative min_sup mapped down to integers directly.
 */
public class MainCUP {
    private static final String OUTPUT_ROOT = "output";
    private static final String FIFA_PATH = "datasets/FIFA.txt";
    private static final String BMS2_PATH = "datasets/BMS2.txt";
    private static final String KOSARAK_PATH = "datasets/Kosarak.txt";
    private static final String MSNBC_PATH = "datasets/MSNBC.txt";

    private static boolean noTrace = true;

    /**
     * Boots the Paper 1 pipeline, parses CLI arguments, and executes CUP end-to-end.
     */
    public static void main(String[] args) {
        String dataFile = null;
        String datasetKey = null;
        String outputDir = null;
        double minSupport = 0.00001; // default relative minimum support

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--data") && i + 1 < args.length) {
                dataFile = args[i + 1];
            } else if (args[i].equals("--dataset") && i + 1 < args.length) {
                datasetKey = args[i + 1].toLowerCase();
            } else if (args[i].equals("--sup") && i + 1 < args.length) {
                minSupport = Double.parseDouble(args[i + 1]);
            } else if (args[i].equals("--out") && i + 1 < args.length) {
                outputDir = args[i + 1];
            } else if (args[i].equals("--no-trace")) {
                noTrace = true;
            }
        }

        if (datasetKey != null) {
            runNamedDataset(datasetKey, minSupport, outputDir);
            return;
        }

        if (dataFile != null) {
            runDataset(dataFile, outputDir != null ? outputDir : OUTPUT_ROOT, minSupport);
            return;
        }

        throw new IllegalArgumentException(
                "Provide either --dataset fifa|bms2|kosarak|msnbc or --data <path>. Use --no-trace to disable detailed trace.");
    }

    private static void runNamedDataset(String datasetKey, double minSupport, String customOutputDir) {
        switch (datasetKey) {
            case "fifa":
                runFifa(minSupport, customOutputDir);
                return;
            case "bms2":
                runBms2(minSupport, customOutputDir);
                return;
            case "kosarak":
                runKosarak(minSupport, customOutputDir);
                return;
            case "msnbc":
                runMsnbc(minSupport, customOutputDir);
                return;
            default:
                throw new IllegalArgumentException(
                        "Unknown dataset key: " + datasetKey + ". Use fifa, bms2, kosarak, or msnbc.");
        }
    }

    public static void runFifa(double minSupport, String customOutputDir) {
        runDataset(FIFA_PATH, resolveOutputDir(customOutputDir, "fifa"), minSupport);
    }

    public static void runBms2(double minSupport, String customOutputDir) {
        runDataset(BMS2_PATH, resolveOutputDir(customOutputDir, "bms2"), minSupport);
    }

    public static void runKosarak(double minSupport, String customOutputDir) {
        runDataset(KOSARAK_PATH, resolveOutputDir(customOutputDir, "kosarak"), minSupport);
    }

    public static void runMsnbc(double minSupport, String customOutputDir) {
        runDataset(MSNBC_PATH, resolveOutputDir(customOutputDir, "msnbc"), minSupport);
    }

    private static String resolveOutputDir(String customOutputDir, String datasetName) {
        return customOutputDir != null ? customOutputDir : OUTPUT_ROOT + "/" + datasetName;
    }

    private static void runDataset(String dataFile, String outputDir, double minSupport) {
        System.out.println("=========================================================");
        System.out.println("CUP Algorithm (Paper 1)");
        System.out.println("=========================================================");
        System.out.println("Dataset                 : " + dataFile);
        System.out.println("Relative minSupport     : " + minSupport);
        System.out.println("Output directory        : " + outputDir);
        System.out.println("Execution status        : initialized\n");

        ResearchOutputWriter logger = new ResearchOutputWriter(outputDir);
        if (noTrace) {
            logger.setTraceEnabled(false);
        }
        ClickstreamSequenceDatabase db = new ClickstreamSequenceDatabase();

        try {
            db.loadFile(dataFile, logger);
            CUPAlgorithm algo = new CUPAlgorithm(minSupport, logger);
            algo.setStorePatternsInMemory(false);
            algo.run(db);
        } catch (IOException e) {
            System.err.println("Fatal execution failure while loading input data: " + e.getMessage());
        } finally {
            logger.close();
        }
    }
}
