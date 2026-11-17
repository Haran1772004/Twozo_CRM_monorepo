package com.tlc.search.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Depicts the stored script details for a Search Request.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ScriptInfo {

    private String id;
    private String fieldName;
    private Map<String, Object> paramsMap;

    public String getId() {
        return id;
    }

    public ScriptInfo(final String id, final String fieldName, final Map<String, Object> paramsMap) {
        this.id = id;
        this.fieldName = fieldName;
        this.paramsMap = paramsMap;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public Map<String, Object> getParamsMap() {
        return paramsMap;
    }

    public void setParamsMap(final Map<String, Object> paramsMap) {
        this.paramsMap = paramsMap;
    }

    public void putIntoParamsMap(final String param, final String value) {
        if (Objects.isNull(this.paramsMap)) {
            this.paramsMap = new HashMap<>();
        }
        paramsMap.put(param, value);
    }
}
