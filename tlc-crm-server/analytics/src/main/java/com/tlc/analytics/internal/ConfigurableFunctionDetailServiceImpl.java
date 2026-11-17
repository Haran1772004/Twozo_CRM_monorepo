package com.tlc.analytics.internal;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.resolver.function.ConfigurableFunctionDetailResolver;
import com.tlc.analytics.service.function.ConfigurableFunctionDetailService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ConfigurableFunctionDetailService} backed by {@link ConfigurableFunctionDetailResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ConfigurableFunctionDetailServiceImpl implements ConfigurableFunctionDetailService {

    private final ConfigurableFunctionDetailResolver resolver;

    /**
     * <p>
     * Private constructor initializing the resolver dependency.
     * </p>
     */
    private ConfigurableFunctionDetailServiceImpl() {
        this.resolver = ConfigurableFunctionDetailResolver.getInstance();
    }

    private static class Instance {
        private static final ConfigurableFunctionDetailServiceImpl INSTANCE = new ConfigurableFunctionDetailServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ConfigurableFunctionDetailServiceImpl}.
     * </p>
     *
     * @return {@link ConfigurableFunctionDetailServiceImpl}
     */
    public static ConfigurableFunctionDetailServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurableFunctionDetail get(final Long functionId) {
        if (Objects.isNull(functionId)) {
            return null;
        }
        return resolver.getById(functionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ConfigurableFunctionDetail> getAll(final Set<Long> functionIds) {
        if (Objects.isNull(functionIds) || functionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return functionIds.stream().map(this::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
