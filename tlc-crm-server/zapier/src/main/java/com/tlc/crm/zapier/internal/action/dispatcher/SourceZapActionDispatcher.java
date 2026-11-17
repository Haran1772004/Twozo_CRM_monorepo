package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * Specialized dispatcher for handling Zapier action executions related to Source entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface SourceZapActionDispatcher extends ZapActionDispatcher {

    /**
     * Gets the instance of {@code SourceZapActionDispatcher}.
     *
     * @param source Refers the source.
     * @return The instance of {@code SourceZapActionDispatcher}.
     */
    static SourceZapActionDispatcher getInstance(final Source source) {
        return switch (source) {
            case COMPANY -> CompanyZapActionDispatcher.getInstance();
            case CONTACT -> ContactZapActionDispatcher.getInstance();
            case DEAL -> DealZapActionDispatcher.getInstance();
            case PRODUCT -> ProductZapActionDispatcher.getInstance();
            case ACTIVITY -> ActivityZapActionDispatcher.getInstance();
            default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_SOURCE);
        };
    }

    /**
     * Validates the given model instance against the specified validation groups.
     *
     * @param model  The model to validate.
     * @param groups Optional validation groups to apply.
     * @return A collection of validation error messages. If the model is valid, the collection will be empty.
     */
    Collection<String> validate(final TlcModel model, final Class<?>... groups);
}
