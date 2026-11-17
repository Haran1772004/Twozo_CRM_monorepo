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
 * Custom text analyzer where the tokenizer is uax url email & filter is of lowercase.
 * Note - String field is split on whitespace while keeping the special chars but neglects @, #.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = OpenSearchCustomAnalyzerServiceRegistrar.class)
public final class CustomUaxUrlEmailAnalyzer implements OpenSearchCustomAnalyzerService {

    private static class Instance {
        private static final CustomUaxUrlEmailAnalyzer INSTANCE = new CustomUaxUrlEmailAnalyzer();
    }

    private CustomUaxUrlEmailAnalyzer() {
    }

    /**
     * <p>
     * Gets {@link CustomUaxUrlEmailAnalyzer}.
     * </p>
     *
     * @return {@link CustomUaxUrlEmailAnalyzer}.
     */
    public static CustomUaxUrlEmailAnalyzer getInstance() {
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
        return IndexCustomAnalyzer.CUSTOM_UAX_URL_EMAIL_ANALYZER;
    }

    /**
     * {@inheritDoc}
     */
    public Analyzer getCustomAnalyzer() {
        return new Analyzer.Builder().custom(custom ->
                custom.tokenizer(BuiltInTokenizer.UAX_URL_EMAIL.getName()).filter(BuiltInTokenFilter.LOWERCASE.getName())).build();
    }
}