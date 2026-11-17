package com.tlc.web.client.event;

import com.tlc.web.client.internal.event.CodecRegistrarImpl;

/**
 * A registrar that registers the codec for serialization and deserialization purposes.
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface CodecRegistrar {

    /**
     * Fetches the codec registrar
     *
     * @return CodecRegistrar
     */
    static CodecRegistrar get() {
        return CodecRegistrarImpl.getInstance();
    }

    /**
     * <p>
     *  Registers the codec for the given class.
     *  <hr/>
     *  Only the implementation class has to be provided as parameter,
     *  Interface should not be passed as the parameter.
     * </p>
     * <p>
     *  <b style="color:red">No need to register the codec for byte array and String types but
     *      the registration is mandatory for all other data types </b>
     * </p>
     *
     * @param clazz Class to be registered
     * @param <T>   Type of the class to be registered
     */
    <T> void register(final Class<T> clazz);

    /**
     * Registered codec will be removed for the given class
     *
     * @param clazz Class to be unregistered
     * @param <T>   Type of the class to be unregistered
     */
    <T> void unregister(final Class<T> clazz);

    /**
     * Re-binds all previously registered codecs onto the current Event Bus.
     * Required after an in-JVM Vert.x reboot because codecs do not survive {@code vertx.close()}.
     */
    default void rebindAll() {
    }

}
