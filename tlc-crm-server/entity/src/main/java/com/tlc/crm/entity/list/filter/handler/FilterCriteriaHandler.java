package com.tlc.crm.entity.list.filter.handler;

import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.SourceHolder;
import com.tlc.crm.entity.list.filter.internal.FilterCriteriaHandlerImpl;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;

import java.util.Collection;

/**
 * <p>
 * Handler that provides abstract way to manage filter criteria
 * </p>
 *
 * @author Sathish
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface FilterCriteriaHandler {

    /**
     * <p>
     * Gets the {@link FilterCriteriaHandler} instance
     * </p>
     *
     * @return The {@link FilterCriteriaHandler}
     */
    static FilterCriteriaHandler getInstance() {
        return FilterCriteriaHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Applies the specified filter criteria for the given organization and provider
     * </p>
     *
     * @param orgId              Represents the organisation id
     * @param provider           Represents the name of the {@link Provider}
     * @param multiQuery         Represents the {@link MultiQuery} to which the condition will be appended
     * @param filterCriteriaList Represents the {@link FilterCriteria} that holds the values to apply
     */
    void applyCriteria(final Long orgId, final Provider provider, final MultiQuery multiQuery,
                       final Collection<FilterCriteria> filterCriteriaList);

    /**
     * <p>
     * Constructs the appender of joins
     * </p>
     *
     * @param source        Name of the source
     * @param relatedSource Holds the details of the {@link CriteriaField}
     * @return {@link SourceHolder} for appends the list of join clauses into {@link SelectQuery}
     */
    SourceHolder constructSourceHolder(final FilterSourceType source, final FilterSourceType relatedSource);

    /**
     * <p>
     * Constructs the {@link JoinsLoader} for appends the joins into {@link SelectQuery}
     * </p>
     *
     * @param multiQuery     Represents the {@link MultiQuery}
     * @param joinType       Represents the {@link JoinClause.JoinType} to join the local and remote tables.
     * @param storeType      Represents the store type of the field.
     * @param fieldId        Represents the field id.
     * @param valueEntityIds Holds the value entity ids.
     * @return Constructed {@link JoinsLoader}
     */
    JoinsLoader getJoinsLoader(final MultiQuery multiQuery, final JoinClause.JoinType joinType,
                               final StoreType storeType, final Long fieldId, final Collection<Long> valueEntityIds);
}