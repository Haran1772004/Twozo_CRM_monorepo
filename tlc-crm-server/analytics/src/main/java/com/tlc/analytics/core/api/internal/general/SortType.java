package com.tlc.analytics.core.api.internal.general;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing different types of sorting.
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum SortType
{
    /**
     * No sorting.
     */
    NONE( "NONE" ),

    /**
     * Ascending sorting.
     */
    ASC( "ASC" ),

    /**
     * Descending sorting.
     */
    DESC( "DESC" );

    /**
     * The string representation of the sort type.
     */
    private final String sortType;
}
