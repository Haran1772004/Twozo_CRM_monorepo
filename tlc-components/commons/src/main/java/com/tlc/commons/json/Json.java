package com.tlc.commons.json;

import com.tlc.commons.json.impl.ImmutableJsonObject;
import com.tlc.commons.json.impl.JsonArrayImpl;
import com.tlc.commons.json.impl.JsonObjectImpl;
import com.tlc.commons.json.impl.JsonParserImpl;
import com.tlc.commons.json.impl.OrderedJsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import java.io.InputStream;
import java.util.*;

/**
 * @author Abishek
 * @version 1.0
 */
public class Json
{
    private final static JsonParser JSON_PARSER = new JsonParserImpl();
    private static final ImmutableJsonObject IMMUTABLE_JSON_OBJECT = new ImmutableJsonObject( new JsonObjectImpl( JSON_PARSER, Collections.emptyMap() ),
                                                                                              JSON_PARSER );

    public static JsonArray array()
    {
        return new JsonArrayImpl(JSON_PARSER);
    }

    public static JsonArray array(Collection<?> list)
    {
        return new JsonArrayImpl(JSON_PARSER, new ArrayList<>(list));
    }

    public static JsonArray array(String data)
    {
        final List<?> decoded = JSON_PARSER.decodeJsonArray(data);
        return new JsonArrayImpl(JSON_PARSER, decoded);
    }

    public static JsonArray array(byte[] data)
    {
        final List<?> decoded = JSON_PARSER.decodeJsonArray(data);
        return new JsonArrayImpl(JSON_PARSER, decoded);
    }

    public static JsonArray array(InputStream inputStream)
    {
        final List<?> decoded = JSON_PARSER.decodeJsonArray(inputStream);
        return new JsonArrayImpl(JSON_PARSER, decoded);
    }

    public static JsonObject object()
    {
        return new JsonObjectImpl(JSON_PARSER);
    }

    public static JsonObject object(String data)
    {
        final Map<?, ?> decoded = JSON_PARSER.decodeJsonObject(data);
        return new JsonObjectImpl(JSON_PARSER, decoded);
    }

    public static JsonObject object(Map<String, ?> data)
    {
        return new JsonObjectImpl(JSON_PARSER, new HashMap<>(data));
    }

    /**
     * <p>
     * Init {@link OrderedJsonObject}.
     * </p>
     *
     * @param data Map's data.
     * @return {@link JsonObject}.
     */
    public static JsonObject orderedObject(final LinkedHashMap<String, ?> data) {
        return new OrderedJsonObject(JSON_PARSER, data);
    }

    /**
     * <p>
     * Init {@link OrderedJsonObject}.
     * </p>
     *
     * @return {@link JsonObject}.
     */
    public static JsonObject orderedObject() {
        return new OrderedJsonObject(JSON_PARSER);
    }

    public static JsonObject object(byte[] data)
    {
        final Map<?, ?> decoded = JSON_PARSER.decodeJsonObject(data);
        return new JsonObjectImpl(JSON_PARSER, decoded);
    }

    public static JsonObject object(InputStream inputStream)
    {
        final Map<?, ?> decoded = JSON_PARSER.decodeJsonObject(inputStream);
        return new JsonObjectImpl(JSON_PARSER, decoded);
    }

    /**
     * <p>
     * Checks whether the json is empty.
     * </p>
     *
     * @param jsonObject {@link JsonObject}.
     * @return whether the json size is empty.
     */
    public static boolean isEmpty(final JsonObject jsonObject) {
        return Objects.isNull(jsonObject) || NumericalConstant.ZERO == jsonObject.size();
    }

    /**
     * <p>
     * Checks whether the json is empty.
     * </p>
     *
     * @param jsonArray {@link JsonArray}.
     * @return whether the json size is empty.
     */
    public static boolean isEmpty(final JsonArray jsonArray) {
        return Objects.isNull(jsonArray) || NumericalConstant.ZERO == jsonArray.size();
    }

    /**
     * <p>
     * Creates a empty immutable JSON object.
     * </p>
     *
     * @return The {@link ImmutableJsonObject}
     */
    public static JsonObject emptyObject()
    {
        return IMMUTABLE_JSON_OBJECT;
    }
}
