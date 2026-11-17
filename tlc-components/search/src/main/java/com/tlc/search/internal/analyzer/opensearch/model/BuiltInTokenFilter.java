package com.tlc.search.internal.analyzer.opensearch.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.AnalysisModel;
import org.opensearch.client.opensearch._types.analysis.SnowballLanguage;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;

/**
 * <p>
 * Built-in {@link TokenFilter}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum BuiltInTokenFilter implements AnalysisModel {

    LOWERCASE("lowercase", 1, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder()
            .lowercase(builder -> builder).build()).build()),
    UPPERCASE("uppercase", 2, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            uppercase(builder -> builder).build()).build()),
    STOP("stop", 3, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            stop(builder -> builder.stopwords("")).build()).build()),
    ASCII_FOLDING("asciifolding", 4, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            asciifolding(builder -> builder.preserveOriginal(true)).build()).build()),
    STEMMER("stemmer", 5, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            stemmer(builder -> builder.language("")).build()).build()),
    SNOWBALL("snowball", 6, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            snowball(builder -> builder.language(SnowballLanguage.English)).build()).build()),
    KSTEM("kstem", 7, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder()
            .kstem(builder -> builder).build()).build()),
    EDGE_NGRAM("edge_ngram", 8, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            edgeNgram(builder -> builder.minGram(1).maxGram(2)).build()).build()),
    NGRAM("ngram", 9, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            ngram(builder -> builder.minGram(1).maxGram(2)).build()).build()),
    SHINGLE("shingle", 10, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            shingle(builder -> builder).build()).build()),
    TRIM("trim", 11, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            trim(builder -> builder).build()).build()),
    UNIQUE("unique", 12, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            unique(builder -> builder).build()).build()),
    WORD_DELIMITER("word_delimiter", 13, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            wordDelimiter(builder -> builder).build()).build()),
    WORD_DELIMITER_GRAPH("word_delimiter_graph", 14, new TokenFilter.Builder().definition(
            new TokenFilterDefinition.Builder().wordDelimiterGraph(builder -> builder).build()).build()),
    LENGTH("length", 15, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            length(builder -> builder.min(1).max(10)).build()).build()),
    LIMIT("limit", 16, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            limit(builder -> builder.consumeAllTokens(true).maxTokenCount(5)).build()).build()),
    SYNONYM("synonym", 17, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            synonym(builder -> builder).build()).build()),
    SYNONYM_GRAPH("synonym_graph", 18, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            synonymGraph(builder -> builder).build()).build()),
    ELISION("elision", 19, new TokenFilter.Builder().definition(new TokenFilterDefinition.Builder().
            elision(builder -> builder.articles("").articlesCase(true)).build()).build()),
    ;

    private final String name;
    private final short id;
    private final TokenFilter tokenFilter;

    BuiltInTokenFilter(final String name, final int id, final TokenFilter tokenFilter) {
        this.name = name;
        this.id = (short) id;
        this.tokenFilter = tokenFilter;
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
     * Gets the {@link TokenFilter}.
     * </p>
     *
     * @return {@link TokenFilter}.
     */
    public TokenFilter getTokenFilter() {
        return tokenFilter;
    }

    /**
     * <p>
     * Gets {@link BuiltInTokenFilter}.
     * </p>
     *
     * @param id Represents id.
     * @return {@link BuiltInTokenFilter}.
     */
    public static BuiltInTokenFilter get(short id) {
        return switch (id) {
            case 1 -> LOWERCASE;
            case 2 -> UPPERCASE;
            case 3 -> STOP;
            case 4 -> ASCII_FOLDING;
            case 5 -> STEMMER;
            case 6 -> SNOWBALL;
            case 7 -> KSTEM;
            case 8 -> EDGE_NGRAM;
            case 9 -> NGRAM;
            case 10 -> SHINGLE;
            case 11 -> TRIM;
            case 12 -> UNIQUE;
            case 13 -> WORD_DELIMITER;
            case 14 -> WORD_DELIMITER_GRAPH;
            case 15 -> LENGTH;
            case 16 -> LIMIT;
            case 17 -> SYNONYM;
            case 18 -> SYNONYM_GRAPH;
            case 19 -> ELISION;
            default -> throw ErrorCode.get(IndexingErrorCodes.INVALID_ANALYSIS_MODEL);
        };
    }
}