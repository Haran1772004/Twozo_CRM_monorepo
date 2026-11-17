package com.tlc.crm.entity.list.filter.models;

import com.tlc.sql.common.api.dml.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Holds the details of the join priority.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public class JoinPriorityDetails {

    private final JoinPreference joinPreference;
    private Collection<Table> mapperTables;

    public JoinPriorityDetails(final JoinPreference joinPreference) {
        this.joinPreference = joinPreference;
    }

    public JoinPreference getJoinPriorityType() {
        return joinPreference;
    }

    public Collection<Table> getMapperTables() {
        return mapperTables;
    }

    public void setMapperTables(final Collection<Table> mapperTables) {
        this.mapperTables = mapperTables;
    }

    public void addMapperTable(final Table mapperTable) {

        if (Objects.isNull(mapperTables)) {
            mapperTables = new ArrayList<>();
        }
        mapperTables.add(mapperTable);
    }
}