package com.tlc.analytics.config.action.validator.reports;

import com.tlc.analytics.config.action.validator.AnalyticsValidator;
import com.tlc.analytics.config.action.validator.AnalyticsConfigValidator;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.FieldCategory;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.chart.GraphType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Provides an abstract way to validate the chart view configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see AnalyticsConfigValidator
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class ChartViewValidator implements AnalyticsConfigValidator<ChartView>
{
    private static class Instance
    {
        private static final ChartViewValidator INSTANCE = new ChartViewValidator();
    }

    public static ChartViewValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param chartViews Represents the {@link ChartView} that containing the chart view data
     * @return {@code true} if the filter fields are valid; otherwise, {@code false}
     */
    @Override
    public boolean isValid( final @NonNull Long orgId,
                            final @NonNull Collection<ChartView> chartViews )
    {
        for( final ChartView chartView : chartViews )
        {
            final GraphType graphType = chartView.getGraphType();
            final Collection<ReportField> reportFields = chartView.getReportFields();
            final Collection<FilterField> filterFields = chartView.getFilterFields();
            final Map<Axis, Set<FieldCategory>> fieldTypeMap = new HashMap<>();

            for( final ReportField reportField : reportFields )
            {
                fieldTypeMap.computeIfAbsent( reportField.getAxis(), k -> new HashSet<>() )
                            .add( reportField.getFieldType() );
            }
            final Collection<FieldCategory> xFieldCategories = fieldTypeMap.get( Axis.X_AXIS );
            final Collection<FieldCategory> yFieldCategories = fieldTypeMap.get( Axis.Y_AXIS );

            switch( graphType )
            {
                case PIE, BAR ->
                {
                    if( xFieldCategories.isEmpty() )
                    {
                        return yFieldCategories.contains( FieldCategory.CATEGORICAL ) || yFieldCategories.contains( FieldCategory.AGGREGATE );
                    }
                    else if( !xFieldCategories.contains( FieldCategory.AGGREGATE ) && !yFieldCategories.contains( FieldCategory.CATEGORICAL ) )
                    {
                        return true;
                    }
                    else
                    {
                        return xFieldCategories.contains( FieldCategory.CATEGORICAL ) || xFieldCategories.contains( FieldCategory.AGGREGATE );
                    }
                }
                case LINE ->
                {
                    if( xFieldCategories.isEmpty() )
                    {
                        return yFieldCategories.contains( FieldCategory.CATEGORICAL ) || yFieldCategories.contains( FieldCategory.AGGREGATE );
                    }
                    else if( !xFieldCategories.contains( FieldCategory.AGGREGATE ) )
                    {
                        return true;
                    }
                    else
                    {
                        return xFieldCategories.contains( FieldCategory.CATEGORICAL ) || xFieldCategories.contains( FieldCategory.AGGREGATE );
                    }
                }
            }

            if( !filterFields.isEmpty() )
            {
                return AnalyticsValidator.INSTANCE()
                                         .getFilterValidator()
                                         .isValid( orgId, filterFields );
            }
        }

        return false;
    }
}