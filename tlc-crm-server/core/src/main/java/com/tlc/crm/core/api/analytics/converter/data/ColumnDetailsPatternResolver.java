package com.tlc.crm.core.api.analytics.converter.data;

import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.CaseWhenConfig;
import com.tlc.analytics.config.model.view.report.ReportField;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.resource.FGSEQUENCEPATTERN;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolving column pattern details used in analytics queries.
 *
 * @author karthick V
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnDetailsPatternResolver {

    private static class Instance {
        private static final ColumnDetailsPatternResolver INSTANCE = new ColumnDetailsPatternResolver();
    }

    /**
     * Gets the {@link ColumnDetailsPatternResolver} instance
     *
     * @return {@link ColumnDetailsPatternResolver}
     */
    public static ColumnDetailsPatternResolver INSTANCE() {
        return ColumnDetailsPatternResolver.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets a map of column patterns to their commit values.
     * </p>
     *
     * @param reportFields the report fields containing case-when configurations
     * @param filterFields the filter fields containing case-when configurations
     * @return a map where keys are pattern names and values are their commit values, or an empty map if no patterns are found
     */
    public Map<String, Long> getColumnPatternMap(final Collection<ReportField> reportFields, final Collection<FilterField> filterFields) {
        final Set<String> patternSet = Stream.concat(reportFields.stream().flatMap(this::getCaseWhenStream),
                filterFields.stream().flatMap(this::getCaseWhenStream)).map(CaseWhenConfig::getColumn)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (patternSet.isEmpty()) {
            return Collections.emptyMap();
        }

        final Table fgSequencePatternTable = Table.get(FGSEQUENCEPATTERN.TABLE);
        final Column patternColumn = fgSequencePatternTable.getColumn(FGSEQUENCEPATTERN.PATTERN);
        final SelectQuery patternSelectQuery = SelectQuery.get(fgSequencePatternTable);

        patternSelectQuery.addSelectClause(List.of(patternColumn, fgSequencePatternTable.getColumn(FGSEQUENCEPATTERN.COMMIT_VALUE)));
        patternSelectQuery.setWhereClause(new WhereClause(Criteria.in(patternColumn, patternSet)));

        final DataContainer dataContainer = DataStoreProvider.getReadableDataStore().get(patternSelectQuery);

        return Objects.isNull(dataContainer) ? Collections.emptyMap() : dataContainer.getRows(fgSequencePatternTable)
                .collect(Collectors.toMap(row -> row.get(FGSEQUENCEPATTERN.PATTERN), row -> row.get(FGSEQUENCEPATTERN.COMMIT_VALUE)));
    }

    /**
     * <p>
     * Safely retrieves a Stream of {@link CaseWhenConfig} from the given {@link BaseField}.
     * </p>
     *
     * @param field it holds the {@link BaseField} details
     * @return a {@link Stream} of {@link CaseWhenConfig} details
     */
    private Stream<CaseWhenConfig> getCaseWhenStream(final BaseField field) {
        final Collection<CaseWhenConfig> caseWhenConfig = field.getCaseWhenConfig();

        return Objects.isNull(caseWhenConfig) ? Stream.empty() : caseWhenConfig.stream();
    }
}


