package com.tlc.commons.internal.template.store;

import com.tlc.commons.template.model.TemplateWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the {@link TemplateWrapper} of all the modules.
 *
 * @author Kishore J
 * @version 1.0
 */
public class TemplateWrapperStore {

    private final Map<String, Map<String, TemplateWrapper>> templateWrappers;

    private TemplateWrapperStore() {
        templateWrappers = new ConcurrentHashMap<>();
    }

    private static class InstanceHolder {
        private static final TemplateWrapperStore TEMPLATE_WRAPPER_STORE = new TemplateWrapperStore();
    }

    /**
     * <p>
     * Gets the {@link TemplateWrapperStore} instance
     * </p>
     *
     * @return The {@link TemplateWrapperStore}
     */
    public static TemplateWrapperStore getInstance() {
        return InstanceHolder.TEMPLATE_WRAPPER_STORE;
    }

    /**
     * {@inheritDoc}
     *
     * @param templateName Represents the template name
     * @return An Optional containing the {@link TemplateWrapper} if found, or empty if not found
     */
    public Optional<TemplateWrapper> get(final String templateName) {
        return templateWrappers.values().stream().map(templateWrapperMap -> templateWrapperMap.get(templateName))
                .filter(Objects::nonNull).findFirst();
    }

    /**
     * <p>
     * Registers a template wrapper in the {@link TemplateWrapperStore}
     * </p>
     *
     * @param moduleName         Represents the module name
     * @param templateWrapperMap A {@link Map} contains the key is a template name and the value is a {@link TemplateWrapper}
     */
    public void register(final String moduleName, final Map<String, TemplateWrapper> templateWrapperMap) {
        templateWrappers.computeIfAbsent(moduleName, templateWrappers -> new HashMap<>()).putAll(templateWrapperMap);
    }

    /**
     * <p>
     * Unregisters a module template wrapper from the {@link TemplateWrapperStore}
     * </p>
     *
     * @param moduleName Represents the module name
     */
    public void unregister(final String moduleName) {
        templateWrappers.remove(moduleName);
    }

    /**
     * <p>
     * Clears all registered template wrapper
     * </p>
     */
    public void clear() {
        templateWrappers.clear();
    }
}
