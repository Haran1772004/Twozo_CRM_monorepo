package com.tlc.search.internal.analyzer.opensearch.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.model.AnalysisModel;
import org.opensearch.client.opensearch._types.analysis.TokenChar;
import org.opensearch.client.opensearch._types.analysis.Tokenizer;
import org.opensearch.client.opensearch._types.analysis.TokenizerDefinition;

/**
 * <p>
 * Built-in {@link Tokenizer}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public enum BuiltInTokenizer implements AnalysisModel {

    STANDARD("standard", 1, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            standard(builder -> builder.maxTokenLength(255)).build()).build()),
    UAX_URL_EMAIL("uax_url_email", 2, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            uaxUrlEmail(builder -> builder.maxTokenLength(255)).build()).build()),
    WHITESPACE("whitespace", 3, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            whitespace(b -> b).build()).build()),

    KEYWORD("keyword", 4, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            keyword(builder -> builder.bufferSize(256)).build()).build()),
    PATTERN("pattern", 5, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            pattern(builder -> builder.pattern("\\W+").flags("EMPTY").group(-1)).build()).build()),
    CHAR_GROUP("char_group", 6, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            charGroup(builder -> builder.tokenizeOnChars("whitespace", "punctuation")).build()).build()),
    PATH_HIERARCHY("path_hierarchy", 7, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            pathHierarchy(builder -> builder.delimiter("").bufferSize(256).replacement("").reverse(false).skip(-1)).build()).build()),
    NGRAM("ngram", 8, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            ngram(builder -> builder.minGram(1).maxGram(2).tokenChars(TokenChar.Custom)).build()).build()),
    EDGE_NGRAM("edge_ngram", 9, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            edgeNgram(builder -> builder.minGram(1).maxGram(2).tokenChars(TokenChar.Custom)).build()).build()),
    LETTER("letter", 10, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            letter(builder -> builder).build()).build()),
    LOWERCASE("lowercase", 11, new Tokenizer.Builder().definition(new TokenizerDefinition.Builder().
            lowercase(builder -> builder).build()).build());

    private final String name;
    private final short id;
    private final Tokenizer tokenizer;

    BuiltInTokenizer(final String name, final int id, final Tokenizer tokenizer) {
        this.name = name;
        this.id = (short) id;
        this.tokenizer = tokenizer;
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
     * Gets the {@link Tokenizer}.
     * </p>
     *
     * @return {@link Tokenizer}.
     */
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    /**
     * <p>
     * Gets {@link BuiltInTokenizer}.
     * </p>
     *
     * @param id Represents id.
     * @return {@link BuiltInTokenizer}.
     */
    public static BuiltInTokenizer get(short id) {
        return switch (id) {
            case 1 -> STANDARD;
            case 2 -> UAX_URL_EMAIL;
            case 3 -> WHITESPACE;
            case 4 -> KEYWORD;
            case 5 -> PATTERN;
            case 6 -> CHAR_GROUP;
            case 7 -> PATH_HIERARCHY;
            case 8 -> NGRAM;
            case 9 -> EDGE_NGRAM;
            case 10 -> LETTER;
            case 11 -> LOWERCASE;
            default -> throw ErrorCode.get(IndexingErrorCodes.INVALID_ANALYSIS_MODEL);
        };
    }
}