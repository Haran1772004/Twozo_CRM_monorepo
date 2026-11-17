package com.tlc.search.internal.analyzer.opensearch;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.search.internal.analyzer.opensearch.registrar.OpenSearchCustomAnalyzerServiceRegistrar;
import com.tlc.search.internal.analyzer.service.IndexCustomAnalyzerService;
import org.opensearch.client.opensearch._types.analysis.Analyzer;

/**
 * <p>
 * Open search's custom analyzers.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = OpenSearchCustomAnalyzerServiceRegistrar.class)
public interface OpenSearchCustomAnalyzerService extends IndexCustomAnalyzerService {

    /**
     * <p>
     * Gets custom {@link Analyzer}.
     * </p>
     *
     * @return custom {@link Analyzer}.
     */
    Analyzer getCustomAnalyzer();
}