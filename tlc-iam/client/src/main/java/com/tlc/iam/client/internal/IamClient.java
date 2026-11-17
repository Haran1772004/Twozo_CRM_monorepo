package com.tlc.iam.client.internal;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that performs the IAM client startup and shutdown activities
 *
 * @author Selvakumar
 * @version 1.0
 */
public class IamClient implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamClient.class);

    private IamClient() {}

    private static final class Instance {
        private static final IamClient INSTANCE = new IamClient();
    }

    public static IamClient getInstance() {
        return IamClient.Instance.INSTANCE;
    }

    /**
     * Performs the startup activities
     *
     * <pre>
     *    Required properties
     *       * productName.auth.success.redirect.url
     *              Example : crm.auth.success.redirect.url=https://twozo.in/landingPage
     *       * productName.auth.failure.redirect.url
     *              Example : crm.auth.failure.redirect.url=https://twozo.in/401page.html
     *
     *    Optional properties
     *       * productName.auth.cookie.domain
     *              By default it will be blank. Example: crm.auth.cookie.domain=.twozo.in
     *       * productName.auth.cookie.path
     *              Default value is <b>/</b>
     *       * productName.auth.cookie.isSecured
     *              Default value is true
     *       * productName.auth.cookie.validity
     *              This value has to be in seconds. Default value is 8 hrs
     *
     * </pre>
     *
     * @param configurations Metadata required to start the IAM Client Services
     */
    @Override
    public void start(final Configurations configurations) {}

    /**
     * Performs the cleanup activities
     */
    @Override
    public void stop() {}
}
