package com.tlc.entity.cache;

import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public interface CacheAdapter<K, V>
{
    void put( final @NonNull K key, final V value );

    void putAll( final @NonNull Map<K, V> pairs );

    void remove( final @NonNull K key );

    void removeAll( final @NonNull Set<K> keys );

    boolean containsKey( final @NonNull K key );

    @Nullable
    V get( final @NonNull K key );

    @NonNull
    Map<K, V> getAll( final @NonNull Set<K> keys );

    void forEach( final @NonNull BiConsumer<K, V> consumer );
}
