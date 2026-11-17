package com.tlc.search.internal.analyzer.opensearch.registrar;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.search.model.IndexCustomAnalyzer;
import com.tlc.search.internal.analyzer.opensearch.OpenSearchCustomAnalyzerService;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Holds {@link OpenSearchCustomAnalyzerService} instances.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class OpenSearchCustomAnalyzerServiceRegistrar implements
        AutoServiceRegistry<IndexCustomAnalyzer, OpenSearchCustomAnalyzerService> {

    private final Map<IndexCustomAnalyzer, OpenSearchCustomAnalyzerService> serviceMap;

    private static class Instance {
        private static final OpenSearchCustomAnalyzerServiceRegistrar INSTANCE = new OpenSearchCustomAnalyzerServiceRegistrar();
    }

    private OpenSearchCustomAnalyzerServiceRegistrar() {
        serviceMap = new EnumMap<>(IndexCustomAnalyzer.class);
    }

    /**
     * <p>
     * Gets {@link OpenSearchCustomAnalyzerServiceRegistrar}.
     * </p>
     *
     * @return {@link OpenSearchCustomAnalyzerServiceRegistrar}.
     */
    public static OpenSearchCustomAnalyzerServiceRegistrar getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Puts instance.
     * </p>
     *
     * @param customAnalyzer  {@link IndexCustomAnalyzer}.
     * @param analyzerService {@link OpenSearchCustomAnalyzerService}.
     */
    @Override
    public void put(final IndexCustomAnalyzer customAnalyzer, final OpenSearchCustomAnalyzerService analyzerService) {
        serviceMap.put(customAnalyzer, analyzerService);
    }

    /**
     * <p>
     * Opts {@link Optional<OpenSearchCustomAnalyzerService>}.
     * </p>
     *
     * @param customAnalyzer {@link IndexCustomAnalyzer}.
     * @return {@link Optional<OpenSearchCustomAnalyzerService>}.
     */
    @Override
    public Optional<OpenSearchCustomAnalyzerService> opt(final IndexCustomAnalyzer customAnalyzer) {
        return Optional.ofNullable(serviceMap.get(customAnalyzer));
    }
}