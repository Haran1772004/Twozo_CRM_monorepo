package com.tlc.crm.entity.action.filter;

import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.sql.common.api.dml.WhereClause;

/**
 * <p>
 * Provides the action which can be perform visibility related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.VISIBILITY)
public class VisibilityFilterAction extends FilterAction {

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
        return null; //TODO Visibility needed.
   }
}
