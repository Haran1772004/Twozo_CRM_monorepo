package com.tlc.crm.core.features.action.merge.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.core.features.action.merge.EntityMergeManagement;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.features.api.deal.DealMergeManager;
import com.tlc.crm.core.field.EntityMergeFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.manager.EntityConfigManager;

import com.tlc.web.WebAction;

import java.util.List;
import java.util.Map;

/**
 * Manages the merge deal related web actions
 *
 * @author pandiaraj
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS)
@WebAction(path = "/deal/merge/mgmt")
public class DealMergeManagement extends EntityMergeManagement<Deal> {

    private final DealMergeManager dealMergeManager;
    private final DealConverter dealConverter;
    private final DealManager dealManager;

    public DealMergeManagement() {
        dealMergeManager = DealMergeManager.getInstance();
        dealConverter = DealConverter.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    protected void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(DealFields.DATA);
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final String type = requestJson.getString(DealFields.TYPE);

        switch (type) {
            case EntityMergeFields.PREVIEW -> getDealPreview(orgId, data, request, response);
            case EntityMergeFields.MERGE -> mergeDeal(orgId, userId, data, response);
            case EntityMergeFields.GET_DEAL_FIELDS -> response.put(DealFields.FIELDS, getDealFields(orgId, response));
            case EntityMergeFields.GET_DEAL -> get(orgId, data, request, response);
        }
    }

    @Override
    protected EntityConfigManager<Deal> getEntityManager() {
        return dealManager;
    }

    /**
     * <p>
     * Gets the deal based on the deal id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param data        Holds the {@link JsonObject} details\
     * @param crmRequest  Represents the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void get(final Long orgId, final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long id = data.getLong(DealFields.ID);

        if (!dealManager.exists(orgId, id)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
        final Deal deal = dealMergeManager.get(orgId, id);

        dealConverter.convertPrimaryDealToJson(deal, crmRequest.i18nResolver()).forEach(crmResponse::put);
    }

    /**
     * <p>
     * Gets the deal fields list based on the organization id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmResponse Represents the {@link CrmResponse} details
     * @return The {@link JsonArray} details
     */
    private JsonArray getDealFields(final Long orgId, final CrmResponse crmResponse) {
        final JsonArray jsonArray = dealConverter.getFieldInfoToJson(orgId, crmResponse);
        final JsonObject jsonObject = Json.object();

        jsonObject.put(DealFields.KEY, DealFields.ACTIVITIES_TO_DO);
        jsonObject.put(DealFields.NAME, crmResponse.resolveI18nString("i18n.deal.fields.activities.todo"));
        jsonObject.put(DealFields.STORE_TYPE, StoreType.SINGLE_NUMBER);
        jsonArray.put(jsonObject);

        return jsonArray;
    }

    /**
     * Gets the deal preview details
     *
     * @param orgId       Represents the organization id
     * @param data        Represents the {@link JsonObject}
     * @param request     Represents the {@link CrmRequest} data
     * @param crmResponse Represents the {@link CrmResponse} data
     */
    private void getDealPreview(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse crmResponse) {
        final Long primaryId = data.getLong(DealFields.PRIMARY_DEAL_ID);
        final Long secondaryId = data.getLong(DealFields.SECONDARY_DEAL_ID);

        if (validatePrimaryAndSecondaryEntities(orgId, primaryId, secondaryId,
                DealErrorCodes.SAME_PRIMARY_AND_SECONDARY_ID_RESTRICTED,
                DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found")) {
            final Deal deal = dealMergeManager.getDealPreviewDetails(orgId, primaryId, secondaryId);

            crmResponse.put(DealFields.PREVIEW, dealConverter.convertPrimaryDealToJson(deal, request.i18nResolver()));
        }
    }

    /**
     * Merges the primary and secondary deal
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Response of the Merged deals
     */
    private void mergeDeal(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Long primaryId = data.getLong(DealFields.PRIMARY_DEAL_ID);
        final Long secondaryId = data.getLong(DealFields.SECONDARY_DEAL_ID);

        if (validatePrimaryAndSecondaryEntities(orgId, primaryId, secondaryId,
                DealErrorCodes.SAME_PRIMARY_AND_SECONDARY_ID_RESTRICTED,
                DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found")) {
            final Map<String, Object> mergedData = dealMergeManager.mergeDeals(orgId, userId, primaryId, secondaryId);
            final Deal retainedDeal = mergedData.containsKey(DealFields.DEAL) ?
                    (Deal) mergedData.get(DealFields.DEAL) : dealMergeManager.get(orgId, primaryId);

            crmResponse.put(DealFields.MERGED_STATUS, Map.of(DealFields.MERGED_RECORDS, mergedData.get(DealFields.MERGED_RECORDS)));
            publishMergeIndexData(orgId, retainedDeal, List.of(secondaryId));
        }
    }


}