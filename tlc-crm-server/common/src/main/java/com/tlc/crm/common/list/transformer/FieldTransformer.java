package com.tlc.crm.common.list.transformer;

import java.util.Map;

/**
 * <p>
 * Provides an abstract way to transforms the field.
 * </p>
 *
 * @author eswari
 * @version 1.0
 */
public interface FieldTransformer {

    /**
     * Transforms the data in the given map based on the specified field.
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  The name of the field to use for the transformation.
     * @param entity The map of data to transform.
     * @return An Object representing the transformed.
     */
    Object transform(final long orgId, final String fieldName, final Map<String, Object> entity);
}