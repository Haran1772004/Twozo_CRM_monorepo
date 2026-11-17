package com.tlc.crm.notifications.internal.handler;

import com.tlc.commons.service.registry.auto.service.AutoService;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Handles notifiable models.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface NotifiableModelHandler<T extends TlcModel> extends AutoService<CrmModelType> {

    /**
     * <p>
     * Handles the models.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param models Collection {@link TlcModel}.
     */
    void handle(final Long orgId, final Collection<T> models);
}