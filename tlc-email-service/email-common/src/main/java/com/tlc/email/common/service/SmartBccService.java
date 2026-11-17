package com.tlc.email.common.service;

import com.tlc.commons.util.Configurations;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ServiceType;

/**
 * <p>
 * Responsible for initializing and managing Smart BCC related email configuration such as domain, alias prefix, and account generation.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SmartBccService extends AbstractAddOnService {

    private SmartBccService() {
    }

    private static class Instance {
        private static final EmailAddOnService INSTANCE = new SmartBccService();
    }

    public static EmailAddOnService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Starts the Smart BCC service by initializing configuration values.
     * </p>
     *
     * @param configurations application-level configurations
     */
    @Override
    public void start(final Configurations configurations) {
        this.prefix = configurations.getNestedConfigs(EmailFields.SMART_BCC).getConfig(EmailFields.ALIAS_PREFIX);
        super.start(configurations);
    }

    /**
     * <p>
     * Gets the service type handled by this implementation.
     * </p>
     *
     * @return {@link ServiceType}
     */
    @Override
    public ServiceType getServiceType() {
        return ServiceType.SMART_BCC;
    }
}