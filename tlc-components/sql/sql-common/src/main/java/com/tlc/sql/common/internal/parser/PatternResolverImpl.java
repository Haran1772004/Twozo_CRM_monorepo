package com.tlc.sql.common.internal.parser;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.common.resource.FGSEQUENCEPATTERN;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * @author Abishek
 * @version 1.0
 */
public class PatternResolverImpl implements PatternResolver
{
    protected final Map<String, Map<String, Long>> patternMap;
    protected final ReadableDataStore readableDataStore;
    protected final Table sequenceTable;

    public PatternResolverImpl(ReadableDataStore readableDataStore, final TableResolver tableResolver)
    {
        this.patternMap = new HashMap<>();
        this.readableDataStore = Objects.requireNonNull(readableDataStore);
        this.sequenceTable = tableResolver.resolve(FGSEQUENCEPATTERN.TABLE);
    }

    @Override
    public Long getLocal(String tableName, String pattern)
    {
        final Map<String, Long> seqIdMap = patternMap.get(tableName);
        if(seqIdMap == null)
        {
            return null;
        }
        else
        {
            return seqIdMap.get(pattern);
        }
    }

    @Override
    public Long getRemote(String tableName, String pattern)
    {
        final Long local = getLocal(tableName, pattern);
        if(local != null)
        {
            return local;
        }
        else
        {
            final WhereClause whereClause =
                    new WhereClause(Criteria.eq(sequenceTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME), tableName)).and(
                            new WhereClause(Criteria.eq(sequenceTable.getColumn(FGSEQUENCEPATTERN.PATTERN), pattern))
                    );
            final SelectQuery selectQuery = SelectQuery.get(sequenceTable);
            selectQuery.addSelectClause(sequenceTable.getColumn(FGSEQUENCEPATTERN.COMMIT_VALUE));
            selectQuery.setWhereClause(whereClause);

            final DataContainer container = readableDataStore.get(selectQuery);
            if(container.isEmpty())
            {
                return null;
            }
            else
            {
                final Row row = container.getRow(sequenceTable);
                final Long remote = row.get(FGSEQUENCEPATTERN.COMMIT_VALUE);
                store(tableName, pattern, remote);
                return remote;
            }
        }
    }

    @Override
    public void store(String tableName, String pattern, Long value)
    {
        patternMap.computeIfAbsent(tableName, k -> new HashMap<>()).put(pattern, value);
    }
}
