package com.tlc.crm.common.iam.internal;

import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.manager.impl.CrmUserManagerImpl;
import com.tlc.crm.common.iam.handler.OrgInitializationStatusHandler;
import com.tlc.crm.common.iam.handler.OrgInitializationStatusHandler.InternalOrgInitializationStatus;
import com.tlc.crm.common.iam.internal.manager.impl.OrganizationManagerImpl;

import com.tlc.crm.security.context.SecureTaskProcessor;

import com.tlc.iam.common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Performs the organization initialization process
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public final class OrgInitializer implements SecureTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgInitializer.class);
    private final CrmUserManagerImpl crmUserManager;
    private final OrganizationManagerImpl organizationManager;
    private final OrgInitiatorContainer orgInitiatorContainer;
    private final OrgInitializationStatusHandler orgInitializationStatusHandler;

    private OrgInitializer() {
        crmUserManager = CrmUserManagerImpl.getInstance();
        organizationManager = OrganizationManagerImpl.getInstance();
        orgInitiatorContainer = OrgInitiatorContainer.getInstance();
        this.orgInitializationStatusHandler = OrgInitializationStatusHandler.getInstance();
    }

    private static class Instance {
        private static final OrgInitializer INSTANCE = new OrgInitializer();
    }

    /**
     * <p>
     * Gets the {@link OrgInitializer} Instance
     * </p>
     *
     * @return {@link OrgInitializer}
     */
    public static OrgInitializer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Initializes the {@link CrmOrg} and {@link CrmUser}
     * </p>
     *
     * @param crmUserEntity Represents the {@link CrmUserEntity} details
     */
    public void initialize(final CrmUserEntity crmUserEntity, final User iamUser) {
        final CrmOrg crmOrg = crmUserEntity.getCrmOrg();
        final Long iamOrgId = crmOrg.getIamOrgId();

        orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.NEW);
        crmOrg.setInitializationStatus(CrmOrg.InitializationStatus.NEW);
        organizationManager.create(crmOrg);
        final Long orgId = crmOrg.id();

        secureProcess(crmUserEntity, () -> {
            crmUserEntity.setUser(iamUser);
            crmUserManager.create(orgId, crmUserEntity);
            orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.IN_PROGRESS);

            try {

                for (final OrgInitiatorAction orgInitiatorAction : orgInitiatorContainer.getRegisteredActions()) {
                    orgInitiatorAction.execute(crmOrg, crmUserEntity);
                }
                organizationManager.updateInitializationStatus(orgId, CrmOrg.InitializationStatus.INTERNALLY_COMPLETED);

                orgInitializationStatusHandler.handleSafely(iamOrgId, internalOrgInitializationStatus -> {
                    switch (internalOrgInitializationStatus) {
                        case SIGNUP_PROCESS_DONE -> {
                            orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.FINISHED);
                            organizationManager.updateInitializationStatus(orgId, CrmOrg.InitializationStatus.COMPLETED);
                        }
                        case FAILED -> {
                            orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.INTERNAL_PROCESS_DONE);
                            organizationManager.updateInitializationStatus(orgId, CrmOrg.InitializationStatus.FAILED);
                        }
                        default -> {
                            orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.INTERNAL_PROCESS_DONE);
                        }
                    }
                });
            } catch (Throwable throwable) {
                orgInitializationStatusHandler.handleSafely(iamOrgId, ignored -> {
                    orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.FAILED);
                    organizationManager.updateInitializationStatus(orgId, CrmOrg.InitializationStatus.FAILED);

                    LOGGER.error("Org Init Action: Initialization failed. Org ID: {}, User ID: {}, Reason: {}, Stack: ", crmOrg.id(), crmUserEntity.id(), throwable.getMessage(), throwable);
                });
            }
        });
    }
}
