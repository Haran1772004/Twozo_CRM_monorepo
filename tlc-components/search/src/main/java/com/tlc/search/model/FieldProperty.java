package com.tlc.search.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * A model that represents the property of field mapping. It holds the details of the field properties
 * like field name, data type, make it searchable and nested properties of the fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FieldProperty {

    public enum Type {
        OBJECT, TEXT, BOOLEAN, INTEGER, LONG, DOUBLE, FLOAT, ARRAY, WILDCARD, DATE, DATE_RANGE, KEYWORD, ALIAS,
        CONSTANT, NESTED_PROPERTY
    }

    private String fieldName;
    private String aliasName;
    private Type type;
    private Collection<FieldProperty> properties;
    private String analyzerName;


    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public Collection<FieldProperty> getProperties() {
        return properties;
    }

    public void addProperty(final FieldProperty property) {
        if (Objects.isNull(properties)) {
            properties = new HashSet<>();
        }
        properties.add(property);
    }

    public void setProperties(final Collection<FieldProperty> properties) {
        this.properties = properties;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public void setAnalyzerName(final String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public String getAnalyzerName() {
        return analyzerName;
    }
}
