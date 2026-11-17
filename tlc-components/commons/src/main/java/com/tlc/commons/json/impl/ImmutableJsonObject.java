package com.tlc.commons.json.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.JsonParser;

import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * <p>
 * ImmutableJson is an implementation of the {@link JsonObject} that provides an immutable JSON object
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class ImmutableJsonObject extends AbstractJsonObject
{
    private final JsonObject jsonObject;
    private final JsonParser jsonParser;

    public ImmutableJsonObject(final JsonObject jsonObject, final JsonParser jsonParser)
    {
        this.jsonObject = jsonObject;
        this.jsonParser = jsonParser;
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Long value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Float value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Short value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Double value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Number value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Object value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final String value )
    {
        return throwError();
    }

    /**
     * <p>
     * Checks if the {@link JsonObject} contains the specified key.
     * </p>
     *
     * @param key The key to be checked in the JSON object.
     * @return If {@link JsonObject} contains the given key it will return true, otherwise it will return false
     */
    @Override
    public boolean containsKey( final String key ) {
        return jsonObject.containsKey( key );
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Boolean value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final Integer value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final JsonArray value )
    {
        return throwError();
    }

    /**
     * <p>
     * Adds the value to the json associated with key
     * </p>
     *
     * @param key   The key to be used for storing the value in the JSON
     * @param value The value to be stored under the specified key in the JSON object
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject put( final String key, final JsonObject value )
    {
        return throwError();
    }

    /**
     * <p>
     * Appends the {@link JsonObject} into given existing {@link JsonObject}
     * </p>
     *
     * @param json Represents the {@link JsonObject}
     * @return Throws an unsupported operation exception
     */
    @Override
    public JsonObject append( final JsonObject json )
    {
        return throwError();
    }

    /**
     * <p>
     * Retrieves the boolean value associated with the specified key
     * </p>
     *
     * @param key Represents the json key
     * @return If {@link JsonObject} contains the given key it will return true, otherwise it will return false
     */
    @Override
    public boolean getBoolean( final String key )
    {
        return jsonObject.getBoolean( key );
    }

    /**
     * <p>
     * Retrieves the boolean value associated with the specified key
     * </p>
     *
     * @param key Represents the json key
     * @return If {@link JsonObject} contains the given key it will return true, otherwise it will return false
     */
    @Override
    public boolean optBoolean( final String key )
    {
        return jsonObject.optBoolean( key );
    }

    /**
     * <p>
     * Retrieves the boolean value associated with the specified key
     * </p>
     *
     * @param key Represents the json key
     * @return If {@link JsonObject} contains the given key it will return true, otherwise it will return false
     */
    @Override
    public boolean optBoolean( final String key, final boolean defaultValue )
    {
        return jsonObject.optBoolean( key, defaultValue );
    }

    /**
     * <p>
     * Retrieves the short value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The short value associated with the specified key
     */
    @Override
    public short getShort( final String key )
    {
        return jsonObject.getShort( key );
    }

    /**
     * <p>
     * Retrieves the short value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The short value associated with the specified key
     */
    @Override
    public short optShort( final String key, final short defaultValue )
    {
        return jsonObject.getShort( key );
    }

    /**
     * <p>
     * Retrieves the int value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The int value associated with the specified key
     */
    @Override
    public int getInt( final String key )
    {
        return jsonObject.getInt( key );
    }

    /**
     * <p>
     * Retrieves the int value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The int value associated with the specified key
     */
    @Override
    public int optInt( final String key, final int defaultValue )
    {
        return jsonObject.optInt( key, defaultValue );
    }

    /**
     * <p>
     * Retrieves the long value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The long value associated with the specified key
     */
    @Override
    public long getLong( final String key )
    {
        return jsonObject.getLong( key );
    }

    /**
     * <p>
     * Retrieves the long value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The long value associated with the specified key
     */
    @Override
    public long optLong( final String key, final long defaultValue )
    {
        return jsonObject.optLong( key, defaultValue );
    }

    /**
     * <p>
     * Retrieves the float value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The float value associated with the specified key
     */
    @Override
    public float getFloat( final String key )
    {
        return jsonObject.getFloat( key );
    }

    /**
     * <p>
     * Retrieves the float value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The float value associated with the specified key
     */
    @Override
    public float optFloat( final String key, final float defaultValue )
    {
        return jsonObject.optFloat( key, defaultValue );
    }

    /**
     * <p>
     * Retrieves the double value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The double value associated with the specified key
     */
    @Override
    public double getDouble( final String key )
    {
        return jsonObject.getDouble( key );
    }

    /**
     * <p>
     * Retrieves the double value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The double value associated with the specified key
     */
    @Override
    public double optDouble( final String key, final double defaultValue )
    {
        return jsonObject.optDouble( key, defaultValue );
    }

    /**
     * <p>
     * Retrieves the string value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The string value associated with the specified key
     */
    @Override
    public String getString( final String key )
    {
        return jsonObject.getString( key );
    }

    /**
     * <p>
     * Retrieves the string value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The string value associated with the specified key
     */
    @Override
    public String optString( final String key, final String defaultValue )
    {
        return jsonObject.optString( key, defaultValue );
    }

    /**
     * <p>
     * Retrieves the value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The value associated with the specified key
     */
    @Override
    public Object get( final String key )
    {
        return jsonObject.get( key );
    }

    /**
     * <p>
     * Retrieves the value associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The value associated with the specified key
     */
    @Override
    public Object opt( final String key )
    {
        return jsonObject.opt( key );
    }

    /**
     * <p>
     * Retrieves the {@link JsonArray} associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The {@link JsonArray} associated with the specified key
     */
    @Override
    public JsonArray getJsonArray( final String key )
    {
        return jsonObject.getJsonArray( key );
    }

    /**
     * <p>
     * Retrieves the {@link JsonArray} associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The {@link JsonArray} associated with the specified key
     */
    @Override
    public JsonArray optJsonArray( final String key )
    {
        return jsonObject.optJsonArray( key );
    }

    /**
     * <p>
     * Retrieves the {@link JsonObject} associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The {@link JsonObject} associated with the specified key
     */
    @Override
    public JsonObject getJsonObject( final String key )
    {
        return jsonObject.getJsonObject( key );
    }

    /**
     * <p>
     * Retrieves the {@link JsonObject} associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The {@link JsonObject} associated with the specified key
     */
    @Override
    public JsonObject optJsonObject( final String key )
    {
        return jsonObject.getJsonObject( key );
    }
    /**
     * <p>
     * Retrieves the {@link JsonObject} associated with the specified key in the {@link JsonObject}
     * </p>
     *
     * @param key Represents the json key
     * @return The {@link JsonObject} associated with the specified key
     */
    @Override
    public JsonObject remove( final String key )
    {
        return jsonObject.remove( key );
    }

    /**
     * <p>
     * Returns the number of key-value pairs in the JSON object.
     * </p>
     *
     * @return The number of key-value pairs in the JSON object
     */
    @Override
    public int size()
    {
        return jsonObject.size();
    }

    /**
     * <p>
     * Iterates over each key-value pair in the JSON object
     * </p>
     *
     * @param consumer The BiConsumer to be applied to each key-value pair in the JSON object
     */
    @Override
    public void forEach( final BiConsumer<String, Object> consumer )
    {
        jsonObject.forEach( consumer );
    }

    /**
     * <p>
     * Returns the byte representation of the JSON object
     * </p>
     *
     * @return An empty byte array
     */
    @Override
    public byte[] getBytes()
    {
        return jsonObject.getBytes();
    }

    /**
     * <p>
     * Writes the byte representation of the JSON object to the provided output stream
     * </p>
     *
     * @param outputStream The outputStream to which the byte representation of the JSON object
     */
    @Override
    public void writeBytes( final OutputStream outputStream )
    {
        jsonObject.writeBytes( outputStream );
    }

    /**
     * <p>
     * Retrieves the set of keys contained in the JSON object.
     * </p>
     *
     * @return A Set of Strings representing the keys in the JSON object.
     */
    @Override
    public Set<String> keySet()
    {
        return jsonObject.keySet();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} to a {@link Map}
     * </p>
     *
     * @return The {@link Map} containing the key-value pairs of the JSON object.
     */
    @Override
    public Map<String, Object> toMap()
    {
        return jsonObject.toMap();
    }

    /**
     * <p>
     * Wraps the given collection into a {@link JsonArray}
     * </p>
     *
     * @param collection Contains the list of element
     * @return The {@link JsonArray}
     */
    @Override
    protected JsonArray wrappedList( final List<?> collection )
    {
        return new JsonArrayImpl( jsonParser, collection );
    }

    /**
     * <p>
     * Wraps the provided map into a {@link JsonObject}
     * </p>
     *
     * @param map The map to be wrapped as a {@link JsonObject}
     * @return The {@link JsonObject}
     */
    @Override
    protected JsonObject wrappedMap( final Map<?, ?> map )
    {
        return new JsonObjectImpl( jsonParser, map );
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @return the value
     */
    @Override
    public <T> T getNullableValue( final String key )
    {
        return jsonObject.getNullableValue( key );
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @return true if the key has null value
     */
    @Override
    public boolean isKeyPresentWithNullValue( final String key )
    {
        return jsonObject.isKeyPresentWithNullValue( key );
    }

    /**
     * {@inheritDoc}
     *
     * @param key          represents the key
     * @param defaultValue the default value to return if the key is not found in the data map
     * @return the value associated with the key, or the default value if the key is not present
     */
    @Override
    public <T> T optNullableValue( final String key, final T defaultValue )
    {
        return jsonObject.optNullableValue( key, defaultValue );
    }

    /**
     * <p>
     * Throws an unsupported operation exception
     * </p>
     */
    private JsonObject throwError()
    {
        throw ErrorCode.get( ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION );
    }
}
