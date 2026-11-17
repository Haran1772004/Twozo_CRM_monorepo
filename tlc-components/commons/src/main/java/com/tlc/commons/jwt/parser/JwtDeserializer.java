package com.tlc.commons.jwt.parser;

import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * A deserializer that converts the bytes to the given type
 *
 * @param <T>
 * @author Selvakumar
 * @version 1.0
 */
public class JwtDeserializer<T> implements Deserializer<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JwtDeserializer.class);
    private Class<T> returnType;

    public JwtDeserializer() {
    }

    public JwtDeserializer(Class<T> returnType) {
        Assert.notNull(returnType, "Return type cannot be null.");
        this.returnType = returnType;
    }

    /**
     * Transforms bytes into the given type of object
     *
     * @param bytes Holds object data
     * @return Converted object
     * @throws DeserializationException
     */
    public T deserialize(byte[] bytes) throws DeserializationException {
        try {
            return this.readValue(bytes);
        } catch (IOException exception) {
            String msg = "Unable to deserialize bytes into a " + this.returnType.getName() + " instance: " + exception.getMessage();
            LOGGER.error(msg);
            throw new DeserializationException(msg, exception);
        }
    }

    /**
     * Transforms the bytes into a object representation
     *
     * @param bytes Holds the data
     * @return The object representation
     * @throws IOException
     */
    private T readValue(byte[] bytes) throws IOException {
        if (Objects.isNull(returnType)) {
            return (T) JwtSerializer.DEFAULT_OBJECT_MAPPER.readValue(bytes, Object.class);
        } else {
            return JwtSerializer.DEFAULT_OBJECT_MAPPER.readValue(bytes, returnType);
        }
    }

}
