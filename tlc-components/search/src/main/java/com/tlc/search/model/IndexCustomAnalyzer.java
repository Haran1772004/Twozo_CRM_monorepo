package com.tlc.search.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.model.TlcEnum;
import com.tlc.search.internal.status.IndexingErrorCodes;
import org.opensearch.client.opensearch._types.analysis.Analyzer;

/**
 * <p>
 * Custom defined {@link Analyzer}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum IndexCustomAnalyzer implements TlcEnum, AnalysisModel {

    CUSTOM_KEYWORD_ANALYZER((short) 1, "customKeywordAnalyzer"),
    CUSTOM_UAX_URL_EMAIL_ANALYZER((short) 2, "customUaxUrlAnalyzer"),
    CUSTOM_WHITESPACE_ANALYZER((short) 3, "customWhitespaceAnalyzer"),
    ;

    private final short id;
    private final String name;

    IndexCustomAnalyzer(final short id, final String name) {
        this.id = id;
        this.name = name;
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
     * Gets {@link IndexCustomAnalyzer}.
     * </p>
     *
     * @param id Represents id.
     * @return {@link IndexCustomAnalyzer}.
     */
    public static IndexCustomAnalyzer get(final short id) {
        return switch (id) {
            case 1 -> CUSTOM_KEYWORD_ANALYZER;
            case 2 -> CUSTOM_UAX_URL_EMAIL_ANALYZER;
            case 3-> CUSTOM_WHITESPACE_ANALYZER;
            default -> throw ErrorCode.get(IndexingErrorCodes.INVALID_ANALYSIS_MODEL);
        };
    }
}