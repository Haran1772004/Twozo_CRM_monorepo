package com.tlc.crm.entity.action.filter;

import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.internal.FilterActionContainer;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * <p>
 * Provides the action which can be perform common filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.COMMON)
public class CommonFilterAction extends FilterAction {

    private final FilterActionContainer filterActionContainer;

    public CommonFilterAction() {
        filterActionContainer = FilterActionContainer.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo              {@link FieldInfo} that holds the field information.
     * @param filterCriteria         holds the details of criteria.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     * @return {@link WhereClause} holds the details about retrieval criteria.
     */
    @Override
    public WhereClause buildWhereClause(final FieldInfo fieldInfo, final FilterCriteria filterCriteria,
                                        final CriteriaAppenderHolder criteriaAppenderHolder) {
        final FilterAction filterAction = filterActionContainer.fetchSourceAction(FilterSourceType
                .get(filterCriteria.getFieldSource().getName()));

        return filterAction.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @param baseTable             Refers to the base table of those who had redirected this method, which is used to
     *                              append the joins after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param mapperTables          Refers to the mapper tables of those who had redirected this method. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     */
    @Override
    public void loadJoins(final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails, final Table... mapperTables) {
    }
}
