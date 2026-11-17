package com.tlc.crm.core.api.analytics.resolver.internal;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.manager.config.field.ReportModularFieldManager;
import com.tlc.commons.listeners.Callback;
import com.tlc.crm.core.api.analytics.cache.module.ReportModularFieldCache;
import com.tlc.crm.core.api.analytics.resolver.module.ReportModularFieldResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link ReportModularFieldResolver} that handles cache-miss resolution asynchronously.
 * Checks cache first; on cache miss, fetches from DB via {@link ReportModularFieldManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ReportModularFieldResolverImpl implements ReportModularFieldResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportModularFieldResolverImpl.class);

    private final ReportModularFieldCache cache;
    private final ReportModularFieldManager reportModularFieldManager;

    /**
     * <p>
     * Private constructor initializing the cache dependency.
     * </p>
     */
    private ReportModularFieldResolverImpl() {
        this.cache = ReportModularFieldCache.getInstance();
        this.reportModularFieldManager = ReportModularFieldManager.INSTANCE();
    }

    private static class Instance {
        private static final ReportModularFieldResolverImpl INSTANCE = new ReportModularFieldResolverImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ReportModularFieldResolverImpl}.
     * </p>
     *
     * @return {@link ReportModularFieldResolverImpl}
     */
    public static ReportModularFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBySourceId(final Long orgId, final Callback<Collection<ReportModularField>> callback) {
        cache.getAsync(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Collection<ReportModularField> result) {
                if (Objects.nonNull(result) && !result.isEmpty()) {
                    callback.onSuccess(result);
                } else {
                    try {
                        final Collection<ReportModularField> reportModularFields = reportModularFieldManager.fetchByOrgId(orgId);

                        if (!reportModularFields.isEmpty()) {
                            callback.onSuccess(reportModularFields);
                            cache.putAsync(orgId, reportModularFields, new Callback<>() {
                                @Override
                                public void onSuccess(final Map.Entry<Long, Collection<ReportModularField>> ignored) {
                                }

                                @Override
                                public void onFailure(final Throwable cause) {
                                    LOGGER.warn("CRM Analytics : Failed to cache ReportModularField for orgId [{}]", orgId, cause);
                                }
                            });
                        } else {
                            callback.onSuccess(Collections.emptyList());
                        }
                    } catch (final Exception exception) {
                        callback.onFailure(exception);
                    }
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                callback.onFailure(cause);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getById(final Long orgId, final Long fieldId, final Callback<ReportModularField> callback) {
        cache.getByIdAsync(orgId, fieldId, new Callback<>() {
            @Override
            public void onSuccess(final ReportModularField result) {
                if (Objects.nonNull(result)) {
                    callback.onSuccess(result);
                } else {
                    try {
                        final ReportModularField reportModularField = reportModularFieldManager.fetchByFieldId(orgId, fieldId);

                        if (Objects.nonNull(reportModularField)) {
                            callback.onSuccess(reportModularField);
                            cache.putByIdAsync(orgId, fieldId, reportModularField, new Callback<>() {
                                @Override
                                public void onSuccess(final Map.Entry<String, ReportModularField> ignored) {
                                }

                                @Override
                                public void onFailure(final Throwable cause) {
                                    LOGGER.warn("CRM Analytics : Failed to cache ReportModularField for fieldId [{}]", fieldId, cause);
                                }
                            });
                        } else {
                            callback.onSuccess(null);
                        }
                    } catch (final Exception exception) {
                        callback.onFailure(exception);
                    }
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                callback.onFailure(cause);
            }
        });
    }
}
