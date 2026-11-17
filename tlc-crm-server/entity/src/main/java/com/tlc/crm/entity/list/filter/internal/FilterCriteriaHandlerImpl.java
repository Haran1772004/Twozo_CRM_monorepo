package com.tlc.crm.entity.list.filter.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.impl.DynamicFieldEmptyValueCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.impl.MaxValueCriteriaAppender;
import com.tlc.crm.entity.list.filter.appender.impl.MinValueCriteriaAppender;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.SourceHolder;
import com.tlc.crm.entity.list.filter.appender.impl.CountCriteriaAppender;
import com.tlc.crm.entity.list.filter.loader.impl.DefaultJoinLoader;
import com.tlc.crm.entity.list.filter.loader.impl.MultiSelectFieldJoinLoader;
import com.tlc.crm.entity.list.filter.appender.impl.NullableFieldCriteriaAppender;
import com.tlc.crm.entity.list.filter.loader.impl.TagFieldJoinLoader;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceJoinDetails;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Implements of the {@link FilterCriteriaHandler} interface to provides functionality for handling filter criteria
 * </P>
 *
 * @author Sathish
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see FilterCriteriaHandler
 */
public class FilterCriteriaHandlerImpl implements FilterCriteriaHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterCriteriaHandlerImpl.class);

    private final FilterActionContainer filterActionContainer;
    private final FieldService fieldService;

    private FilterCriteriaHandlerImpl() {
        this.filterActionContainer = FilterActionContainer.getInstance();
        this.fieldService = FieldService.getSecureInstance();
    }

    private static class Instance {
        private static final FilterCriteriaHandlerImpl INSTANCE = new FilterCriteriaHandlerImpl();
    }

    /**
     * <p>
     * Gets the {@link FilterCriteriaHandler} instance
     * </p>
     *
     * @return The {@link FilterCriteriaHandlerImpl}
     */
    public static FilterCriteriaHandlerImpl getInstance() {
        return FilterCriteriaHandlerImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organisation id
     * @param provider           Represents the name of the {@link Provider}
     * @param multiQuery         Represents the {@link MultiQuery} to which the condition will be appended
     * @param filterCriteriaList Represents the {@link FilterCriteria} that holds the values to apply
     */
    @Override
    public void applyCriteria(final Long orgId, final Provider provider, final MultiQuery multiQuery,
                              final Collection<FilterCriteria> filterCriteriaList) {
        boolean hasOrConnector = false;

        for (final FilterCriteria filterCriteria : filterCriteriaList) {
            if (FilterConnector.OR == filterCriteria.getConnector()) {
                hasOrConnector = true;
                break;
            }
        }

        for (final FilterCriteria filterCriteria : filterCriteriaList) {
            final CriteriaField criteriaField = filterCriteria.getCriteriaField();
            final String sourceName = filterCriteria.getFieldSource().getName();
            final FilterSourceType selectedSource = FilterSourceType.get(sourceName);
            final FilterAction providerAction = filterActionContainer.fetchProviderAction(provider);
            final FilterConnector filterConnector = filterCriteria.getConnector();
            final Connector connector = Objects.isNull(filterConnector) ? null : filterConnector.getConnector();
            final SourceHolder sourceHolder = constructSourceHolder(selectedSource, criteriaField);
            final FilterOptions filterOption = FilterOptions.get(criteriaField.getAliasName());
            final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
            final boolean hasNoValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty();
            final Collection<Long> valueEntityIds = hasNoValue ? Collections.emptySet() : criteriaValues.stream().map(CriteriaValue::getValueEntityId).collect(Collectors.toSet());
            final Comparator comparator = filterCriteria.getComparator();
            final Operator operator = Operator.getOperator(comparator.getAliasName());
            final boolean isInverted = operator.isInvertedOperator();
            final boolean isCountableField = FilterOptions.isCountable(filterOption);
            final JoinClause.JoinType joinType = hasOrConnector || isInverted || hasNoValue || isCountableField
                    ? JoinClause.JoinType.LEFT : JoinClause.JoinType.INNER;
            final FilterAction sourceAction = filterActionContainer.fetchSourceAction(selectedSource);
            final Long fieldId = criteriaField.getId();
            final CriteriaAppenderHolder criteriaAppenderHolder = new CriteriaAppenderHolder();
            final NullableFieldCriteriaAppender nullableFieldCriteriaAppender = new NullableFieldCriteriaAppender(comparator, hasNoValue);
            final MaxValueCriteriaAppender maxValueCriteriaAppender = new MaxValueCriteriaAppender(providerAction,
                    selectedSource, filterOption, joinType);
            final MinValueCriteriaAppender minValueCriteriaAppender = new MinValueCriteriaAppender(providerAction,
                    selectedSource, filterOption, joinType);
            final CountCriteriaAppender countCriteriaAppender = new CountCriteriaAppender(hasNoValue, filterCriteria,
                    providerAction, selectedSource, filterOption, joinType, provider);
            final DynamicFieldEmptyValueCriteriaAppender dynamicFieldEmptyValueCriteriaAppender = new DynamicFieldEmptyValueCriteriaAppender(sourceAction,
                    selectedSource, filterOption, comparator);

            criteriaAppenderHolder.add(maxValueCriteriaAppender);
            criteriaAppenderHolder.add(minValueCriteriaAppender);
            criteriaAppenderHolder.add(countCriteriaAppender);
            criteriaAppenderHolder.add(nullableFieldCriteriaAppender);
            criteriaAppenderHolder.add(dynamicFieldEmptyValueCriteriaAppender);

            try {
                final FieldInfo fieldInfo;
                final StoreType storeType;
                final Table fieldTable;
                WhereClause whereClause;

                if (criteriaField.isField()) {
                    fieldInfo = fieldService.getFieldsBySource(orgId, sourceName).stream()
                            .filter(field -> Objects.equals(field.id(), fieldId)).findAny()
                            .orElseThrow(() -> ErrorCode.get(FilterErrorCodes.FILTER_FIELD_NOT_FOUND));

                    storeType = fieldInfo.storeType();
                    fieldTable = fieldInfo.isPersistent() ? fieldInfo.table() : null;
                    whereClause = addFieldCriteria(fieldInfo, filterCriteria, sourceAction, criteriaAppenderHolder);
                } else {
                    fieldInfo = null;
                    storeType = null;
                    fieldTable = null;

                    whereClause = addOptionCriteria(filterCriteria, criteriaAppenderHolder);
                }
                whereClause = criteriaAppenderHolder.doAppend(whereClause);
                whereClause = appendUnmappedCriteria(isInverted, hasNoValue, providerAction, selectedSource, whereClause, operator);

                if (Objects.nonNull(whereClause)) {
                    whereClause = providerAction.appendFieldIdCriteria(isCountableField, provider, selectedSource, hasNoValue, operator, whereClause);

                    appendCondition(multiQuery, whereClause, connector);
                }
                final boolean isPersistentField = Objects.nonNull(fieldInfo) && fieldInfo.isPersistent();
                final JoinsLoader joinsLoader = isPersistentField ? new DefaultJoinLoader(multiQuery, joinType) : getJoinsLoader(multiQuery, joinType, storeType, fieldId, valueEntityIds);
                final FilterSourceType providerSource = filterActionContainer.getSource(providerAction);
                final Optional<JoinPreference> optionalJoinPriorityType = SourceJoinDetails.getJoinPriorityType(providerSource, selectedSource);
                final JoinPreference joinPreference = optionalJoinPriorityType.orElseGet(() -> JoinPreference.ANY);
                final FilterCriteriaDetails filterCriteriaDetails = new FilterCriteriaDetails(joinsLoader, new JoinPriorityDetails(joinPreference));

                filterCriteriaDetails.setFieldInfo(fieldInfo);
                filterCriteriaDetails.setFilterOptions(filterOption);
                filterCriteriaDetails.setSourceHolder(sourceHolder);
                filterCriteriaDetails.setFieldTable(fieldTable);
                providerAction.appendJoins(filterCriteriaDetails);
            } catch (Exception e) {
                LOGGER.info("Filter: Failed to apply filter criteria, ORG ID: {} ID: {}", orgId, fieldId);
            }
        }
    }

    /**
     * Appends unmapped criteria to the existing {@link WhereClause} based on provided conditions.
     *
     * @param isInverted       Indicates if the criteria should be inverted.
     * @param hasNoValue       Indicates if there is no value for the criteria.
     * @param providerAction   The {@link FilterAction} associated with the provider.
     * @param source           The {@link FilterSourceType} source from which criteria are fetched.
     * @param existingCriteria The existing {@link WhereClause} to which criteria may be appended.
     * @param operator         The {@link Operator} used to determine relational or equality conditions.
     * @return The updated {@link WhereClause} with any additional criteria appended if conditions are met.
     */
    private WhereClause appendUnmappedCriteria(final boolean isInverted, final boolean hasNoValue,
                                               final FilterAction providerAction, final FilterSourceType source,
                                               final WhereClause existingCriteria, final Operator operator) {
        final FilterAction sourceAction = filterActionContainer.fetchSourceAction(source);
        WhereClause whereClause = existingCriteria;

        if (!Objects.equals(providerAction, sourceAction) && (isInverted != hasNoValue ||
                (operator.isRelationalOperator() && !operator.isEqualityOperator()))) {
            final Table mainTable = sourceAction.getTable();
            final Criteria criteria = Criteria.notEq(mainTable.getPKColumn(), null);

            whereClause = Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria);
        }

        return whereClause;
    }

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
    @Override
    public JoinsLoader getJoinsLoader(final MultiQuery multiQuery, final JoinClause.JoinType joinType,
                                      final StoreType storeType, final Long fieldId, final Collection<Long> valueEntityIds) {
        if (Objects.isNull(storeType)) {
            return new DefaultJoinLoader(multiQuery, joinType);
        } else {
            return switch (storeType) {
                case MULTI_SELECT -> new MultiSelectFieldJoinLoader(multiQuery, joinType, fieldId, valueEntityIds);
                case TAGS -> new TagFieldJoinLoader(multiQuery, joinType, valueEntityIds);
                default -> new DefaultJoinLoader(multiQuery, joinType);
            };
        }
    }

    /**
     * <p>
     * Checks whether the additional option is null or else. If it is null, fetches the {@link WhereClause} of the {@link CriteriaField}
     * Otherwise fetches the {@link WhereClause} based on {@link AdditionalOption}. Then adds the WhereClause into {@link SelectQuery}
     * </p>
     *
     * @param fieldInfo              {@link FieldInfo} that holds the field information
     * @param filterCriteria         Holds the details of filter criteria, It is used to build the field criteria
     * @param filterAction           Builds the criteria and append the joins
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    private WhereClause addFieldCriteria(final FieldInfo fieldInfo, final FilterCriteria filterCriteria, final FilterAction filterAction,
                                         final CriteriaAppenderHolder criteriaAppenderHolder) {
        return filterAction.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
    }

    /**
     * <p>
     * Fetches the FilterAction of the filter source type for redirects the building of criteria
     * NOTE: filter source type - the source is related to the provided CriteriaOption.(like Contact, Deal, Company etc.,)
     * Then fetches the WhereClause of the CriteriaOption and adds those into {@link SelectQuery}
     * Then fetches the HavingClause for the required FilterCriteria
     * </p>
     *
     * @param filterCriteria         Holds the details of filter criteria, It is used to build the option criteria
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     */
    private WhereClause addOptionCriteria(final FilterCriteria filterCriteria,
                                          final CriteriaAppenderHolder criteriaAppenderHolder) {
        final FilterAction filterAction = filterActionContainer.fetchSourceAction(
                ((CriteriaOption) filterCriteria.getCriteriaField()).getSourceType().getSource());

        return filterAction.buildWhereClause(null, filterCriteria, criteriaAppenderHolder);
    }

    /**
     * <p>
     * Constructs the appender of joins
     * </p>
     *
     * @param source        Name of the source
     * @param criteriaField Holds the details of the {@link CriteriaField}
     * @return {@link SourceHolder} for appends the list of join clauses into {@link SelectQuery}
     */
    private SourceHolder constructSourceHolder(final FilterSourceType source, final CriteriaField criteriaField) {
        return constructSourceHolder(source, criteriaField instanceof final CriteriaOption criteriaOption
                ? criteriaOption.getSourceType().getSource() : null);
    }

    /**
     * <p>
     * Constructs the appender of joins
     * </p>
     *
     * @param source        Name of the source
     * @param relatedSource Holds the details of the {@link CriteriaField}
     * @return {@link SourceHolder} for appends the list of join clauses into {@link SelectQuery}
     */
    @Override
    public SourceHolder constructSourceHolder(final FilterSourceType source, final FilterSourceType relatedSource) {
        return new SourceHolder() {

            @Override
            public FilterSourceType source() {
                return source;
            }

            @Override
            public FilterSourceType relatedSource() {
                return relatedSource;
            }
        };
    }

    /**
     * <p>
     * Appends a condition to the existing where clause of the derived query using the specified connector
     * </p>
     *
     * @param multiQuery  Represents the {@link MultiQuery} to which the condition will be appended
     * @param whereClause Represents the {@link WhereClause} holds the details about retrieval criteria
     * @param connector   Represents the {@link Connector} for combining the clause
     */
    private void appendCondition(final MultiQuery multiQuery, final WhereClause whereClause, final Connector connector) {
        final WhereClause existing = multiQuery.getWhereClause();

        if (Objects.isNull(existing)) {
            multiQuery.setWhereClause(whereClause);
        } else if (Objects.equals(Connector.OR, connector)) {
            multiQuery.setWhereClause(existing.or(whereClause));
        } else {
            multiQuery.setWhereClause(existing.and(whereClause));
        }
    }
}
