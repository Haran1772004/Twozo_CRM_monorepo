package com.tlc.search.internal.analyzer.opensearch.impl;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.search.internal.analyzer.opensearch.OpenSearchCustomAnalyzerService;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenFilter;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenizer;
import com.tlc.search.internal.analyzer.opensearch.registrar.OpenSearchCustomAnalyzerServiceRegistrar;
import com.tlc.search.model.IndexCustomAnalyzer;
import org.opensearch.client.opensearch._types.analysis.Analyzer;

/**
 * <p>
 * Custom text analyzer where the tokenizer is whitespace & filter is of lowercase.
 * Note - String field is split on whitespace while keeping all special chars intact.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = OpenSearchCustomAnalyzerServiceRegistrar.class)
public class CustomWhiteSpaceAnalyzer implements OpenSearchCustomAnalyzerService {

    private static class Instance {
        private static final CustomWhiteSpaceAnalyzer INSTANCE = new CustomWhiteSpaceAnalyzer();
    }

    private CustomWhiteSpaceAnalyzer() {
    }

    /**
     * <p>
     * Gets {@link CustomWhiteSpaceAnalyzer}.
     * </p>
     *
     * @return {@link CustomWhiteSpaceAnalyzer}.
     */
    public static CustomWhiteSpaceAnalyzer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets {@link IndexCustomAnalyzer}.
     * </p>
     *
     * @return {@link IndexCustomAnalyzer}.
     */
    @Override
    public IndexCustomAnalyzer getType()  {
        return IndexCustomAnalyzer.CUSTOM_WHITESPACE_ANALYZER;
    }

    /**
     * {@inheritDoc}
     */
    public Analyzer getCustomAnalyzer() {
        return new Analyzer.Builder().custom(custom ->
                custom.tokenizer(BuiltInTokenizer.WHITESPACE.getName()).filter(BuiltInTokenFilter.LOWERCASE.getName())).build();
    }
}
