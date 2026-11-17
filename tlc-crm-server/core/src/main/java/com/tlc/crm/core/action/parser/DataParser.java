package com.tlc.crm.core.action.parser;

/**
 * <p>
 * Parses data to bytes/map based on our action.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface DataParser {

    /**
     * <p>
     * Converts data into bytes.
     * </p>
     *
     * @param data Holds related data.
     * @return bytes.
     */
    byte[] serialize(final Object data);

    /**
     * <p>
     * Deserializes the bytes into Object.
     * </p>
     *
     * @param bytes Holds bytes.
     * @return deserialized object.
     */
    Object deserialize(final byte[] bytes);
}
