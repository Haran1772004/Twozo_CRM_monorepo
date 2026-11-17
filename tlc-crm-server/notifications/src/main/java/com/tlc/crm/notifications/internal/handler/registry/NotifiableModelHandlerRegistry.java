package com.tlc.crm.notifications.internal.handler.registry;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notifications.internal.handler.NotifiableModelHandler;
import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Holds {@link NotifiableModelHandler} instances.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class NotifiableModelHandlerRegistry implements
        AutoServiceRegistry<CrmModelType, NotifiableModelHandler<? extends TlcModel>> {

    private final Map<CrmModelType, NotifiableModelHandler<?>> notifiableModelHandlerMap;

    private NotifiableModelHandlerRegistry() {
        notifiableModelHandlerMap = new HashMap<>(NumericalConstant.FIVE);
    }

    private static class Instance {
        private static final NotifiableModelHandlerRegistry INSTANCE = new NotifiableModelHandlerRegistry();
    }

    /**
     * <p>
     * Gets {@link NotifiableModelHandlerRegistry}.
     * </p>
     *
     * @return {@link NotifiableModelHandlerRegistry}.
     */
    public static NotifiableModelHandlerRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Puts instances.
     * </p>
     *
     * @param modelType    {@link CrmModelType}.
     * @param modelHandler {@link NotifiableModelHandler}.
     */
    public void put(final CrmModelType modelType, final NotifiableModelHandler<? extends TlcModel> modelHandler) {
        notifiableModelHandlerMap.put(modelType, modelHandler);
    }

    /**
     * <p>
     * Opts {@link Optional<NotifiableModelHandler>}.
     * </p>
     *
     * @param modelType {@link CrmModelType}.
     * @return {@link Optional<NotifiableModelHandler>}.
     */
    public Optional<NotifiableModelHandler<? extends TlcModel>> opt(final CrmModelType modelType) {
        return Optional.ofNullable(notifiableModelHandlerMap.get(modelType));
    }

}