package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;

import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.action.formatter.ContactZapActionJsonFormatter;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.validator.type.Group;

import java.util.Collection;

/**
 * Implementation of {@link ContactZapActionDispatcher} for handling
 * Zapier actions related to contact entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ContactZapActionDispatcher extends AbstractSourceZapActionDispatcher {

    private final ContactManager contactManager;
    private final ContactConverter contactConverter;
    private final ContactZapActionJsonFormatter contactZapActionJsonFormatter;

    private ContactZapActionDispatcher() {
        this.contactConverter = ContactConverter.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.contactZapActionJsonFormatter = ContactZapActionJsonFormatter.getInstance();
    }

    private static final class Instance {
        private static final ContactZapActionDispatcher ZAP_ACTION_DISPATCHER = new ContactZapActionDispatcher();
    }

    public static ContactZapActionDispatcher getInstance() {
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
            case CREATE_CONTACT -> create(zapActionRequestContext);
            case UPDATE_CONTACT -> update(zapActionRequestContext);
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
        final JsonObject contactJson = contactZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());
        final Contact contact = contactConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, false, contactJson), contactJson);
        final Collection<String> errors = validate(contact, Group.Create.class);

        if (errors.isEmpty()) {
            try {
                contactManager.create(contact);
                index(orgId, contact, Action.CREATE);

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.contact.create.success"));
                response.send();
            } catch (Throwable throwable) {
                LOGGER.error("Zapier Action Execution: Failed to execute the contact create action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.contact.create.failed"));
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
        final JsonObject contactJson = contactZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());

        if (contactManager.exists(orgId, contactJson.getLong(ZapAttributes.ID))) {
            final Contact contact = contactConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, true, contactJson), contactJson);
            final Collection<String> errors = validate(contact, Group.Update.class);

            if (errors.isEmpty()) {
                try {
                    contactManager.update(contact);
                    index(orgId, contact, Action.UPDATE);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.contact.update.success"));
                    response.send();
                } catch (Throwable throwable) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the contact update action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.contact.update.failed"));
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
