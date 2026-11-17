package com.tlc.analytics.config.action.validator.reports;

import com.tlc.analytics.config.action.validator.AnalyticsConfigValidator;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.AnalyticsDataType;
import com.tlc.analytics.config.model.view.report.FieldCategory;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.service.column.ColumnDetailsService;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides an abstract way to validate the report field configuration
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 * @see AnalyticsConfigValidator
 */
public class ReportFieldsValidator implements AnalyticsConfigValidator<ReportField> {
    private final ColumnDetailsService columnDetailsService;

    private ReportFieldsValidator() {
        this.columnDetailsService = ColumnDetailsService.getInstance();
    }

    private static class Instance {
        private static final ReportFieldsValidator INSTANCE = new ReportFieldsValidator();
    }

    public static ReportFieldsValidator INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Checks if a field type is valid for a given data type.
     * </p>
     *
     * @param analyticsDataType  the data type to check
     * @param fieldCategory the field type to check
     * @return true if the field type is valid, false otherwise
     */
    private boolean isValidFieldType(final AnalyticsDataType analyticsDataType, final FieldCategory fieldCategory) {
        switch (fieldCategory) {
            case CATEGORICAL -> {
                return Optional.ofNullable(analyticsDataType)
                        .filter(dt -> !Objects.equals(AnalyticsDataType.NUMERICAL, dt))
                        .isPresent();
            }
            case AGGREGATE -> {
                return Optional.ofNullable(analyticsDataType)
                        .filter(dt -> Objects.equals(AnalyticsDataType.NUMERICAL, dt))
                        .isPresent();
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param reportFields Represents the {@link ReportField} that containing the report fields data
     * @return {@code true} if the filter fields are valid; otherwise, {@code false}
     */
    @Override
    public boolean isValid(final Long orgId, final Collection<ReportField> reportFields) {

        for (ReportField reportField : reportFields) {
            final ColumnDetail columnInfo = columnDetailsService.get(reportField.getColumnId());

            assert columnInfo != null;

            return OperationValidator.INSTANCE().isValidViewOperation(columnInfo.getDataType(), reportField.getOperationType())
                    && isValidFieldType(columnInfo.getDataType(), reportField.getFieldType());
        }
        return true;
    }
}
