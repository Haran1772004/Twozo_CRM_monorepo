package com.tlc.sql.common.resolver;

/**
 * A resolver is responsible for resolving and managing primary key patterns in data schema or configurations.
 *
 * @author Sathish
 * @version 1.0
 */
public interface PKPatternResolver {

    Long resolve(final String pattern);
}