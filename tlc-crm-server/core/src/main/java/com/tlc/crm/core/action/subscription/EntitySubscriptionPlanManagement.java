package com.tlc.crm.core.action.subscription;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;

import com.tlc.crm.core.api.subscription.manager.EntitySummaryManager;
import com.tlc.crm.core.api.subscription.model.DataLossSummary;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.web.WebAction;

/**
 * <p>
 * This endpoint reads the request {@code type} and routes the request to the corresponding
 * handler.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/crm/subscription/entity/mgmt")
public class EntitySubscriptionPlanManagement extends CrmSecureAction {

    private static final String OPEN_DEALS_VALUE = "openDealsValue";
    private static final String OPEN_DEALS_COUNT = "openDealsCount";
    private static final String CONTACTS_COUNT = "contactsCount";
    private static final String COMPANIES_COUNT = "companiesCount";
    private static final String SCHEDULED_ACTIVITIES_COUNT = "scheduledActivitiesCount";

    private final EntitySummaryManager entitySummaryManager;

    public EntitySubscriptionPlanManagement() {
        entitySummaryManager = EntitySummaryManager.getInstance();
    }

    @Override
    public void sProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());

        switch (type) {
            case "dataToBeLost" -> dataToBeLost(crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Loads the summary of organization data that may be lost as part of a subscription plan action.
     * </p>
     *
     * @param crmRequest  holds the current request context.
     * @param crmResponse holds the response to be populated.
     */
    private void dataToBeLost(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final DataLossSummary dataLossSummary = entitySummaryManager.get(crmRequest.orgId());

        crmResponse.put(OPEN_DEALS_VALUE, dataLossSummary.getOpenDealsValue());
        crmResponse.put(OPEN_DEALS_COUNT, dataLossSummary.getOpenDealsCount());
        crmResponse.put(CONTACTS_COUNT, dataLossSummary.getContactsCount());
        crmResponse.put(COMPANIES_COUNT, dataLossSummary.getCompaniesCount());
        crmResponse.put(SCHEDULED_ACTIVITIES_COUNT, dataLossSummary.getScheduledActivitiesCount());
    }
}