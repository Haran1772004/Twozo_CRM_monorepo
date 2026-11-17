package com.tlc.crm.core.notification.email.list.action.filter;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;

import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.sql.resource.OMECEMAILCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECENTITYCONVERSATIONS;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.CountCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.list.filter.models.JoinPreference;
import com.tlc.crm.entity.list.filter.models.JoinPriorityDetails;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.sql.resource.OMEMDEALEMAIL;
import com.tlc.crm.core.sql.resource.OMEMRECENTINBOUNDCONVERSATION;
import com.tlc.crm.core.sql.resource.OMEMRECENTOUTBOUNDCONVERSATION;

import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides the action which can be perform email related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.EMAIL)
public class EmailFilterAction extends FilterAction {

    private final CoreMailManager mailManager;
    private final DealManager dealManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;

    public EmailFilterAction() {
        mailManager = CoreMailManager.getInstance();
        dealManager = DealManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
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
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final Long valueEntityId = Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId();
        final Operator operator = Operator.getOperator(filterCriteria.getComparator().getAliasName());
        final FilterOptions filterOptions = FilterOptions.get(filterCriteria.getCriteriaField().getAliasName());

        if (Objects.isNull(filterOptions)) {
            return super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        }

        Column column = null;
        final WhereClause filter = switch (filterOptions) {
            case EMAIL_CONVERSATION -> {
                column = getTable().getPKColumn();

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case EMAIL_MESSAGE_COUNT -> {
                final Table table = getTable();
                final CountCriteriaAppenderRequisites criteriaRequisites = new CountCriteriaAppenderRequisites.Builder()
                        .setCount(value)
                        .setNullableCriteriaRequired(false)
                        .setPriorityDetailHolders(List.of(new JoinPriorityDetails(JoinPreference.MAPPER_TABLE)))
                        .setRelatedSourceType(FilterSourceType.EMAIL)
                        .setFieldInfo(fieldInfo)
                        .build(table.getPKColumn());

                setRequisitesIntoCountCriteriaAppender(criteriaRequisites, criteriaAppenderHolder);
                yield new WhereClause(Criteria.eq(table.getColumn(OMEMCONVERSATIONS.IS_ACTIVE), true)).and(
                        Criteria.eq(table.getColumn(OMEMCONVERSATIONS.IS_DELETED), false));
            }
            case LAST_EMAIL_SENT, LAST_EMAIL_RECEIVED -> {
                final FilterAction parentFilterAction = fetchParentFilterAction(fieldInfo);

                yield parentFilterAction.buildCriteriaForDateField(Operator.getOperator(filterCriteria.getComparator().getAliasName()),
                    criteriaValue, getTable().getColumn(OMEMCONVERSATIONS.CREATED_TIME))
                    .and(Criteria.eq(getEntityConversationTable().getColumn(OMECENTITYCONVERSATIONS.TYPE), EntityConversation.Type.EMAIL.getId()));
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
     * @param fieldTable   Represents the table of the column.
     * @param fieldInfo    Represents the details of the field.
     * @param filterOption Represents the option of the filter.
     * @return Field {@link Column}.
     */
    @Override
    protected Column getFieldColumn(final Table fieldTable, final FieldInfo fieldInfo, final FilterOptions filterOption) {
        if (Objects.isNull(filterOption)) {
            return super.getFieldColumn(fieldTable, fieldInfo, null);
        }

        return switch (filterOption) {
            case LAST_EMAIL_SENT -> getRecentOutboundConversationTable().getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CREATED_TIME);
            case LAST_EMAIL_RECEIVED -> getRecentInboundConversationTable().getColumn(OMEMRECENTINBOUNDCONVERSATION.CREATED_TIME);
            case EMAIL_CONVERSATION -> getContactConversationTable().getPKColumn();
            default -> super.getFieldColumn(fieldTable, fieldInfo, filterOption);
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadJoins(final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails, final Table... mapperTables) {
        if (baseTable.equals(dealManager.getTable())) {
            final Table dealConversationTable = getDealConversationTable();
            final Table entityConversationTable = getEntityConversationTable();
            final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();
            final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();

            if (Objects.nonNull(filterOption)) {
                final Column pkColumn = getTable().getPKColumn();
                final Table emailConversationTable = getEmailConversationTable();
                final Column entityConversationPkColumn = entityConversationTable.getPKColumn();

                joinsLoader.load(baseTable.getPKColumn(), dealConversationTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID));
                joinsLoader.load(dealConversationTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.ID));
                joinsLoader.load(entityConversationPkColumn, emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID));
                joinsLoader.load(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), pkColumn);

                switch (filterOption) {
                    case LAST_EMAIL_SENT -> joinsLoader.load(pkColumn, getRecentOutboundConversationTable().getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID));
                    case LAST_EMAIL_RECEIVED -> joinsLoader.load(pkColumn, getRecentInboundConversationTable().getColumn(OMEMRECENTINBOUNDCONVERSATION.CONVERSATION_ID));
                }
            }
        } else if (baseTable.equals(contactManager.getTable())) {
            super.loadJoins(baseTable, filterCriteriaDetails, mapperTables);
            final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();
            final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

            if (Objects.nonNull(filterOption)) {
                final Column pkColumn = getTable().getPKColumn();
                final Table contactConversationTable = getContactConversationTable();
                final Table entityConversationTable = getEntityConversationTable();
                final Table emailConversationTable = getEmailConversationTable();
                final Column entityConversationPkColumn = entityConversationTable.getPKColumn();

                joinsLoader.load(baseTable.getPKColumn(), contactConversationTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID));
                joinsLoader.load(contactConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationTable.getColumn(OMECENTITYCONVERSATIONS.ID));
                joinsLoader.load(entityConversationPkColumn, emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID));
                joinsLoader.load(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), pkColumn);

                switch (filterOption) {
                    case LAST_EMAIL_SENT -> joinsLoader.load(pkColumn, getRecentOutboundConversationTable().getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID));
                    case LAST_EMAIL_RECEIVED -> joinsLoader.load(pkColumn, getRecentInboundConversationTable().getColumn(OMEMRECENTINBOUNDCONVERSATION.CONVERSATION_ID));
                }
            }
        } else if (baseTable.equals(companyManager.getTable())) {
            super.loadJoins(baseTable, filterCriteriaDetails, mapperTables);
            final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();
            final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

            if (Objects.nonNull(filterOption)) {
                final Column pkColumn = getTable().getPKColumn();
                final Table companyConversationTable = getCompanyConversationTable();
                final Table entityConversationTable = getEntityConversationTable();
                final Table emailConversationTable = getEmailConversationTable();
                final Column entityConversationPkColumn = entityConversationTable.getPKColumn();

                joinsLoader.load(baseTable.getPKColumn(), companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID));
                joinsLoader.load(companyConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID), entityConversationPkColumn);
                joinsLoader.load(entityConversationPkColumn, emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.ENTITY_CONVERSATION_ID));
                joinsLoader.load(emailConversationTable.getColumn(OMECEMAILCONVERSATIONS.CONVERSATION_ID), pkColumn);

                switch (filterOption) {
                    case LAST_EMAIL_SENT -> joinsLoader.load(pkColumn, getRecentOutboundConversationTable().getColumn(OMEMRECENTOUTBOUNDCONVERSATION.CONVERSATION_ID));
                    case LAST_EMAIL_RECEIVED -> joinsLoader.load(pkColumn, getRecentInboundConversationTable().getColumn(OMEMRECENTINBOUNDCONVERSATION.CONVERSATION_ID));
                }
            }
        } else {
            super.loadJoins(baseTable, filterCriteriaDetails, mapperTables);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return Array of {@link Table} holds the mapping tables of current source.
     */
    @Override
    protected Table[] getLocalMapperTables() {
        return new Table[]{getCompanyConversationTable(), getContactConversationTable(), getEntityConversationTable()};
    }

    /**
     * {@inheritDoc}
     *
     * @return tag {@link Table}.
     */
    @Override
    public Table getTable() {
        return mailManager.getConversationsTable();
    }

    /**
     * <p>
     * Fetches the table of the deal email.
     * </p>
     *
     * @return Deal email table.
     */
    private Table getDealEmailTable() {
        return Table.get(OMEMDEALEMAIL.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the entity conversation.
     * </p>
     *
     * @return Entity conversation table.
     */
    private Table getEntityConversationTable() {
        return Table.get(OMECENTITYCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the contact conversation.
     * </p>
     *
     * @return Contact conversation table.
     */
    private Table getEmailConversationTable() {
        return Table.get(OMECEMAILCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the contact conversation.
     * </p>
     *
     * @return Contact conversation table.
     */
    private Table getContactConversationTable() {
        return Table.get(OMECCONTACTCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the company conversation.
     * </p>
     *
     * @return Company conversation table.
     */
    private Table getCompanyConversationTable() {
        return Table.get(OMECCOMPANYCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the deal conversation.
     * </p>
     *
     * @return deal conversation table.
     */
    private Table getDealConversationTable() {
        return Table.get(OMECDEALCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the recent outbound conversation.
     * </p>
     *
     * @return Recent outbound conversation table.
     */
    private Table getRecentOutboundConversationTable() {
        return Table.get(OMEMRECENTOUTBOUNDCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the recent inbound conversation.
     * </p>
     *
     * @return Recent inbound conversation table.
     */
    private Table getRecentInboundConversationTable() {
        return Table.get(OMEMRECENTINBOUNDCONVERSATION.TABLE);
    }
}
