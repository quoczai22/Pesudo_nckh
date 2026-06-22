package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import reporting.ResearchOutputWriter;

/**
 * Parses and stores sequences in SPMF format.
 */
public class ClickstreamSequenceDatabase {
    private static final Pattern TUPLE_PATTERN = Pattern.compile("\\((\\d+)\\s*,\\s*[^\\)]*\\)");

    // A list of sequences, where each sequence is a list of integers.
    private List<List<Integer>> sequences;

    public ClickstreamSequenceDatabase() {
        this.sequences = new ArrayList<>();
    }

    /**
     * Loads sequences from a file formatted according to the SPMF standard.
     * Each item is separated by "-1", and a sequence ends with "-2".
     *
     * @param filepath The path to the database file.
     * @param logger A file logger to output reading details (Section 4 info).
     */
    public void loadFile(String filepath, ResearchOutputWriter logger) throws IOException {
        sequences.clear();
        logger.traceHeader("STEP 0: READING DATABASE");
        logger.trace("Input parser mode: AUTO (supports SPMF and tuple TF-IDF sequences)");

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int cid = 0;
            int skippedLines = 0;
            int tupleParsedLines = 0;
            int tokenStreamParsedLines = 0;
            int malformedTokenCount = 0;
            int malformedTokenLines = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("%") || line.startsWith("@")) {
                    continue;
                }

                ParsedSequence parsed = parseSequence(line);
                List<Integer> seq = parsed.items;
                if (!seq.isEmpty()) {
                    sequences.add(seq);
                    logger.trace("  CID " + cid + " length: " + seq.size() + " | payload: " + seq.toString());
                    if (parsed.source == ParseSource.TUPLE) {
                        tupleParsedLines++;
                    } else if (parsed.source == ParseSource.TOKEN_STREAM) {
                        tokenStreamParsedLines++;
                    }
                    cid++;
                } else {
                    skippedLines++;
                }

                if (parsed.malformedTokenCount > 0) {
                    malformedTokenCount += parsed.malformedTokenCount;
                    malformedTokenLines++;
                }
            }
            logger.trace("Skipped non-parseable lines: " + skippedLines);
            logger.trace("Parsed tuple TF-IDF lines: " + tupleParsedLines);
            logger.trace("Parsed token-stream lines: " + tokenStreamParsedLines);
            logger.trace("Malformed numeric tokens encountered: " + malformedTokenCount + " across " + malformedTokenLines + " lines");

            int processedLines = tupleParsedLines + tokenStreamParsedLines + skippedLines;
            if (malformedTokenLines > 0 && processedLines > 0) {
                double malformedLineRatio = (100.0 * malformedTokenLines) / processedLines;
                logger.trace(String.format(Locale.US,
                        "WARNING: %.2f%% of processed lines contain malformed numeric payloads.",
                        malformedLineRatio));
            }
        }
        logger.trace("Read " + sequences.size() + " clickstreams in total.");
    }

    /**
     * Parses one sequence line by trying tuple format first, then SPMF fallback.
     */
    private ParsedSequence parseSequence(String line) {
        ParsedSequence tupleSeq = parseTupleSequence(line);
        if (!tupleSeq.items.isEmpty()) {
            return tupleSeq;
        }
        return parseTokenStreamSequence(line);
    }

    /**
     * Extracts token IDs from tuple payloads such as "(token,tfidf)".
     */
    private ParsedSequence parseTupleSequence(String line) {
        List<Integer> seq = new ArrayList<>();
        Matcher matcher = TUPLE_PATTERN.matcher(line);
        while (matcher.find()) {
            seq.add(Integer.parseInt(matcher.group(1)));
        }
        if (seq.isEmpty()) {
            return ParsedSequence.empty();
        }
        return new ParsedSequence(seq, 0, ParseSource.TUPLE);
    }

    /**
     * Parses classic SPMF sequences where -1 separates itemsets and -2 terminates sequences.
     */
    private ParsedSequence parseTokenStreamSequence(String line) {
        List<Integer> seq = new ArrayList<>();
        int malformedTokens = 0;

        String[] tokens = line.split("\\s+");
        for (String token : tokens) {
            if (token.equals("-2")) {
                break;
            }
            if (token.equals("-1")) {
                continue;
            }

            String itemToken = token;
            int separatorIndex = token.indexOf(':');
            if (separatorIndex > 0) {
                itemToken = token.substring(0, separatorIndex);
            }

            try {
                seq.add(Integer.parseInt(itemToken));
            } catch (NumberFormatException ignored) {
                if (containsDigit(itemToken)) {
                    malformedTokens++;
                }
            }
        }

        if (seq.isEmpty() && malformedTokens == 0) {
            return ParsedSequence.empty();
        }
        return new ParsedSequence(seq, malformedTokens, ParseSource.TOKEN_STREAM);
    }

    /**
     * Identifies token fragments that are expected to be numeric values.
     */
    private boolean containsDigit(String token) {
        for (int i = 0; i < token.length(); i++) {
            if (Character.isDigit(token.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Captures parser output together with diagnostics to avoid silent data-quality failures.
     */
    private static final class ParsedSequence {
        private final List<Integer> items;
        private final int malformedTokenCount;
        private final ParseSource source;

        private ParsedSequence(List<Integer> items, int malformedTokenCount, ParseSource source) {
            this.items = items;
            this.malformedTokenCount = malformedTokenCount;
            this.source = source;
        }

        private static ParsedSequence empty() {
            return new ParsedSequence(new ArrayList<>(), 0, ParseSource.NONE);
        }
    }

    /**
     * Marks which parsing branch produced a sequence for traceability and reproducibility.
     */
    private enum ParseSource {
        TUPLE,
        TOKEN_STREAM,
        NONE
    }

    public List<List<Integer>> getSequences() {
        return sequences;
    }
}
