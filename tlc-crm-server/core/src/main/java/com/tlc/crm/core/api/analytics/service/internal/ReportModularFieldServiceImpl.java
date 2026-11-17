package com.tlc.crm.core.api.analytics.service.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.listeners.Callback;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.resolver.module.ReportModularFieldResolver;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ReportModularFieldService} backed by {@link ReportModularFieldResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ReportModularFieldServiceImpl implements ReportModularFieldService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportModularFieldServiceImpl.class);

    private final ReportModularFieldResolver resolver;

    private ReportModularFieldServiceImpl() {
        this.resolver = ReportModularFieldResolver.getInstance();
    }

    private static class Instance {
        private static final ReportModularFieldServiceImpl INSTANCE = new ReportModularFieldServiceImpl();
    }

    public static ReportModularFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get(final Long orgId, final Long id, final Callback<ReportModularField> callback) {
        if (Objects.isNull(id)) {
            LOGGER.debug("CRM Analytics : ReportModularField get skipped — id is null, orgId: {}", orgId);
            callback.onSuccess(null);
            return;
        }

        LOGGER.debug("CRM Analytics : Fetching ReportModularField by id: {}, orgId: {}", id, orgId);
        resolver.getById(orgId, id, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getByOrgId(final Long orgId, final Callback<Map<Long, ReportModularField>> callback) {
        if (Objects.isNull(orgId)) {
            LOGGER.warn("CRM Analytics : ReportModularField getByOrgId skipped — orgId is null");
            callback.onSuccess(Collections.emptyMap());
            return;
        }

        LOGGER.debug("CRM Analytics : Fetching all ReportModularFields for orgId: {}", orgId);

        resolver.getBySourceId(orgId, new Callback<>() {

            @Override
            public void onSuccess(final Collection<ReportModularField> result) {

                if (Objects.isNull(result) || result.isEmpty()) {
                    LOGGER.debug("CRM Analytics : No ReportModularFields found for orgId: {}", orgId);
                    callback.onSuccess(Collections.emptyMap());
                } else {
                    LOGGER.debug("CRM Analytics : Fetched {} ReportModularFields for orgId: {}", result.size(), orgId);
                    final Map<Long, ReportModularField> map = result.stream().filter(Objects::nonNull)
                            .collect(Collectors.toMap(ReportModularField::id, Function.identity()));
                    callback.onSuccess(map);
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics : Failed to fetch ReportModularFields for orgId: {}", orgId, cause);
                callback.onFailure(cause);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAll(final Long orgId, final Collection<Long> ids, final Callback<Map<Long, ReportModularField>> callback) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            LOGGER.debug("CRM Analytics : ReportModularField getAll skipped — ids is null or empty, orgId: {}", orgId);
            callback.onSuccess(Collections.emptyMap());
            return;
        }

        LOGGER.debug("CRM Analytics : Fetching {} ReportModularFields by ids for orgId: {}", ids.size(), orgId);

        final Map<Long, ReportModularField> resultMap = new ConcurrentHashMap<>();
        final AtomicInteger remaining = new AtomicInteger(ids.size());
        final AtomicInteger failed = new AtomicInteger(NumericalConstant.ZERO);

        for (final Long id : ids) {

            get(orgId, id, new Callback<>() {

                @Override
                public void onSuccess(final ReportModularField result) {

                    if (Objects.nonNull(result)) {
                        resultMap.put(id, result);
                    }

                    if (remaining.decrementAndGet() == NumericalConstant.ZERO && failed.get() == NumericalConstant.ZERO) {
                        LOGGER.debug("CRM Analytics : getAll completed — fetched {}/{} ReportModularFields for orgId: {}", resultMap.size(), ids.size(), orgId);
                        callback.onSuccess(resultMap);
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {

                    if (failed.compareAndSet(NumericalConstant.ZERO, NumericalConstant.ONE)) {
                        LOGGER.error("CRM Analytics : getAll failed for ReportModularField id: {}, orgId: {}", id, orgId, cause);
                        callback.onFailure(cause);
                    }
                }
            });
        }
    }
}
