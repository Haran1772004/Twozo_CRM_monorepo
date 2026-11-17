package com.tlc.crm.core.action.parser.impl;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * <p>
 * Transforms data to bytes/map based on our action.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class NativeParser implements DataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexDataPublisher.class);

    private NativeParser() {
    }

    private static class Instance {
        private static final NativeParser INSTANCE = new NativeParser();
    }

    /**
     * <p>
     * Gets the {@link NativeParser} Instance.
     * </p>
     *
     * @return {@link NativeParser}
     */
    public static NativeParser getInstance() {
        return NativeParser.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts {@link Map} into bytes.
     * </p>
     *
     * @param data Holds related data in a {@link Map}.
     * @return bytes.
     */
    @Override
    public byte[] serialize(final Object data) {

        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(data);

            return byteArrayOutputStream.toByteArray();
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Indexing Record, Serialization: Action failed, Reason : {}", throwable.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * Deserializes the bytes into a {@link Map}.
     * </p>
     *
     * @param bytes Holds bytes.
     * @return {@link Map}
     */
    @Override
    public Object deserialize(final byte[] bytes) {
        try (final ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(bytes);
             final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayOutputStream)) {

            return objectInputStream.readObject();
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Indexing Record, De-Serialization: Action failed, Reason : {}", throwable.getMessage());
            return null;
        }
    }
}
