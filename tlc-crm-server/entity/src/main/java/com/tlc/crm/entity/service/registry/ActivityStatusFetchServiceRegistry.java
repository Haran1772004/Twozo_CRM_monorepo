package com.tlc.crm.entity.service.registry;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.service.ActivityStatusIconService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Holds {@link ActivityStatusIconService} instances.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ActivityStatusFetchServiceRegistry implements AutoServiceRegistry<FieldKeys, ActivityStatusIconService> {

    private final Map<FieldKeys, ActivityStatusIconService> activityStatusFetchServiceMap;

    private ActivityStatusFetchServiceRegistry() {
        activityStatusFetchServiceMap = new HashMap<>(NumericalConstant.ONE);
    }

    private static class Instance {
        private static final ActivityStatusFetchServiceRegistry INSTANCE = new ActivityStatusFetchServiceRegistry();
    }

    /**
     * <p>
     * Gets {@link ActivityStatusFetchServiceRegistry}.
     * </p>
     *
     * @return {@link ActivityStatusFetchServiceRegistry}.
     */
    public static ActivityStatusFetchServiceRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Puts instances.
     * </p>
     *
     * @param fieldKeys                  {@link FieldKeys}.
     * @param activityStatusIconService {@link ActivityStatusIconService}.
     */
    @Override
    public void put(final FieldKeys fieldKeys, final ActivityStatusIconService activityStatusIconService) {
        activityStatusFetchServiceMap.put(fieldKeys, activityStatusIconService);
    }

    /**
     * <p>
     * Opts {@link Optional<ActivityStatusIconService>}.
     * </p>
     *
     * @param fieldKeys {@link FieldKeys}.
     * @return {@link Optional<ActivityStatusIconService>}.
     */
    @Override
    public Optional<ActivityStatusIconService> opt(final FieldKeys fieldKeys) {
        return Optional.ofNullable(activityStatusFetchServiceMap.get(fieldKeys));
    }
}
