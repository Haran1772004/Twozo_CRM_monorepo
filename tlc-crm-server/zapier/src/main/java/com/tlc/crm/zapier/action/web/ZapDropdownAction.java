package com.tlc.crm.zapier.action.web;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.internal.DropdownActionContainer;

import com.tlc.crm.zapier.action.web.secure.SecureZapierAction;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.web.WebAction;

import java.util.Objects;

/**
 * Represents a Zapier action that provides dynamic dropdown data for input fields.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/zap/dropdown")
public class ZapDropdownAction extends SecureZapierAction {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final DropdownActionContainer dropdownActionContainer;

    public ZapDropdownAction() {
        this.dropdownActionContainer = DropdownActionContainer.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The request containing necessary data for processing.
     * @param response The response object where the processed result will be stored.
     */
    @Override
    protected void zapProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson();
        final int page = data.getInt(ZapAttributes.PAGE);
        final int limit = data.optInt(ZapAttributes.LIMIT, DEFAULT_LIMIT);
        final int start = (page * limit) + 1;

        final DropdownRequest dropdownRequest = new DropdownRequest(request);

        dropdownRequest.setLimit(new DropdownLimit(start, limit));
        dropdownRequest.setCriteria(new DropdownCriteria());
        dropdownRequest.setClientType(CrmRequest.ClientType.ZAPIER);
        dropdownRequest.setField(null);

        final String name = data.getString(ZapAttributes.NAME);
        final AsyncDropdownAction asyncDropdownAction = dropdownActionContainer.fetchAsyncAction(name);

        if (Objects.isNull(asyncDropdownAction)) {
            final DropdownAction dropdownAction = dropdownActionContainer.fetchAction(name);
            final OptionContainer optionContainer = dropdownAction.fetchZapierData(dropdownRequest);

            response.put(ZapAttributes.LIST, optionContainer.getOptions());
        } else {
            asyncDropdownAction.fetchAndDispatchData(dropdownRequest, response);
        }
    }
}
