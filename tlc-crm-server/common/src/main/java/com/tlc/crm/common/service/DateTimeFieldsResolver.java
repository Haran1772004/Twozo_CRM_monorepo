package com.tlc.crm.common.service;

import com.tlc.crm.common.service.impl.DateTimeFieldsResolverImpl;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Resolves the {@link DateTime} field in {@link TlcModel}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface DateTimeFieldsResolver {

    /**
     * <p>
     * Gets the {@link DateTimeFieldsResolver}.
     * </p>
     *
     * @return {@link DateTimeFieldsResolver}.
     */
    static DateTimeFieldsResolver get() {
        return DateTimeFieldsResolverImpl.getInstance();
    }

    /**
     * <p>
     * Resolves the {@link DateTime} fields.
     * </p>
     *
     * @param model {@link TlcModel}.
     */
    void resolve(final TlcModel model);

    /**
     * <p>
     * Resolves the {@link DateTime} fields for given fields.
     * </p>
     *
     * @param model      {@link TlcModel}.
     * @param fieldNames Field names in the model.
     */
    void resolve(final TlcModel model, final String... fieldNames);
}
