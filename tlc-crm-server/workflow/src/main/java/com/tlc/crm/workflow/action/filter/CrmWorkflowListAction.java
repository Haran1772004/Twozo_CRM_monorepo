package com.tlc.crm.workflow.action.filter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.filter.Type;
import com.tlc.crm.workflow.field.WorkflowAttributes;

/**
 * <p>
 * Handles the HTTP requests that are relevant to workflow list actions
 * </p>
 *
 * @author Kavinila E, Karthick V
 * @version 1.0
 */
public abstract class CrmWorkflowListAction extends AuthorizationController {

    protected static final int DEFAULT_START_VALUE = 1;
    protected static final int DEFAULT_LIMIT = 20;
    protected static final int DEFAULT_FILTER_VALUE = -1;
    protected static final String FILTER_GROUP = "filterGroup";

    protected CrmWorkflowListAction() {
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents an {@link CrmRequest}
     * @param crmResponse Represents an {@link CrmResponse}
     */
    @Override
    protected final void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject requestJson = crmRequest.getRequestJson();

        switch (requestJson.getString(WorkflowAttributes.TYPE)) {
            case "getFilters" -> loadFilters(orgId, userId, getType(), crmResponse);
            case "list" -> loadList(orgId, userId, requestJson.getJsonObject(WorkflowAttributes.DATA), crmResponse);
        }
    }

    /**
     * <p>
     * Loads the filter data which is mapped for the given org id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    protected abstract void loadFilters(final long orgId, final long userId, final Type type, final CrmResponse crmResponse);

    /**
     * <p>
     * Loads the list of {@link Workflow} details
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param jsonObject  The {@link JsonObject} containing filter criteria, pagination details, and sorting information.
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected abstract void loadList(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse crmResponse);

    /**
     * <p>
     * Retrieves the {@link Type} based on the given registered path
     * </p>
     *
     * @return The {@link Type}
     */
    protected abstract Type getType();
}
