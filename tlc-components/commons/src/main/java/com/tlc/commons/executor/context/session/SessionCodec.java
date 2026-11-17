package com.tlc.commons.executor.context.session;

import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextCodec;
import com.tlc.commons.json.Json;

/**
 * Codec to encode and decode the session values
 *
 * @author sundar
 * @version 1.0
 */
public class SessionCodec implements ContextCodec {
    /**
     * {@inheritDoc}
     *
     * @param value the {@link SessionContext} to be encoded
     * @return the resulting encoded bytes
     */
    @Override
    public byte[] encode(final Context value) {
        final SessionContext sessionContext = (SessionContext) value;

        return sessionContext.getData().getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return the {@link SessionContext}
     */
    @Override
    public Context decode(final byte[] data) {
        return new SessionContext(Json.object(data));
    }
}