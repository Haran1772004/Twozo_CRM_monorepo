package com.tlc.crm.zapier.api.models;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.zapier.fields.ZapAttributes;

import lombok.Getter;

/**
 * Context object that encapsulates the data and metadata for a specific Zapier action request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Getter
public class ZapActionRequestContext {

    private final Long orgId;
    private final Long userId;
    private final JsonObject jsonObject;
    private final ZapAction zapAction;
    private final CrmResponse response;

    public ZapActionRequestContext(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        this.orgId = crmRequest.orgId();
        this.userId = crmRequest.userId();
        final JsonObject data = crmRequest.getRequestJson();
        this.jsonObject = data.getJsonObject(ZapAttributes.STRUCT);
        this.zapAction = ZapAction.get(data.getShort(ZapAttributes.EVENT));
        this.response = crmResponse;
    }
}
