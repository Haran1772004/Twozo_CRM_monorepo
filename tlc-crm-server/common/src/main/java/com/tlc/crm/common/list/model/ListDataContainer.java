package com.tlc.crm.common.list.model;

import com.tlc.commons.json.Json;

import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.ListRowListener;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.transformer.FieldTransformer;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * A model which represents the {@link ListDataContainer}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ListDataContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListDataContainer.class);

    private final long orgId;
    private final Collection<Long> userIds;
    private final Collection<Map<String, Object>> data;
    private final I18nResolutionAdapter i18NResolutionAdapter;

    public ListDataContainer(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Collection<Map<String, Object>> responseData) {
        this(orgId, i18NResolutionAdapter, responseData, new HashSet<>());
    }

    public ListDataContainer(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Collection<Map<String, Object>> data,
                             final Collection<Long> userIds) {
        this.orgId = orgId;
        this.i18NResolutionAdapter = i18NResolutionAdapter;
        this.data = data;
        this.userIds = userIds;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public long getOrgId() {
        return orgId;
    }

    public Collection<Map<String, Object>> getData() {
        return data;
    }

    public I18nResolutionAdapter getI18NResolutionAdapter() {
        return i18NResolutionAdapter;
    }

    /**
     * <p>
     * Converts the rows into data.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter} details
     * @param rowListener     Represents the {@link ListRowListener}
     */
    public Collection<Map<String, Object>> getData(final ListInterpreter listInterpreter, final ListRowListener rowListener) {
        return groupRowsByPk(listInterpreter, rowListener).values();
    }

    /**
     * <p>
     * Converts the rows into data.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter} details
     * @param rowListener     Represents the {@link ListRowListener}
     */
    public Map<String, Map<String, Object>> groupRowsByPk(final ListInterpreter listInterpreter, final ListRowListener rowListener) {
        final long transformAndGroupTime = System.currentTimeMillis();
        final Map<String, Map<String, Object>> data = new LinkedHashMap<>();
        final ListStructure structure = listInterpreter.structure();

        for (final Map<String, Object> row : this.data) {
            final Long pk = (Long) row.get(listInterpreter.primaryField());
            final Map<String, Object> convertedRow = new HashMap<>();

            row.forEach((key, value) -> {
                final FieldTransformer transformer = structure.getTransformer(key);

                if (transformer != null) {
                    convertedRow.put(key, resolveValue(transformer.transform(orgId, key, row)));
                } else if (!key.contains("_")) {
                    convertedRow.put(key, resolveValue(value));
                }
            });

            if (rowListener != null) {
                rowListener.call(pk, Json.object(convertedRow));
            }
            data.put(pk.toString(), convertedRow);
        }

        LOGGER.debug("List Action: Field transformer process and grouping by PK values completed in {}ms, Org ID: {} ", System.currentTimeMillis() - transformAndGroupTime, orgId);
        return data;
    }

    /**
     * <p>
     * Resolves the given value based on its type.
     * </p>
     *
     * @param value The value to be resolved.
     * @return The resolved value, or the original value if no resolution is applicable.
     */
    public Object resolveValue(final Object value) {
        final boolean hasI18nResolver = Optional.ofNullable(i18NResolutionAdapter).isPresent();

        if (value instanceof final I18nKey i18nKey) {
            return hasI18nResolver ? i18NResolutionAdapter.get(i18nKey) : i18nKey.getKey();
        } else if (value instanceof final String stringValue) {
            return hasI18nResolver ? i18NResolutionAdapter.get(stringValue) : stringValue;
        } else if (value instanceof Map) {
            final Map<String, Object> valueMap = new HashMap<>((Map<String, Object>) value);

            valueMap.keySet().forEach(key -> valueMap.put(key, resolveValue(valueMap.get(key))));

            return valueMap;
        } else if (value instanceof final Collection<?> values) {
            final Collection<Object> result = new ArrayList<>();

            values.forEach(data -> result.add(resolveValue(data)));

            return result;
        }

        return value;
    }
}
