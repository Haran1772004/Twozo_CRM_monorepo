package com.tlc.search.internal.analyzer.opensearch.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.AnalysisModel;
import org.opensearch.client.opensearch._types.analysis.CharFilter;
import org.opensearch.client.opensearch._types.analysis.CharFilterDefinition;

/**
 * <p>
 * Built-in {@link CharFilter}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum BuiltInCharFilter implements AnalysisModel {
    HTML_STRIP("html_strip", 1, new CharFilter.Builder().definition(new CharFilterDefinition.Builder().
            htmlStrip(builder -> builder).build()).build()),
    MAPPING("mapping", 2, new CharFilter.Builder().definition(new CharFilterDefinition.Builder().
            mapping(builder -> builder.mappings("")).build()).build()),
    PATTERN_REPLACE("pattern_replace", 3, new CharFilter.Builder().definition(new CharFilterDefinition.Builder().
            patternReplace(builder -> builder.pattern("")).build()).build()),
    ICU_NORMALIZER("icu_normalizer", 4, new CharFilter.Builder().definition(new CharFilterDefinition.Builder().
            icuNormalizer(builder -> builder).build()).build()),
    ;

    private final String name;
    private final short id;
    private final CharFilter charFilter;

    BuiltInCharFilter(final String name, final int id, final CharFilter charFilter) {
        this.name = name;
        this.id = (short) id;
        this.charFilter = charFilter;
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
     * Gets the {@link CharFilter}.
     * </p>
     *
     * @return {@link CharFilter}.
     */
    public CharFilter getCharFilter() {
        return charFilter;
    }

    /**
     * <p>
     * Gets {@link BuiltInCharFilter}.
     * </p>
     *
     * @param id Represents id.
     * @return {@link BuiltInCharFilter}.
     */
    public static BuiltInCharFilter get(short id) {
        return switch (id) {
            case 1 -> HTML_STRIP;
            case 2 -> MAPPING;
            case 3 -> PATTERN_REPLACE;
            case 4 -> ICU_NORMALIZER;
            default -> throw ErrorCode.get(IndexingErrorCodes.INVALID_ANALYSIS_MODEL);
        };
    }
}