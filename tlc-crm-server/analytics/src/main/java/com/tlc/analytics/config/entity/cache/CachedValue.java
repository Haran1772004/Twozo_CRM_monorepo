package com.tlc.analytics.config.entity.cache;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;

/**
 * <p>
 * Represents a cached value with metadata about its creation and last access time.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor( staticName = "create", access = AccessLevel.PACKAGE )
public class CachedValue<T>
{
    @Nonnull
    private final T actualCache;
    private final long cacheCreatedTime;
    @Setter
    private long lastAccessTime = System.currentTimeMillis();
}
