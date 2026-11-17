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

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.list.CompanySummarySQLListInterpreter;
import com.tlc.crm.core.list.EntitySummaryListAction;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;

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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the list actions in the company summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class CompanySummaryListAction extends EntitySummaryListAction {

    private final CompanyManager companyManager;

    public CompanySummaryListAction() {
        companyManager = CompanyManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CompanySummarySQLListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents the organisation Id
     * @param userId                  Represents the user Id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link CompanySummarySQLListInterpreter}
     */
    @Override
    public CompanySummarySQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                        final Provider provider, final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new CompanySummarySQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}.
     */
    @Override
    public Provider getProvider(final ListRequestData listRequestData) {
        final Source source = getSource();

        return switch (source) {
            case CONTACT -> Provider.CONTACT_LIST;
            case DEAL -> Provider.DEAL_LIST;
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
            case CONTACT -> config.isEnabledForCompanySummaryContactList();
            case DEAL -> config.isEnabledForCompanySummaryDealList();
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
            final long companyId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();
            final Source source = getSource();

            final JsonArray criteriaArray = switch (source) {
                case CONTACT -> getContactListCriteria(companyId);
                case DEAL -> getDealListCriteria(companyId, requestJson);
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
     * Load the company related deal criteria
     * </p>
     *
     * @param companyId    Represents the company Id
     * @param selectQuery  Represent the {@link SelectQuery} to fetch deal list summary data
     */
    @Override
    public void loadDealEntityCriteria(final long companyId, SelectQuery selectQuery) {
        final Table dealTable = dealManager.getTable();
        final Table dealCompanyTable = dealManager.getDealCompanyTable();
        final Column companyPkColumn = companyManager.getTable().getPKColumn();
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)),
                new JoinClause(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyPkColumn)));
        selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(companyPkColumn, companyId))
                .and(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fields.get(companyKey).id()))), Connector.AND));
    }

    /**
     * <p>
     * Gets the contact list criteria
     * </p>
     *
     * @param companyId Represents the company Id
     * @return The contact list criteria json
     */
    private JsonArray getContactListCriteria(final long companyId) {
        final String companyKey = ContactFields.COMPANY;
        final Long contactSourceId = fieldSourceService.getSource(Source.CONTACT.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, companyKey);

        return getCriteriaArray(contactSourceId, companyId, Map.of(fields.get(companyKey).id(), Pair.of(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(), FilterConnector.AND)));
    }

    /**
     * <p>
     * Gets the deal list criteria
     * </p>
     *
     * @param companyId Represents the company Id
     * @return The deal list criteria json
     */
    private JsonArray getDealListCriteria(final long companyId, final JsonObject requestJson) {
        final String companyKey = DealFields.COMPANY;
        final Long dealSourceId = fieldSourceService.getSource(Source.DEAL.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

        return getDealCriteriaArray(dealSourceId, companyId, requestJson, Map.of(fields.get(companyKey).id(), Pair.of(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(), FilterConnector.AND)));
    }
}