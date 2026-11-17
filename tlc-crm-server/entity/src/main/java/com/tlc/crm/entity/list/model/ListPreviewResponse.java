package com.tlc.crm.entity.list.model;

import java.util.Map;

/**
 * <p>
 * Holds the {@link ListPreviewResponse} that contains the preview details to be loaded into the list response
 * </p>
 *
 * @param entityRows The rows containing the id and its related partial preview data
 */
public record ListPreviewResponse(Map<String, Map<String, Object>> entityRows) {
}