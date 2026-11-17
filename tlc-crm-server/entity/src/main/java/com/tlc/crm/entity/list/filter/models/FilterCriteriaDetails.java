package com.tlc.crm.entity.list.filter.models;

import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.SourceHolder;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * Holds the details of the join.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public class FilterCriteriaDetails {

    private final JoinsLoader joinsLoader;
    private final JoinPriorityDetails joinPriorityDetails;

    private FieldInfo fieldInfo;
    private SourceHolder sourceHolder;
    private FilterOptions filterOptions;
    private boolean useAliasTable;
    private Table fieldTable;

    public FilterCriteriaDetails() {
        this.joinsLoader = null;
        this.joinPriorityDetails = null;
    }
    public FilterCriteriaDetails(final JoinsLoader joinsLoader, final JoinPriorityDetails joinPriorityDetails) {
        this.joinsLoader = joinsLoader;
        this.joinPriorityDetails = joinPriorityDetails;
    }

    public FilterCriteriaDetails(final boolean useAliasTable, final FilterCriteriaDetails filterCriteriaDetails,
                                 final JoinPriorityDetails joinPriorityDetails) {
        this(filterCriteriaDetails, filterCriteriaDetails.joinsLoader, joinPriorityDetails);
        this.useAliasTable = useAliasTable;
    }

    public FilterCriteriaDetails(final FilterCriteriaDetails filterCriteriaDetails, final JoinsLoader joinsLoader) {
        this(filterCriteriaDetails, joinsLoader, filterCriteriaDetails.joinPriorityDetails);
    }

    private FilterCriteriaDetails(final FilterCriteriaDetails filterCriteriaDetails, final JoinsLoader joinsLoader,
                                   final JoinPriorityDetails joinPriorityDetails) {
        this.joinsLoader = joinsLoader;
        this.fieldInfo = filterCriteriaDetails.fieldInfo;
        this.sourceHolder = filterCriteriaDetails.sourceHolder;
        this.filterOptions = filterCriteriaDetails.filterOptions;
        this.joinPriorityDetails = joinPriorityDetails;
        this.fieldTable = filterCriteriaDetails.fieldTable;
    }

    public void setFieldInfo(final FieldInfo fieldInfo) {
        this.fieldInfo = fieldInfo;
    }

    public void setFilterOptions(final FilterOptions filterOptions) {
        this.filterOptions = filterOptions;
    }

    public void setSourceHolder(final SourceHolder sourceHolder) {
        this.sourceHolder = sourceHolder;
    }

    public JoinsLoader getJoinsLoader() {
        return joinsLoader;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public FilterOptions getFilterOptions() {
        return filterOptions;
    }

    public JoinPriorityDetails getPriorityDetailsHolder() {
        return joinPriorityDetails;
    }

    public SourceHolder getSourceHolder() {
        return sourceHolder;
    }

    public void setUseAliasTable(final boolean useAliasTable) {
        this.useAliasTable = useAliasTable;
    }

    public boolean useAliasTable() {
        return useAliasTable;
    }

    public void setFieldTable(final Table fieldTable) {
        this.fieldTable = fieldTable;
    }

    public Table getFieldTable() {
        return fieldTable;
    }
}