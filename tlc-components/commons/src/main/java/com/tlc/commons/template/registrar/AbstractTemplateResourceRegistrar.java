package com.tlc.commons.template.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.commons.template.model.TemplateRegistrarData;
import com.tlc.commons.template.model.TemplateWrapper;
import com.tlc.commons.template.service.TemplateService;
import com.tlc.commons.internal.template.store.TemplateWrapperStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract for registering template-related resources
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AbstractTemplateResourceRegistrar implements ResourceRegistrar<TemplateRegistrarData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTemplateResourceRegistrar.class);
    private static final String FORWARD_SLASH = "/";
    private static final String DOT = ".";

    protected AbstractTemplateResourceRegistrar() {
    }

    /**
     * <p>
     * Registers the given templates resources files.
     * </p>
     *
     * @param templateRegistrarData Holds the {@link TemplateRegistrarData} which is needed for registering the resources actions.
     */
    @Override
    public void register(final TemplateRegistrarData templateRegistrarData) {
        final Map<String, TemplateWrapper> templateWrapperMap = new HashMap<>();
        final Collection<String> templateUrls = templateRegistrarData.getTemplateUrls();
        final String moduleName = templateRegistrarData.getModuleName();

        for (final String url : templateUrls) {
            final String templateName = url.substring(url.lastIndexOf(FORWARD_SLASH) + 1, url.lastIndexOf(DOT));

            try {
                templateWrapperMap.put(templateName, getTemplateService().get(url));
                LOGGER.info("Template registration successful templateName {}, moduleName {}", templateName, moduleName);
            } catch (final Exception exception) {
                LOGGER.info("Template registration failed templateName {}, moduleName {}, error {}", templateName, moduleName, exception.getMessage());
                throw ErrorCode.get(ErrorCodes.TEMPLATE_REGISTRATION_FAILED);
            }
        }
        TemplateWrapperStore.getInstance().register(templateRegistrarData.getModuleName(), templateWrapperMap);
    }

    /**
     * <p>
     * Removes the registered templates resources files
     * </p>
     *
     * @param registrarData Holds the {@link TemplateRegistrarData} which is needed for removing the resources actions.
     */
    @Override
    public void unregister(final TemplateRegistrarData registrarData) {
        TemplateWrapperStore.getInstance().unregister(registrarData.getModuleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        TemplateWrapperStore.getInstance().clear();
    }

    /**
     * <p>
     * Fetches the {@link TemplateService}
     * </p>
     *
     * @return The {@link TemplateService}
     */
    protected abstract TemplateService<String, ? extends TemplateWrapper> getTemplateService();
}
