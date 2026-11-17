package com.tlc.web.client.internal.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.commons.code.ErrorCode;
import com.tlc.web.client.internal.status.WebClientErrorCodes;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.io.IOException;

/**
 * A codec that is used to serialize and deserialize the data passed to the event bus
 *
 * @param <T>
 * @author Selvakumar
 * @version 1.0
 */
public class ObjectCodec<T> implements MessageCodec<T, T> {

    private final Class<T> clazz;
    private final ObjectMapper objectMapper;

    ObjectCodec(final Class<T> clazz) {
        super();
        this.clazz = clazz;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void encodeToWire(final Buffer buffer, final T data) {
        final byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(data);
            buffer.appendBytes(bytes);
        } catch (JsonProcessingException e) {
            throw ErrorCode.get(WebClientErrorCodes.OBJECT_SERIALIZATION_FAILED);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T decodeFromWire(final int pos, final Buffer buffer) {
        try {
            final byte[] bytes = buffer.getBytes();
            final JavaType type = objectMapper.getTypeFactory().constructParametricType(ObjectCodec.class, clazz);

            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw ErrorCode.get(WebClientErrorCodes.OBJECT_DESERIALIZATION_FAILED);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T transform(final T customMessage) {
        return customMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return clazz.getName() + "-Codec";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte systemCodecID() {
        return -1;
    }

}

