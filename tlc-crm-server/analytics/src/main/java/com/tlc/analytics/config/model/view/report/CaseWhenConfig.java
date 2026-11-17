package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.keys.view.report.ReportKeys;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.filter.fields.CriteriaFields;

import com.tlc.sql.common.api.dml.Connector;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single CASE WHEN configuration.
 * Supports multiple values (always an array) and multiple WHENs.
 */
@Getter
public class CaseWhenConfig {

    private final JsonObject configJson;
    private final String column;
    private final List<Object> value;
    private final int operator;
    private final Connector connector;
    private final int operationId;

    public CaseWhenConfig(final JsonObject jsonObject) {
        this.configJson = jsonObject;
        this.column = configJson.getString(ReportKeys.COLUMN);
        this.operator = jsonObject.getInt(ReportKeys.OPERATOR);
        this.value = new ArrayList<>();
        final JsonArray valuesArray = jsonObject.getJsonArray(ReportKeys.VALUE);
        final int valuesSize = valuesArray.size();
        this.connector = jsonObject.containsKey(CriteriaFields.CONNECTOR.getName())
                ? Connector.valueOf(jsonObject.getString(CriteriaFields.CONNECTOR.getName()).toUpperCase())
                : null;
        this.operationId = configJson.getInt(ReportKeys.OPERATION);

        for (int index = 0; index < valuesSize; index++) {
            value.add(valuesArray.get(index));
        }
    }

    /**
     * Parses a top-level JSON array string and returns a list of CaseWhenConfig objects.
     *
     * @param configAsString JSON array string
     * @return List of CaseWhenConfig
     */
    public static Collection<CaseWhenConfig> parseArray(final String configAsString) {
        final JsonArray array = Json.array(configAsString);
        final Collection<CaseWhenConfig> caseWhenConfigList = new ArrayList<>();
        final int arraySize = array.size();

        for (int index = 0; index < arraySize; index++) {
            caseWhenConfigList.add(new CaseWhenConfig(array.getJsonObject(index)));
        }

        return caseWhenConfigList;
    }
}