package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;

import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.action.formatter.CompanyZapActionJsonFormatter;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.validator.type.Group;

import java.util.Collection;

/**
 * Implementation of {@link CompanyZapActionDispatcher} for handling
 * Zapier actions related to company entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CompanyZapActionDispatcher extends AbstractSourceZapActionDispatcher {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;
    private final CompanyZapActionJsonFormatter companyZapActionJsonFormatter;

    private CompanyZapActionDispatcher() {
        this.companyManager = CompanyManager.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.companyZapActionJsonFormatter = CompanyZapActionJsonFormatter.getInstance();
    }

    private static final class Instance {
        private static final CompanyZapActionDispatcher ZAP_ACTION_DISPATCHER = new CompanyZapActionDispatcher();
    }

    public static CompanyZapActionDispatcher getInstance() {
        return Instance.ZAP_ACTION_DISPATCHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapActionRequestContext The context containing the request, response, and action details.
     */
    @Override
    public void dispatch(final ZapActionRequestContext zapActionRequestContext) {
        switch (zapActionRequestContext.getZapAction()) {
            case CREATE_COMPANY -> create(zapActionRequestContext);
            case UPDATE_COMPANY -> update(zapActionRequestContext);
        }
    }

    /**
     * Handles the creation logic for a Zapier action using the provided request context.
     *
     * @param zapActionRequestContext The context containing request data, metadata, and utilities needed to perform the action.
     */
    private void create(final ZapActionRequestContext zapActionRequestContext) {
        final Long orgId = zapActionRequestContext.getOrgId();
        final CrmResponse response = zapActionRequestContext.getResponse();
        final JsonObject companyJson = companyZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());
        final Company company = companyConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, false, companyJson), companyJson);
        final Collection<String> errors = validate(company, Group.Create.class);

        if (errors.isEmpty()) {
            try {
                companyManager.create(company);
                index(orgId, company, Action.CREATE);

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.company.create.success"));
                response.send();
            } catch (Throwable throwable) {
                LOGGER.error("Zapier Action Execution: Failed to execute the company create action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.company.create.failed"));
                response.setErrorCode(ErrorCode.get(ZapierErrorCodes.CREATE_ACTION_FAILED));
                response.send();
            }
        } else {
            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.DATA_VALIDATION_FAILED));
            response.put(ZapAttributes.ERRORS, errors);
            response.send();
        }
    }

    /**
     * Performs an update operation for a Zapier action using the provided request context.
     *
     * @param zapActionRequestContext The context containing data and parameters required to update the target entity.
     */
    private void update(final ZapActionRequestContext zapActionRequestContext) {
        final Long orgId = zapActionRequestContext.getOrgId();
        final CrmResponse response = zapActionRequestContext.getResponse();
        final JsonObject companyJson = companyZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());

        if (companyManager.exists(orgId, companyJson.getLong(ZapAttributes.ID))) {
            final Company company = companyConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, true, companyJson), companyJson);
            final Collection<String> errors = validate(company, Group.Update.class);

            if (errors.isEmpty()) {
                try {
                    companyManager.update(company);
                    index(orgId, company, Action.UPDATE);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.company.update.success"));
                    response.send();
                } catch (Throwable throwable) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the company update action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.company.update.failed"));
                    response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UPDATE_ACTION_FAILED));
                    response.send();
                }
            } else {
                response.setErrorCode(ErrorCode.get(ZapierErrorCodes.DATA_VALIDATION_FAILED));
                response.put(ZapAttributes.ERRORS, errors);
                response.send();
            }
        } else {
            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.ENTITY_NOT_FOUND));
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.invalid"));
            response.send();
        }
    }
}
