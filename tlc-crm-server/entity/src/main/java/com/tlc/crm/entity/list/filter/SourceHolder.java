package com.tlc.crm.entity.list.filter;

import com.tlc.crm.entity.list.filter.source.FilterSourceType;

/**
 * <p>
 * Provides the holder of the required sources.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SourceHolder {

    /**
     * <p>
     * Provides the source name of field.
     * </p>
     *
     * @return source name of field.
     */
    FilterSourceType source();

    /**
     * <p>
     * Provides the related source of field.
     * </p>
     *
     * @return Related source of field.
     */
    FilterSourceType relatedSource();
}
