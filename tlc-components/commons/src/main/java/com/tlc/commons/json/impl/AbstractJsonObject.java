package com.tlc.commons.json.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * @author Abishek
 * @version 1.0
 */
public abstract class AbstractJsonObject implements JsonObject
{
    private final Map<String, Object> dataMap;
    AbstractJsonObject()
    {
        this.dataMap = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    AbstractJsonObject(Map<?, ?> map)
    {
        this.dataMap = (Map<String, Object>) map;
    }

    @Override
    public boolean containsKey(String key)
    {
        return dataMap.containsKey(key);
    }

    @Override
    public JsonObject put(String key, Boolean value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Short value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Integer value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Long value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Float value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Double value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Number value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, String value)
    {
        localPut(key, value);
        return this;
    }

    @Override
    public JsonObject put(String key, Object value)
    {
        if(value instanceof AbstractJsonArray jsonArray)
        {
            localPut(key, jsonArray.getList());
        }
        else if(value instanceof AbstractJsonObject jsonObject)
        {
            localPut(key, jsonObject.getMap());
        }
        else
        {
            localPut(key, value);
        }
        return this;
    }

    @Override
    public JsonObject put(String key, JsonObject value)
    {
        localPut(key, ((AbstractJsonObject)value).getMap());
        return this;
    }

    @Override
    public JsonObject put(String key, JsonArray value)
    {
        localPut(key, ((AbstractJsonArray)value).getList());
        return this;
    }

    @Override
    public JsonObject append(JsonObject json)
    {
        dataMap.putAll(((AbstractJsonObject)json).getMap());
        return this;
    }

    @Override
    public boolean getBoolean(String key)
    {
        final Object object = localNonNullGet(key);
        return object instanceof Boolean ? ((Boolean) object)
                : Boolean.parseBoolean((String)object);
    }

    @Override
    public boolean optBoolean(String key)
    {
        return optBoolean(key, false);
    }

    @Override
    public boolean optBoolean(String key, boolean defaultValue)
    {
        final Object object = localGet(key);
        if(object != null)
        {
            try
            {
                return object instanceof Boolean ? ((Boolean) object)
                        : Boolean.parseBoolean((String)object);
            }
            catch (Exception ignored) {}
        }
        return defaultValue;
    }

    @Override
    public short getShort(String key)
    {
        final Object object = localNonNullGet(key);
        return object instanceof Number ? ((Number) object).shortValue()
                : Short.parseShort((String) object);
    }

    @Override
    public short optShort(String key, short defaultValue)
    {
        final Object object = localGet(key);
        if (object != null)
        {
            try
            {
                return object instanceof Number ? ((Number) object).shortValue()
                        : Short.parseShort((String) object);
            }
            catch (Exception ignored) {}
        }
        return defaultValue;
    }

    @Override
    public int getInt(String key)
    {
        final Object object = localNonNullGet(key);
        return object instanceof Number ? ((Number) object).intValue()
                : Integer.parseInt((String) object);
    }

    @Override
    public int optInt(String key, int defaultValue)
    {
        final Object object = localGet(key);
        if (object != null)
        {
            try
            {
                return object instanceof Number ? ((Number) object).intValue()
                        : Integer.parseInt((String) object);
            }
            catch (Exception ignored) {}
        }
        return defaultValue;

    }

    @Override
    public long getLong(String key)
    {
        final Object object = localNonNullGet(key);
        return object instanceof Number ? ((Number) object).longValue()
                : Long.parseLong((String) object);
    }

    @Override
    public long optLong(String key, long defaultValue)
    {
        final Object object = localGet(key);
        if (object != null)
        {
            try
            {
                return object instanceof Number ? ((Number) object).longValue()
                        : Long.parseLong((String) object);
            }
            catch (Exception ignored) {}
        }
        return defaultValue;

    }

    @Override
    public float getFloat(String key)
    {
        final Object object = localNonNullGet(key);
        return object instanceof Number ? ((Number) object).floatValue()
                : Float.parseFloat((String) object);
    }

    @Override
    public float optFloat(String key, float defaultValue)
    {
        final Object object = localGet(key);
        if (object != null)
        {
            try
            {
                return object instanceof Number ? ((Number) object).floatValue()
                        : Float.parseFloat((String) object);
            }
            catch (Exception ignored) {}
        }
        return defaultValue;

    }

    @Override
    public double getDouble(String key)
    {
        final Object object = localNonNullGet(key);
        return object instanceof Number ? ((Number) object).doubleValue()
                : Double.parseDouble((String) object);
    }

    @Override
    public double optDouble(String key, double defaultValue)
    {
        final Object object = localGet(key);
        if (object != null)
        {
            try
            {
                return object instanceof Number ? ((Number) object).doubleValue()
                        : Double.parseDouble((String) object);
            }
            catch (Exception ignored) {}
        }
        return defaultValue;

    }

    @Override
    public String getString(String key)
    {
        final Object object = localNonNullGet(key);
        final String value = object instanceof String ? ((String) object) : object.toString();

        return value.isBlank() ? null : value.trim();
    }

    @Override
    public String optString(String key, String defaultValue)
    {
        final Object object = localGet(key);
        if (object != null)
        {
            try
            {
                final String value = object instanceof String ? ((String) object) : object.toString();

                return value.isBlank() ? null : value.trim();
            }
            catch (Exception ignored) {}
        }
        return defaultValue;
    }

    @Override
    public JsonArray getJsonArray(String key)
    {
        final Object object = localNonNullGet(key);
        return wrappedList((List<?>) object);
    }

    @Override
    public JsonArray optJsonArray(String key)
    {
        final Object object = localGet(key);
        try
        {
            return object == null ? null : wrappedList((ArrayList<?>) object);
        }
        catch (Exception ignored){}
        return null;
    }

    @Override
    public JsonObject getJsonObject(String key)
    {
        final Object object = localNonNullGet(key);
        return wrappedMap((Map<?, ?>) object);
    }

    @Override
    public JsonObject optJsonObject(String key)
    {
        final Object object = localGet(key);
        try
        {
            return object == null ? null : wrappedMap((Map<?, ?>) object);
        }
        catch (Exception ignored){}
        return null;
    }

    @Override
    public Object get(String key)
    {
        return localNonNullGet(key);
    }

    @Override
    public Object opt(String key)
    {
        return localGet(key);
    }

    @Override
    public JsonObject remove(String key)
    {
        dataMap.remove(key);
        return this;
    }

    @Override
    public int size()
    {
        return dataMap.size();
    }

    @Override
    public void forEach(BiConsumer<String, Object> consumer)
    {
        dataMap.forEach(consumer);
    }

    @Override
    public Set<String> keySet()
    {
        return dataMap.keySet();
    }

    @Override
    public Map<String, Object> toMap()
    {
        return Map.copyOf(dataMap);
    }

    /**
     * Prunes the JSON tree by identifying and deleting all {@code JsonObject}/{@code JsonArray}
     * instances with a size of zero.
     */
    @Override
    public void removeEmptyObjects() {
        final Iterator<Map.Entry<String, Object>> dataMapIterator = dataMap.entrySet().iterator();

        while (dataMapIterator.hasNext()) {
            final Map.Entry<String, Object> dataMapEntry = dataMapIterator.next();
            final Object value = dataMapEntry.getValue();

            if (value instanceof Map<?, ?> childMap) {
                wrappedMap(childMap).removeEmptyObjects();

                if (childMap.isEmpty()) {
                    dataMapIterator.remove();
                }
            } else if (value instanceof List<?> childList) {
                wrappedList(childList).removeEmptyObjects();

                if (childList.isEmpty()) {
                    dataMapIterator.remove();
                }
            }
        }
    }

    protected Map<String, Object> getMap()
    {
        return dataMap;
    }

    private Object localNonNullGet(String key)
    {
        final Object value = dataMap.get(key);
        if(value == null)
        {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "Object not found for key - " + key);
        }
        return value;
    }

    private Object localGet(String key)
    {
        return dataMap.get(key);
    }

    private void localPut(String key, Object value)
    {
        if(value != null)
        {
            dataMap.put(key, value);
        }
        else
        {
            dataMap.remove(key);
        }
    }

    protected abstract JsonArray wrappedList(List<?> collection);

    protected abstract JsonObject wrappedMap(Map<?, ?> map);

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @param <T> Represents the type of the value associated with the given key
     * @return the value
     */
    public <T> T getNullableValue(final String key) {
        return (T) localGet(key);
    }

    /**
     * {@inheritDoc}
     *
     * @param key Refers the key used to retrieve the value.
     * @return The trimmed string if valid, otherwise {@code null}.
     */
    public String getNullableString(final String key) {
        final Object object = localGet(key);

        if (object instanceof String value) {
            return value.isBlank() ? null : value.trim();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @return true if the key has null value
     */
    public boolean isKeyPresentWithNullValue(final String key) {
        return dataMap.containsKey(key) && Objects.isNull(getNullableValue(key));
    }

    /**
     * {@inheritDoc}
     *
     * @param <T>          the type of the value associated with the given key
     * @param key          represents the key
     * @param defaultValue the default value to return if the key is not found in the data map
     * @return the value associated with the key, or the default value if the key is not present
     */
    public <T> T optNullableValue(final String key, final T defaultValue) {
        return dataMap.containsKey(key) ? getNullableValue(key) : defaultValue;
    }

    /**
     * {@inheritDoc}
     *
     * @param <T>           the type of the value associated with the given key
     * @param key           represents the key
     * @param valueFunction the function used to compute a value if the key is not present
     * @return the value associated with the key, or the newly computed and stored value
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(final String key, final Function<String, T> valueFunction) {
        if (dataMap.containsKey(key)) {
            final Object object = dataMap.get(key);

            if (object instanceof final Map<?, ?> valueMap) {
                return (T) wrappedMap(valueMap);
            } else if (object instanceof final List<?> valueList) {
                return (T) wrappedList(valueList);
            }
            return (T) object;
        }
        final T value = valueFunction.apply(key);

        put(key, value);
        return value;
    }
}
