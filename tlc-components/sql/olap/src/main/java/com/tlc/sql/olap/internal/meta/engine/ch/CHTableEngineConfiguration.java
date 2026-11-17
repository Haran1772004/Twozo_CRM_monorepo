package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.olap.internal.meta.engine.OlapTableEngineConfiguration;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Configuration holder for ClickHouse table engine parameters parsed from meta.xml.
 * Maintains both the legacy column-name collection used by existing engines
 * (ReplacingMergeTree, SummingMergeTree) and a unified ordered param list that
 * preserves the exact declaration order of column refs, scalar literals, and
 * function calls for engines like Distributed and ReplicatedReplacingMergeTree.
 *
 * @author sundar
 * @version 1.0
 */
@Getter
public class CHTableEngineConfiguration extends OlapTableEngineConfiguration {

    private final Collection<String> columns;
    private final List<Argument> engineParams;

    /**
     * Creates a new engine configuration with the given engine name.
     *
     * @param name the ClickHouse engine name (e.g. "ReplacingMergeTree", "Distributed")
     */
    public CHTableEngineConfiguration(final String name) {
        super(name);
        this.columns = new HashSet<>();
        this.engineParams = new ArrayList<>();
    }

    /**
     * Adds a column name to the legacy column collection used by existing engines.
     *
     * @param column the column name to add
     */
    public void addColumnDefinition(final String column) {
        columns.add(column);
    }

    /**
     * Appends an engine parameter to the ordered param list.
     * The parameter can be any {@link Argument} subtype — column reference,
     * scalar literal, or function call — and its position in the list
     * reflects its declaration order in the meta.xml engine-configuration block.
     *
     * @param param the engine parameter argument to append
     */
    public void addEngineParam(final Argument param) {
        engineParams.add(param);
    }
}