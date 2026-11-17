package com.tlc.search.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.search.fields.SearchKeys;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInAnalyzer;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenizer;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInTokenFilter;
import com.tlc.search.internal.analyzer.opensearch.model.BuiltInCharFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Analysis components that's based on Json data.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class AnalysisComponent {

    private final JsonObject configAsJson;

    public AnalysisComponent(final String config) {
        this.configAsJson = Objects.isNull(config) || config.isBlank() ? Json.object() : Json.object(config);
    }

    /**
     * <p>
     * Gets the default analyzer name.
     * </p>
     *
     * @return the default analyzer name.
     */
    public String getDefaultAnalyzerName() {
        return configAsJson.containsKey(SearchKeys.DEFAULT_ANALYZER) ?
                IndexCustomAnalyzer.get(configAsJson.getShort(SearchKeys.DEFAULT_ANALYZER)).getName() : BuiltInAnalyzer.STANDARD.getName();
    }

    /**
     * <p>
     * Gets the collection {@link IndexCustomAnalyzer}.
     * </p>
     *
     * @return collection {@link IndexCustomAnalyzer}.
     */
    public Collection<IndexCustomAnalyzer> getCustomAnalyzers() {
        return getAnalysisIds(SearchKeys.CUSTOM_ANALYZERS).stream().map(IndexCustomAnalyzer::get).toList();
    }

    /**
     * <p>
     * Gets the collection {@link BuiltInAnalyzer}.
     * </p>
     *
     * @return collection {@link BuiltInAnalyzer}.
     */
    public Collection<BuiltInAnalyzer> getAnalyzers() {
        return getAnalysisIds(SearchKeys.ANALYZERS).stream().map(BuiltInAnalyzer::get).toList();
    }

    /**
     * <p>
     * Gets the collection {@link BuiltInTokenizer}.
     * </p>
     *
     * @return collection {@link BuiltInTokenizer}.
     */
    public Collection<BuiltInTokenizer> getTokenizers() {
        return getAnalysisIds(SearchKeys.TOKENIZERS).stream().map(BuiltInTokenizer::get).toList();
    }

    /**
     * <p>
     * Gets the collection {@link BuiltInTokenFilter}.
     * </p>
     *
     * @return collection {@link BuiltInTokenFilter}.
     */
    public Collection<BuiltInTokenFilter> getTokenFilters() {
        return getAnalysisIds(SearchKeys.TOKENIZER_FILTERS).stream().map(BuiltInTokenFilter::get).toList();
    }

    /**
     * <p>
     * Gets the collection {@link BuiltInCharFilter}.
     * </p>
     *
     * @return collection {@link BuiltInCharFilter}.
     */
    public Collection<BuiltInCharFilter> getCharFilters() {
        return getAnalysisIds(SearchKeys.CHAR_FILTERS).stream().map(BuiltInCharFilter::get).toList();
    }

    /**
     * <p>
     * Gets the analysis ids.
     * </p>
     *
     * @return the analysis ids.
     */
    private Collection<Short> getAnalysisIds(final String key) {

        if (!configAsJson.containsKey(key)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(configAsJson.getJsonArray(key).toList()).stream().map(value -> ((Integer) value).shortValue()).toList();
    }
}
