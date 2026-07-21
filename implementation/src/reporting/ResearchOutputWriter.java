package reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manage standardized output logging across multiple text files.
 * Adheres to international research standards: separates statistical summaries,
 * extracted patterns (SPMF format), and detailed algorithmic trace steps.
 */
public class ResearchOutputWriter {
    private final String outputDir;
    private final Map<String, BufferedWriter> writers;
    private boolean traceEnabled = true;

    public ResearchOutputWriter(String outputDir) {
        this.outputDir = outputDir;
        this.writers = new HashMap<>();
        
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Disable tracing if system property "noTrace" or env var "NO_TRACE" is true
        if ("true".equalsIgnoreCase(System.getProperty("noTrace")) ||
            "true".equalsIgnoreCase(System.getenv("NO_TRACE"))) {
            this.traceEnabled = false;
        }
    }

    public ResearchOutputWriter(String outputDir, boolean traceEnabled) {
        this.outputDir = outputDir;
        this.writers = new HashMap<>();
        
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.traceEnabled = traceEnabled;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    /**
     * Appends a message to a specific file within the Output directory.
     * @param filename Target file name (e.g., "Extracted_Patterns.txt").
     * @param message Message payload.
     */
    public void log(String filename, String message) {
        try {
            BufferedWriter bw = writers.computeIfAbsent(filename, k -> {
                try {
                    return new BufferedWriter(new FileWriter(new File(outputDir, k), false)); // overwrite mode
                } catch (IOException e) {
                    System.err.println("Failed to create writer for " + k);
                    return null;
                }
            });

            if (bw != null) {
                bw.write(message);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to write to " + filename);
        }
    }

    /**
     * Convenience method for detailed algorithmic traces.
     */
    public void trace(String message) {
        if (traceEnabled) {
            log("Detailed_Execution_Trace.txt", message);
        }
    }

    /**
     * Convenience method for standardized section headers in trace log.
     */
    public void traceHeader(String title) {
        if (traceEnabled) {
            trace("");
            trace("=======================================================================");
            trace("  " + title);
            trace("=======================================================================");
        }
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void ensureFile(String filename) {
        try {
            BufferedWriter bw = writers.computeIfAbsent(filename, k -> {
                try {
                    return new BufferedWriter(new FileWriter(new File(outputDir, k), false));
                } catch (IOException e) {
                    System.err.println("Failed to create writer for " + k);
                    return null;
                }
            });
            if (bw != null) {
                bw.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize " + filename);
        }
    }

    /**
     * Closes all active file streams.
     */
    public void close() {
        for (BufferedWriter bw : writers.values()) {
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException ignored) {}
            }
        }
        writers.clear();
    }
}
