package com.tlc.search.internal.analyzer.opensearch.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.AnalysisModel;
import org.opensearch.client.opensearch._types.analysis.Analyzer;

/**
 * <p>
 * Built-in {@link Analyzer}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum BuiltInAnalyzer implements AnalysisModel {

    STANDARD("standard", 1, new Analyzer.Builder().standard(builder -> builder).build()),
    SIMPLE("simple", 2, new Analyzer.Builder().simple(builder -> builder).build()),
    WHITESPACE("whitespace", 3, new Analyzer.Builder().whitespace(builder -> builder).build()),
    STOP("stop", 4, new Analyzer.Builder().stop(builder -> builder).build()),
    KEYWORD("keyword", 5, new Analyzer.Builder().keyword(builder -> builder).build()),
    PATTERN("pattern", 6, new Analyzer.Builder().pattern(builder -> builder).build()),
    FINGERPRINT("fingerprint", 7, new Analyzer.Builder().fingerprint(builder -> builder).build()),
    LANGUAGE("language", 8, new Analyzer.Builder().language(builder -> builder).build());

    private final Analyzer analyzer;
    private final String name;
    private final short id;

    BuiltInAnalyzer(final String name, final int id, final Analyzer analyzer) {
        this.analyzer = analyzer;
        this.name = name;
        this.id = (short) id;
    }

    /**
     * {@inheritDoc}
     */
    public short getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the {@link Analyzer}.
     * </p>
     *
     * @return {@link Analyzer}.
     */
    public Analyzer get() {
        return analyzer;
    }

    /**
     * <p>
     * Gets {@link BuiltInAnalyzer}.
     * </p>
     *
     * @param id Represents id.
     * @return {@link BuiltInAnalyzer}.
     */
    public static BuiltInAnalyzer get(final short id) {

        return switch (id) {
            case 1 -> STANDARD;
            case 2 -> SIMPLE;
            case 3 -> WHITESPACE;
            case 4 -> STOP;
            case 5 -> KEYWORD;
            case 6 -> PATTERN;
            case 7 -> FINGERPRINT;
            case 8 -> LANGUAGE;
            default -> throw ErrorCode.get(IndexingErrorCodes.INVALID_ANALYSIS_MODEL);
        };
    }
}