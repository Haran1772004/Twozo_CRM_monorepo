package com.tlc.crm.core.action.marketinglist;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.list.MarketingListSQLListInterpreter;

import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.web.WebAction;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Process the contact list action in the marketing list
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS, validateFieldPermission = true)
@WebAction(path = "/marketing/contact/list")
public class MarketingContactListAction extends EntityListAction {

    private final FieldService fieldService;
    private final FieldSourceService fieldSourceService;
    private final ContactManager contactManager;
    private final MarketingListManager marketingListManager;
    private String searchText;

    public MarketingContactListAction() {
        fieldService = FieldService.getSecureInstance();
        fieldSourceService = FieldSourceService.getInstance();
        contactManager = ContactManager.getInstance();
        marketingListManager = MarketingListManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link MarketingListSQLListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents the organisation Id
     * @param userId                  Represents the user Id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter   Represents the {@link I18nResolutionAdapter}.
     * @return {@link MarketingListSQLListInterpreter}
     */
    @Override
    public MarketingListSQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                    final Provider provider, final JoinLoader joinLoader,
                                                                    final I18nResolutionAdapter i18NResolutionAdapter) {
        return new MarketingListSQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     */
    @Override
    protected void applyDefaultCriteria(final ListInterpreter listInterpreter) {
        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            final Table contactTable = contactManager.getTable();
            final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
            final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);

            listInterpreter.setWhereClause(new WhereClause(Criteria.contains(firstNameColumn, searchText)).
                    or(Criteria.contains(lastNameColumn, searchText)).or(Criteria.contains(firstNameColumn.concatWithTrim(lastNameColumn.trim()), searchText)));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}.
     */
    @Override
    public Provider getProvider(final ListRequestData listRequestData) {
        return Provider.CONTACT_LIST;
    }

    /**
     * <p>
     * Checks whether the field is enabled or not
     * </p>
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @param source Represents the {@link Source}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    public boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isMandatoryField() || config.isEnabledForMarketingList();
    }

    /**
     * <p>
     * Loads the summary details into the response JSON.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     */
    @Override
    protected void loadSummaryIntoResponse(final EntityListInterpreter listInterpreter, final JsonObject response) {
    }

    /**
     * <p>
     * Loads the required preview details into the provided entity rows.
     * </p>
     *
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param entityRows      The {@link Map} containing entity IDs to their associated data rows
     */
    @Override
    protected void loadPreviewData(final EntityListInterpreter listInterpreter, final Map<String, Map<String, Object>> entityRows) {
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
        final long marketingListId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();

        marketingListManager.setMarketingListPreference(listRequestData.getOrgId(), listRequestData.getUserId(), marketingListId);

        if (requestJson.optBoolean(ListActionFields.FETCH_DATA, true)) {
            searchText = listRequestData.getParameter(FieldKeys.SEARCH_TEXT.getName());
            final Optional<FieldInfo> optionalMarketingListField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.MARKETING_LIST);

            if (optionalMarketingListField.isPresent()) {
                final JsonArray criteriaArray = Json.array();
                final JsonObject criteriaJson = Json.object();
                final JsonObject fieldJson = Json.object();
                final JsonArray valueArray = Json.array();
                final JsonObject valueJson = Json.object();

                fieldJson.put(ListActionFields.FIELD_ID, optionalMarketingListField.get().id());
                criteriaJson.put(ListActionFields.FIELD, fieldJson);
                criteriaJson.put(ListActionFields.SOURCE_ID, fieldSourceService.getSource(Source.CONTACT.getName()).id());
                criteriaJson.put(CriteriaFields.CONNECTOR_ID.getName(), FilterConnector.AND.getId());
                criteriaJson.put(CriteriaFields.COMPARATOR_ID.getName(), Operator.EQUAL.getId());

                valueJson.put(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(), marketingListId);
                valueArray.put(valueJson);
                criteriaJson.put(CriteriaFields.VALUES.getName(), valueArray);
                criteriaArray.put(criteriaJson);

                requestJson.put(CriteriaFields.CRITERIA.getName(), criteriaArray);
            }
        }
        super.processListAction(listRequestData, crmResponse);
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    @Override
    protected ListType getListType() {
        return ListType.MARKETING_CONTACT_LIST;
    }
}