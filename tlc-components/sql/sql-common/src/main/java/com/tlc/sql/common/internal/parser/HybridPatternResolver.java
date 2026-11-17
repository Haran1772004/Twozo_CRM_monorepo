package com.tlc.sql.common.internal.parser;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.common.resource.FGSEQUENCEPATTERN;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Abishek
 * @version 1.0
 */
public class HybridPatternResolver extends PatternResolverImpl
{
    private final Set<String> loadedTables;
    public HybridPatternResolver(ReadableDataStore readableDataStore, final TableResolver tableResolver)
    {
        super(readableDataStore, tableResolver);
        this.loadedTables = new HashSet<>();
    }

    @Override
    public Long getLocal(String tableName, String pattern)
    {
        if(loadedTables.contains(tableName))
        {
            return super.getLocal(tableName, pattern);
        }
        else
        {
            final Map<String, Long> map = patternMap.computeIfAbsent(tableName, k -> new HashMap<>());
            final WhereClause whereClause = new WhereClause(Criteria.eq(sequenceTable.getColumn(FGSEQUENCEPATTERN.TABLE_NAME), tableName));
            readableDataStore.fetchData(sequenceTable, whereClause, row ->
            {
                final String patternL = (String) row.get(FGSEQUENCEPATTERN.PATTERN);
                final Long value = (Long) row.get(FGSEQUENCEPATTERN.COMMIT_VALUE);
                map.put(patternL, value);
            });
            loadedTables.add(tableName);
            return map.get(pattern);
        }
    }
}