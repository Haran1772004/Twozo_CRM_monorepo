package com.tlc.crm.entity.list.filter.appender.impl;

import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppender;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;

import com.tlc.sql.common.api.dml.JoinClause;

/**
 * <p>
 * Implements of the {@link CriteriaAppender} interface to provides functionality to appends the min value criteria
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class MinValueCriteriaAppender extends AbstractMinMaxValueCriteriaAppender {

    public static final Type TYPE = () -> "MIN_VALUE_CRITERIA";

    public MinValueCriteriaAppender(final FilterAction providerAction, final FilterSourceType sourceType,
                                    final FilterOptions filterOption, final JoinClause.JoinType joinType) {
        super(providerAction, sourceType, filterOption, joinType, Event.MIN);
    }

    /**
     * {@inheritDoc}
     *
     * @return Appender {@link Type}.
     */
    @Override
    public Type getType() {
        return TYPE;
    }
}