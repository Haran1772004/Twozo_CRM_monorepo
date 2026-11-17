package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;

import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.action.formatter.ProductZapActionJsonFormatter;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.validator.type.Group;

import java.util.Collection;

/**
 * Implementation of {@link ProductZapActionDispatcher} for handling
 * Zapier actions related to product entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ProductZapActionDispatcher extends AbstractSourceZapActionDispatcher {

    private final ProductConverter productConverter;
    private final ProductManager productManager;
    private final ProductZapActionJsonFormatter productZapActionJsonFormatter;

    private ProductZapActionDispatcher() {
        this.productConverter = ProductConverter.getInstance();
        this.productManager = ProductManager.getInstance();
        this.productZapActionJsonFormatter = ProductZapActionJsonFormatter.getInstance();
    }

    private static final class Instance {
        private static final ProductZapActionDispatcher ZAP_ACTION_DISPATCHER = new ProductZapActionDispatcher();
    }

    public static ProductZapActionDispatcher getInstance() {
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
            case CREATE_PRODUCT -> create(zapActionRequestContext);
            case UPDATE_PRODUCT -> update(zapActionRequestContext);
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
        final JsonObject productJson = productZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());
        final Product product = productConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, false, productJson), productJson);
        final Collection<String> errors = validate(product, Group.Create.class);

        if (errors.isEmpty()) {
            try {
                productManager.create(product);
                index(orgId, product, Action.CREATE);

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.product.create.success"));
                response.send();
            } catch (Throwable throwable) {
                LOGGER.error("Zapier Action Execution: Failed to execute the product create action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.product.create.failed"));
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
        final JsonObject productJson = productZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());

        if (productManager.exists(orgId, productJson.getLong(ZapAttributes.ID))) {
            final Product product = productConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, true, productJson), productJson);
            final Collection<String> errors = validate(product, Group.Update.class);

            if (errors.isEmpty()) {
                try {
                    productManager.update(product);
                    index(orgId, product, Action.UPDATE);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.product.update.success"));
                    response.send();
                } catch (Throwable throwable) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the product update action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.product.update.failed"));
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
