package com.tlc.commons.service;

/**
 * Codec for encoding and decoding operations
 *
 * @author sundar
 * @version 1.0
 */
public interface Codec<T> {

    /**
     * Encodes the provided {@link T} into bytes
     * <p>
     * <strong>Caution:</strong> When encoding data for publication via the messaging queue, it is advisable to keep the data size minimal to efficiently handle large amounts of data transmission.
     * </p>
     *
     * @param value the {@link T} to be encoded
     * @return the resulting encoded bytes
     */
    byte[] encode(final T value);

    /**
     * Decodes the given bytes into a {@link T}
     *
     * @param data the bytes to decode
     * @return the {@link T}
     */
    T decode(final byte[] data);
}