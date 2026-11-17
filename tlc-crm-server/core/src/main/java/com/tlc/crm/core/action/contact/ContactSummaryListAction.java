package com.tlc.crm.core.action.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.list.ContactSummarySQLListInterpreter;
import com.tlc.crm.core.list.EntitySummaryListAction;

import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the list actions in the contact summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class ContactSummaryListAction extends EntitySummaryListAction {

    private final ContactManager contactManager;

    public ContactSummaryListAction() {
        contactManager = ContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ContactSummarySQLListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents the organisation Id
     * @param userId                  Represents the user Id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link ContactSummarySQLListInterpreter}
     */
    @Override
    public ContactSummarySQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                        final Provider provider, final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new ContactSummarySQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @return The provider name.
     */
    @Override
    public Provider getProvider(final ListRequestData listRequestData) {
        final Source source = getSource();

        return switch (source) {
            case DEAL -> Provider.DEAL_LIST;
            case COMPANY -> Provider.COMPANY_LIST;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
    }

    /**
     * <p>
     * Checks whether the field is enabled or not
     * </p>
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    public boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isMandatoryField() || switch (source) {
            case COMPANY -> config.isEnabledForContactSummaryCompanyList();
            case DEAL -> config.isEnabledForContactSummaryDealList();
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
    }

    /**
     * Performs the list action process.
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param crmResponse     Refers the {@link CrmResponse} object.
     */
    @Override
    public void processListAction(final ListRequestData listRequestData, final CrmResponse crmResponse) {
        final JsonObject requestJson = listRequestData.getRequestJson();

        if (requestJson.optBoolean(ListActionFields.FETCH_DATA, true)) {
            final long contactId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();
            final Source source = getSource();

            final JsonArray criteriaArray = switch (source) {
                case COMPANY -> getCompanyListCriteria(contactId);
                case DEAL -> getDealListCriteria(contactId, requestJson);
                default -> null;
            };

            if (Objects.nonNull(criteriaArray)) {
                requestJson.put(CriteriaFields.CRITERIA.getName(), criteriaArray);
                listRequestData.setRequestJson(requestJson);
            }
        }
        super.processListAction(listRequestData, crmResponse);
    }

    /**
     * <p>
     * Loads the contact related deal criteria
     * </p>
     *
     * @param contactId    Represents the contact Id
     * @param selectQuery  Represent the {@link SelectQuery} to fetch deal list summary data
     */
    @Override
    public void loadDealEntityCriteria(final long contactId, final SelectQuery selectQuery) {
        final Table dealTable = dealManager.getTable();
        final Table participantTable = dealManager.getParticipantTable();
        final Column contactPkColumn = contactManager.getTable().getPKColumn();

        selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
        selectQuery.addJoinClause(new JoinClause(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactPkColumn));
        selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(contactPkColumn, contactId)), Connector.AND));
    }

    /**
     * <p>
     * Gets the company list criteria
     * </p>
     *
     * @param contactId Represents the contact Id
     * @return The company list criteria json
     */
    private JsonArray getCompanyListCriteria(final long contactId) {
        final String idKey = DealFields.ID;
        final Long contactSourceId = fieldSourceService.getSource(Source.CONTACT.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, idKey);

        return getCriteriaArray(contactSourceId, contactId, Map.of(fields.get(idKey).id(), Pair.of(CriteriaValueAttributes.VALUE.getName(), FilterConnector.AND)));
    }

    /**
     * <p>
     * Gets the deal list criteria
     * </p>
     *
     * @param contactId Represents the contact Id
     * @return The deal list criteria json
     */
    private JsonArray getDealListCriteria(final long contactId, final JsonObject requestJson) {
        final String primaryContactKey = DealFields.PRIMARY_CONTACT;
        final String relatedContactsKey = DealFields.RELATED_CONTACTS;
        final Long dealSourceId = fieldSourceService.getSource(Source.DEAL.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, primaryContactKey, relatedContactsKey);
        final Map<Long, Pair<String, FilterConnector>> filterFieldMap = new LinkedHashMap<>();
        final String criteriaKey = CriteriaValueAttributes.VALUE_ENTITY_ID.getName();

        filterFieldMap.put(fields.get(primaryContactKey).id(), Pair.of(criteriaKey, FilterConnector.AND));
        filterFieldMap.put(fields.get(relatedContactsKey).id(), Pair.of(criteriaKey, FilterConnector.OR));

        return getDealCriteriaArray(dealSourceId, contactId, requestJson, filterFieldMap);
    }
}