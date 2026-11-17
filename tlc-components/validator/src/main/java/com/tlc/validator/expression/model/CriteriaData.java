package com.tlc.validator.expression.model;

/**
 * A model which refers the {@link CriteriaData}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CriteriaData {

    private final String fieldKey;
    private final String compareFieldKey;
    private final Object value;
    private final ExpressionDataType expressionDataType;

    private CriteriaData(final Builder builder) {
        this.fieldKey = builder.fieldKey;
        this.compareFieldKey = builder.compareFieldKey;
        this.value = builder.value;
        this.expressionDataType = builder.expressionDataType;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getCompareFieldKey() {
        return compareFieldKey;
    }

    public Object getValue() {
        return value;
    }

    public ExpressionDataType getExpressionDataType() {
        return expressionDataType;
    }

    /**
     * A builder that builds the {@link CriteriaData}.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    public static class Builder {

        private final String fieldKey;
        private final Object value;
        private final ExpressionDataType expressionDataType;

        private String compareFieldKey;

        public Builder(final String fieldKey, final Object value, final ExpressionDataType expressionDataType) {
            this.fieldKey = fieldKey;
            this.value = value;
            this.expressionDataType = expressionDataType;
        }

        public Builder compareFieldKey(final String compareFieldKey) {
            this.compareFieldKey = compareFieldKey;
            return this;
        }

        public CriteriaData build() {
            return new CriteriaData(this);
        }
    }
}