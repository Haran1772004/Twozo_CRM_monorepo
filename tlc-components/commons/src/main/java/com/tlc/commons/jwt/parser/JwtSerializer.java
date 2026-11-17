package com.tlc.commons.jwt.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A serializer that converts the object representation data into bytes array
 *
 * @param <T> Type of the object
 * @author Selvakumar
 * @version 1.0
 */
public class JwtSerializer<T> implements Serializer<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(JwtDeserializer.class);
    static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    public JwtSerializer() {
    }

    /**
     * Transforms the given object into byte array representation
     *
     * @param t Object to be serialized
     * @return byte array representation
     * @throws SerializationException
     */
    public byte[] serialize(T t) throws SerializationException {
        Assert.notNull(t, "Object to serialize cannot be null.");

        try {
            return JwtSerializer.DEFAULT_OBJECT_MAPPER.writeValueAsBytes(t);
        } catch (JsonProcessingException var4) {
            String msg = "Unable to serialize object: " + var4.getMessage();
            LOGGER.error(msg);
            throw new SerializationException(msg, var4);
        }
    }

}

