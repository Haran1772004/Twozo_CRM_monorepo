package com.tlc.crm.entity.internal;

import com.tlc.commons.Counter;
import com.tlc.crm.entity.dropdown.DefaultOption;
import com.tlc.crm.entity.handler.DefaultOptionHandler;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.type.Option;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implements of the {@link DefaultOptionHandler} interface that provides functionality for handling default options
 *
 * @author Kishore J
 * @version 1.0
 * @see DefaultOptionHandler
 */
public class DefaultOptionHandlerImpl implements DefaultOptionHandler {

    private final OptionManager optionManager;

    private DefaultOptionHandlerImpl() {
        optionManager = OptionManager.getInstance();
    }

    private static class Instance {
        private static final DefaultOptionHandlerImpl INSTANCE = new DefaultOptionHandlerImpl();
    }

    /**
     * <p>
     * Gets the {@link DefaultOptionHandlerImpl} instance
     * </p>
     *
     * @return The {@link DefaultOptionHandlerImpl}
     */
    public static DefaultOptionHandlerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param fieldId        Represents the field id
     * @param defaultOptions Which contains the collection of default options
     */
    @Override
    public void create(final long orgId, final long fieldId, final Collection<DefaultOption> defaultOptions) {
        final Counter orderIncrementer = new Counter();
        final Collection<Option> options = new ArrayList<>();

        for (final DefaultOption defaultOption : defaultOptions) {
            final Option option = new Option();

            option.setOrgId(orgId);
            option.setFieldId(fieldId);
            option.setOption(defaultOption.getName());
            option.setDefault(defaultOption.isDefault());
            option.setEditable(defaultOption.isEditable());
            option.setOrder(orderIncrementer.incrementAndGet());
            options.add(option);
        }
        optionManager.create(orgId, options);
    }
}
