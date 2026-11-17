package com.tlc.search.internal.analyzer.opensearch.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenFilter;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenizer;
import com.tlc.search.model.IndexCustomAnalyzer;
import com.tlc.search.internal.analyzer.opensearch.OpenSearchCustomAnalyzerService;
import com.tlc.search.internal.analyzer.opensearch.registrar.OpenSearchCustomAnalyzerServiceRegistrar;
import org.opensearch.client.opensearch._types.analysis.Analyzer;

/**
 * <p>
 * Custom text analyzer where the tokenizer is keyword & filter is of lowercase.
 * Note - Entire string field is treated as a single token and converted into a lower case text.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = OpenSearchCustomAnalyzerServiceRegistrar.class)
public final class CustomKeywordAnalyzer implements OpenSearchCustomAnalyzerService {

    private static class Instance {
        private static final CustomKeywordAnalyzer INSTANCE = new CustomKeywordAnalyzer();
    }

    private CustomKeywordAnalyzer() {
    }

    /**
     * <p>
     * Gets {@link CustomKeywordAnalyzer}.
     * </p>
     *
     * @return {@link CustomKeywordAnalyzer}.
     */
    public static CustomKeywordAnalyzer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the custom text analyzer name.
     * </p>
     *
     * @return custom text analyzer name.
     */
    @Override
    public IndexCustomAnalyzer getType() {
        return IndexCustomAnalyzer.CUSTOM_KEYWORD_ANALYZER;
    }

    /**
     * {@inheritDoc}
     */
    public Analyzer getCustomAnalyzer() {
        return new Analyzer.Builder().custom(custom ->
                custom.tokenizer(BuiltInTokenizer.KEYWORD.getName()).filter(BuiltInTokenFilter.LOWERCASE.getName())).build();    }
}