package com.tlc.crm.entity.search.api.model;

import com.tlc.i18n.I18nKey;
import com.tlc.search.model.AnalysisComponent;

/**
 * <p>
 * A model that represents an index data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record Index(Long id, I18nKey displayName, String name, String alternateName, Long sourceId,
                    AnalysisComponent analysisComponent) {

    public Index(final Long id, final I18nKey displayName, final String name, final AnalysisComponent analysisComponent) {
        this(id, displayName, name, null, null, analysisComponent);
    }

    public Index(final Long id, final I18nKey displayName, final String name, final Long sourceId, AnalysisComponent analysisComponent) {
        this(id, displayName, name, null, sourceId, analysisComponent);
    }
}