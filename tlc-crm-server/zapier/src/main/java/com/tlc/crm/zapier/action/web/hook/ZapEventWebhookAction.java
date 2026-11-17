package com.tlc.crm.zapier.action.web.hook;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;

import com.tlc.crm.zapier.action.web.secure.SecureZapierAction;
import com.tlc.crm.zapier.api.manager.ZapEventWebhookManager;
import com.tlc.crm.zapier.api.models.ZapEventWebhook;
import com.tlc.crm.zapier.converter.ZapEventWebhookConverter;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;
import com.tlc.crm.zapier.validator.Group;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import com.tlc.web.WebAction;

import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides an abstract way to handle the webhook registration/unregistration of zapier events
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/zap/event/webhook")
public class ZapEventWebhookAction extends SecureZapierAction {

    protected final ConverterConfigBuilder configBuilder;
    protected final ZapEventWebhookConverter zapEventWebhookConverter;
    protected final ZapEventWebhookManager zapTriggerWebhookManager;
    private final ModelValidator modelValidator;

    public ZapEventWebhookAction() {
        this.configBuilder = ConverterConfigBuilder.getInstance();
        this.zapEventWebhookConverter = ZapEventWebhookConverter.getInstance();
        this.zapTriggerWebhookManager = ZapEventWebhookManager.getInstance();
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
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
        final String type = data.getString(ZapAttributes.TYPE);

        switch (type) {
            case "subscribe" -> subscribe(request, data, response);
            case "unsubscribe" -> unsubscribe(request, data, response);
        }
    }

    /**
     * Manages the registration of zapier event webhook subscription.
     *
     * @param request  Refers the {@link CrmRequest}.
     * @param data     Refers the request json.
     * @param response Refers the response to be sent.
     */
    private void subscribe(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        data.put(ZapAttributes.IS_SUBSCRIBED, true);

        final ZapEventWebhook zapEventWebhook = zapEventWebhookConverter.convert(configBuilder.build(request, false, data), data);
        final Collection<String> errors = validate(zapEventWebhook, Group.EventWebhook.class);

        if (errors.isEmpty()) {
            zapTriggerWebhookManager.save(zapEventWebhook);

            response.put(ZapAttributes.ID, zapEventWebhook.id());
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.webhook.subscription.success"));
            response.send();
        } else {
            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.WEBHOOK_SUBSCRIPTION_FAILED));
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.webhook.subscription.failed"));
            response.send();
        }
    }

    /**
     * Manages the registration of zapier event webhook unsubscription.
     *
     * @param request  Refers the {@link CrmRequest}.
     * @param data     Refers the request json.
     * @param response Refers the response to be sent.
     */
    private void unsubscribe(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        data.put(ZapAttributes.IS_SUBSCRIBED, false);

        final ZapEventWebhook zapEventWebhook = zapEventWebhookConverter.convert(configBuilder.build(request, true, data), data);
        final Collection<String> errors = validate(zapEventWebhook, Group.EventWebhook.class);

        if (errors.isEmpty()) {
            zapTriggerWebhookManager.save(zapEventWebhook);
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.webhook.unsubscription.success"));
            response.send();
        } else {
            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.WEBHOOK_UNSUBSCRIPTION_FAILED));
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.webhook.unsubscription.failed"));
            response.send();
        }
    }

    /**
     * Validates the given {@code ZapEventWebhook} instance using the specified validation groups.
     *
     * @param zapEventWebhook The webhook object to be validated.
     * @param validatorGroups One or more validation groups to apply during validation.
     * @return The collection of validation error messages; empty if the object is valid.
     */
    private Collection<String> validate(final ZapEventWebhook zapEventWebhook, final Class<?>... validatorGroups) {
        final Set<ConstraintViolation<ZapEventWebhook>> violations = new HashSet<>(modelValidator.validate(zapEventWebhook, validatorGroups));

        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

}
