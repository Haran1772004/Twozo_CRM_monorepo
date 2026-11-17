package com.tlc.crm.common.list.transformer.model;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A model which represents the {@link TransformerResponse}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class TransformerResponse {

    private Collection<Map<String, Object>> results;
    private Collection<Long> userIds;

    public Collection<Map<String, Object>> getResults() {
        return results;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public void setResults(final Collection<Map<String, Object>> results) {
        this.results = results;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }
}
