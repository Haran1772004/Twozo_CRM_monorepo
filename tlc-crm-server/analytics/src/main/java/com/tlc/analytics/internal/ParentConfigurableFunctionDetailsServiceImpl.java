package com.tlc.analytics.internal;

import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.resolver.function.ParentConfigurableFunctionDetailResolver;
import com.tlc.analytics.service.function.ParentConfigurableFunctionDetailsService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Implementation of {@link ParentConfigurableFunctionDetailsService} backed by {@link ParentConfigurableFunctionDetailResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ParentConfigurableFunctionDetailsServiceImpl implements ParentConfigurableFunctionDetailsService {

    private final ParentConfigurableFunctionDetailResolver resolver;

    /**
     * <p>
     * Private constructor initializing the resolver dependency.
     * </p>
     */
    private ParentConfigurableFunctionDetailsServiceImpl() {
        this.resolver = ParentConfigurableFunctionDetailResolver.getInstance();
    }

    private static class Instance {
        private static final ParentConfigurableFunctionDetailsServiceImpl INSTANCE = new ParentConfigurableFunctionDetailsServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ParentConfigurableFunctionDetailsServiceImpl}.
     * </p>
     *
     * @return {@link ParentConfigurableFunctionDetailsServiceImpl}
     */
    public static ParentConfigurableFunctionDetailsServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ParentConfigurableFunctionDetail> getParentConfigurableFunctionDetailsList(final Long columnFunctionId) {
        if (Objects.isNull(columnFunctionId)) {
            return Collections.emptyList();
        }
        final Collection<ParentConfigurableFunctionDetail> result = resolver.getBySourceId(columnFunctionId);

        return Objects.nonNull(result) ? result : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParentConfigurableFunctionDetail getParentConfigurableFunctionDetail(final Long functionId) {
        if (Objects.isNull(functionId)) {
            return null;
        }
        return resolver.getById(functionId);
    }
}
