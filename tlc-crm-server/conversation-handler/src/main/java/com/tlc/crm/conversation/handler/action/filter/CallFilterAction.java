package com.tlc.crm.conversation.handler.action.filter;

import com.tlc.crm.conversation.handler.api.manager.internal.EntityConversationManagerImpl;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.features.action.call.manager.impl.CallLogManagerImpl;
import com.tlc.crm.core.sql.resource.OMNCALLLOG;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.notifications.sql.resource.MNCCALLPARTICIPANT;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Objects;

/**
 * Provides filter actions for voice call-related criteria, such as the
 * voice call filter option for contacts.
 *
 * @author Saravanan
 * @version 1.0
 */
@Filter(source = FilterSourceType.CALL)
public class CallFilterAction extends FilterAction {

    private final CallLogManagerImpl callLogManager;
    private final EntityConversationManagerImpl entityConversationManager;
    private final ContactManager contactManager;

    public CallFilterAction() {
        callLogManager = CallLogManagerImpl.getInstance();
        entityConversationManager = EntityConversationManagerImpl.getInstance();
        contactManager = ContactManager.getInstance();
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
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty() ? null :
                criteriaValues.stream().findAny().get();
        final Long valueEntityId = Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId();
        final Operator operator = Operator.getOperator(filterCriteria.getComparator().getAliasName());
        final FilterOptions filterOptions = FilterOptions.get(filterCriteria.getCriteriaField().getAliasName());

        Column column = null;

        if (Objects.isNull(filterOptions)) {
            return super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        }

        final WhereClause filter = switch (filterOptions) {
            case VOICE_CALL -> {
                final Table callLogTable = callLogManager.getTable();
                final Table callParticipantTable = entityConversationManager.getCallParticipantTable();
                column = callLogTable.getPKColumn();

                yield new WhereClause(new Criteria(callLogTable.getPKColumn(), valueEntityId, operator))
                        .and(new Criteria(callParticipantTable.getPKColumn(), valueEntityId, operator));
            }
            default -> super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        };

        final NullableFieldCriteriaAppenderRequisites criteriaRequisites = new NullableFieldCriteriaAppenderRequisites.Builder().build(column);

        setRequisitesIntoNullableFieldCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
        return filter;
    }

    /**
     * {@inheritDoc}
     *
     * @param baseTable             Refers to the base table of the previous {@link FilterAction}, which is used to
     *                              append the joins after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param mapperTables          Refers to the mapper tables of those who had redirected this method. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     */
    @Override
    public void loadJoins(final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails, final Table... mapperTables) {
        if (baseTable.equals(contactManager.getTable())) {
            appendVoiceCallJoins(baseTable.getPKColumn(), filterCriteriaDetails);
        } else {
            super.loadJoins(baseTable, filterCriteriaDetails, mapperTables);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    @Override
    protected void appendJoinsBasedOnField(final FilterCriteriaDetails filterCriteriaDetails) {
        appendVoiceCallJoins(contactManager.getTable().getPKColumn(), filterCriteriaDetails);
    }

    /**
     * Appends the joins required to filter contacts based on voice call criteria.
     *
     * @param contactPkColumn       the primary key column of the contact table.
     * @param filterCriteriaDetails holds the filter options and join loader required to configure the joins.
     */
    private void appendVoiceCallJoins(final Column contactPkColumn, final FilterCriteriaDetails filterCriteriaDetails) {
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();

        if (Objects.nonNull(filterOption) && filterOption == FilterOptions.VOICE_CALL) {
            final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();
            final Table callLogTable = callLogManager.getTable();
            final Table callParticipantTable = entityConversationManager.getCallParticipantTable();

            joinsLoader.load(contactPkColumn, callLogTable.getColumn(OMNCALLLOG.PARTICIPANT_ID));
            joinsLoader.load(contactPkColumn, callParticipantTable.getColumn(MNCCALLPARTICIPANT.CONTACT_ID));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return callLogManager.getTable();
    }
}
