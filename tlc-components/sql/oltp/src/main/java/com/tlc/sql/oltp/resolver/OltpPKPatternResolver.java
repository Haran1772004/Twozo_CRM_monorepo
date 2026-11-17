package com.tlc.sql.oltp.resolver;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.resolver.PKPatternResolver;
import com.tlc.sql.common.resource.FGSEQUENCEPATTERN;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sql.oltp.api.dml.OltpTable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A resolver is responsible for resolving and managing primary key patterns in data schema or configurations.
 *
 * @author Sathish
 * @version 1.0
 */
public class OltpPKPatternResolver implements PKPatternResolver {

    private final Map<String, Long> pkPatternMap;

    public OltpPKPatternResolver(final Table... tables) {
        if (Objects.isNull(tables) || 0 == tables.length) {
            pkPatternMap = Collections.emptyMap();
            return;
        }
        final Table sequenceTable = getSequenceTable();
        final SelectQuery selectQuery = SelectQuery.get(sequenceTable);
        final List<String> tableNames = Arrays.stream(tables).filter(Objects::nonNull).map(Table::getName).toList();

        selectQuery.addSelectClause(sequenceTable.getColumn(FGSEQUENCEPATTERN.PATTERN));
        selectQuery.addSelectClause(sequenceTable.getColumn(FGSEQUENCEPATTERN.COMMIT_VALUE));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(sequenceTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME), tableNames)));
        final DataContainer dataContainer = SqlServiceProvider.get().getAdminDataStore().get(selectQuery);
        final Stream<Row> sequenceRows = dataContainer.getRows(sequenceTable);

        this.pkPatternMap = sequenceRows.collect(Collectors.toMap(
                sequenceRow -> sequenceRow.get(FGSEQUENCEPATTERN.PATTERN),
                sequenceRow -> sequenceRow.get(FGSEQUENCEPATTERN.COMMIT_VALUE)));
    }

    public Long resolve(final String pattern) {
        return pkPatternMap.get(pattern);
    }

    private Table getSequenceTable() {
        return OltpTable.get(FGSEQUENCEPATTERN.TABLE);
    }
}