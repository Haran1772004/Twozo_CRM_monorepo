package com.tlc.crm.core.action.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;

import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.list.DealSummarySQLListInterpreter;
import com.tlc.crm.core.list.EntitySummaryListAction;

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
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the list actions in the deal summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class DealSummaryListAction extends EntitySummaryListAction {

    private final String contactNameKey = "CONTACT_FIRST_NAME";

    /**
     * <p>
     * Gets the {@link DealSummarySQLListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents the organisation Id
     * @param userId                  Represents the user Id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolver}.
     * @return The {@link DealSummarySQLListInterpreter}
     */
    @Override
    public DealSummarySQLListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request, final Provider provider,
                                                                     final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter) {
        return new DealSummarySQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
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
            case PRODUCT -> Provider.PRODUCT_LIST;
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
            case CONTACT -> config.isEnabledForDealSummaryContactList();
            case PRODUCT -> config.isEnabledForDealSummaryProductList();
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
            final long dealId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();
            final Source source = getSource();

            final JsonArray criteriaArray = switch (source) {
                case CONTACT -> getContactListCriteria(dealId);
                case PRODUCT -> getProductListCriteria(dealId);
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
     * {@inheritDoc}
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param responseJson    Represents the list response json
     */
    @Override
    protected void postProcess(final Provider provider, final ListRequestData listRequestData, final JsonObject responseJson) {

        if (Objects.equals(Provider.CONTACT_LIST, provider)) {
            final long dealId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();
            final Long primaryContactId = dealManager.getPrimaryContactId(listRequestData.getOrgId(), dealId);

            loadRelatedContactsData(responseJson, primaryContactId, listRequestData.getRequestJson().optBoolean(ListActionFields.FETCH_STRUCTURE));
        } else if (Objects.equals(Provider.PRODUCT_LIST, provider)) {
            final long dealId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();

            loadProductVariationData(listRequestData.getOrgId(), dealId, responseJson);
        }
    }

    /**
     * <p>
     * Loads the related contacts data into the response json
     * </p>
     *
     * @param responseJson     Represents the response json
     * @param primaryContactId Represents the primary contact id of the deal
     * @param fetchStructure   Represents whether the structure is required on not
     */
    private void loadRelatedContactsData(final JsonObject responseJson, final Long primaryContactId, final boolean fetchStructure) {

        if (fetchStructure && responseJson.containsKey(ListActionFields.STRUCTURE)) {
            final JsonObject structureJson = responseJson.getJsonObject(ListActionFields.STRUCTURE);

            if (structureJson.containsKey(ListActionFields.COLUMNS)) {
                final JsonArray fieldArray = structureJson.getJsonArray(ListActionFields.COLUMNS);
                final String nameKey = DealFields.NAME;
                final String firstNameKey = ContactFields.FIRST_NAME;
                final String fieldTypeKey = FieldKeys.FIELD_TYPE.getName();

                for (int index = 0; index < fieldArray.size(); index++) {
                    final JsonObject fieldJson = fieldArray.getJsonObject(index);

                    if (fieldJson.containsKey(nameKey) && Objects.equals(firstNameKey, fieldJson.getString(nameKey))) {
                        fieldJson.put(fieldTypeKey, contactNameKey);
                        break;
                    }
                }
            }
        }

        if (responseJson.containsKey(ListActionFields.ROWS)) {
            final Collection<Map<String, Object>> entityRows = (Collection<Map<String, Object>>) responseJson.get(ListActionFields.ROWS);
            final String idKey = ListActionFields.ID;
            final String actionVisibleKey = DealFields.IS_ACTION_VISIBLE;
            final String isPrimaryKey = DealFields.IS_PRIMARY;

            for (final Map<String, Object> map : entityRows) {

                if (map.containsKey(idKey)) {

                    if (Objects.equals(primaryContactId, map.get(idKey))) {
                        map.put(isPrimaryKey, true);
                    } else {
                        map.put(actionVisibleKey, true);
                    }
                }
            }
            final Collection<Map<String, Object>> list = entityRows.stream().sorted(Comparator
                    .comparing(map -> Boolean.TRUE.equals(map.get(isPrimaryKey)), Comparator.reverseOrder())).toList();

            responseJson.put(ListActionFields.ROWS, list);
        }
    }

    /**
     * <p>
     * Gets the contact list criteria
     * </p>
     *
     * @param dealId Represents the deal Id
     * @return The contact list criteria json
     */
    private JsonArray getContactListCriteria(final long dealId) {
        final String idKey = DealFields.ID;
        final Long dealSourceId = fieldSourceService.getSource(Source.DEAL.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, idKey);

        return getCriteriaArray(dealSourceId, dealId, Map.of(fields.get(idKey).id(), Pair.of(CriteriaValueAttributes.VALUE.getName(), FilterConnector.AND)));
    }

    /**
     * <p>
     * Gets the product list criteria
     * </p>
     *
     * @param dealId Represents the deal Id
     * @return The product list criteria json
     */
    private JsonArray getProductListCriteria(final long dealId) {
        final String idKey = DealFields.ID;
        final Long productSourceId = fieldSourceService.getSource(Source.DEAL.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, idKey);

        return getCriteriaArray(productSourceId, dealId, Map.of(fields.get(idKey).id(), Pair.of(CriteriaValueAttributes.VALUE.getName(), FilterConnector.AND)));
    }

    /**
     * <p>
     * Loads the product variation data into the response json
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param dealId       Represents the deal id
     * @param responseJson Represents the response json containing product rows
     */
    private void loadProductVariationData(final long orgId, final long dealId, final JsonObject responseJson) {
        if (responseJson.containsKey(ListActionFields.ROWS)) {
            final Collection<Map<String, Object>> entityRows = (Collection<Map<String, Object>>) responseJson.get(ListActionFields.ROWS);
            final String idKey = ListActionFields.PRODUCT_DETAIL_ID;
            final ProductDetailsHolderManager productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
            final Collection<Long> productDetailIds = new HashSet<>();

            entityRows.forEach(map -> {
                if (map.containsKey(idKey)) {
                    productDetailIds.add(NumberUtils.parseNumber(map.get(idKey)).longValue());
                }
            });
            final Map<Long, String> productVariationName = productDetailsHolderManager.getProductVariationName(orgId, dealId, productDetailIds);

            for (final Map<String, Object> map : entityRows) {
                final Long id = (Long) map.get(idKey);

                if (productVariationName.containsKey(id)) {
                    map.put(ListActionFields.NAME, String.format("%s (%s)", map.get(ListActionFields.NAME), productVariationName.get(id)));
                }
                map.put(ListActionFields.PRODUCT_ID, map.remove(ListActionFields.ID));
                map.put(ListActionFields.ID, map.remove(idKey));
            }
        }
    }
}