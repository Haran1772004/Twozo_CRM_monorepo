package com.tlc.sql.olap.internal.meta.ch;

import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.olap.api.meta.OlapTableDefinition;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ClickHouse-specific table definition that extends {@link OlapTableDefinition}
 * with ORDER BY, PARTITION BY, skip index definitions, and cluster configuration.
 */
public class CHTableDefinition extends OlapTableDefinition {

    @Getter
    private final Collection<OrderByDefinition> orderByDefinitions;
    @Getter
    private final Collection<PartitionByDefinition> partitionByDefinitions;
    @Getter
    private final Map<String, SkipIndexDefinition> skipIndexDefinitions;
    private final String cluster;

    public CHTableDefinition(final String name, final TableType type, final int seqId, final CHTableMetaInput metaInput) {
        super(name, type, seqId, metaInput);
        this.orderByDefinitions = metaInput.getOrderByDefinitions();
        this.partitionByDefinitions = metaInput.getPartitionByDefinitions();
        this.skipIndexDefinitions = metaInput.getSkipIndexDefinitions().stream().collect(Collectors.toMap(SkipIndexDefinition::getName, Function.identity()));
        this.cluster = metaInput.getCluster();
    }

    /**
     * Returns the cluster name associated with this table, if any.
     *
     * @return an {@link Optional} containing the cluster name, or empty if not set
     */
    public Optional<String> getCluster() {
        return Optional.ofNullable(cluster);
    }
}