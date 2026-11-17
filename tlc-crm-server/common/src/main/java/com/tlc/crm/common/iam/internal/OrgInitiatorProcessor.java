package com.tlc.crm.common.iam.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.internal.manager.OrganizationManager;
import com.tlc.crm.common.iam.api.model.CrmOrg;

import com.tlc.iam.common.models.User;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A Processor that process the org initiator requests and triggers the respective org initiator actions
 * based on the request parameters
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class OrgInitiatorProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgInitiatorProcessor.class);
    private final OrgInitializer orgInitializer;
    private final OrganizationManager organizationManager;

    public OrgInitiatorProcessor() {
        orgInitializer = OrgInitializer.getInstance();
        organizationManager = OrganizationManager.getInstance();
    }

    /**
     * <p>
     * Triggers the org initiator.
     * </p>
     *
     * @param streamMessage A record that holds the org initiator request parameters
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject requestParameters = Json.object(streamMessage.data());
        final Long iamOrgId = requestParameters.getLong("id");
        final Long iamUserId = requestParameters.getLong("userId");
        final CrmOrg crmOrg = new CrmOrg();
        final User user = new User();
        final CrmUserEntity crmUser = new CrmUserEntity();

        crmOrg.setIamOrgId(iamOrgId);
        user.setOrgId(iamOrgId);
        user.setFirstName(requestParameters.getString("fullName"));
        crmUser.setIamId(iamUserId);
        crmUser.setCrmOrg(crmOrg);

        LOGGER.info("Org Init Action: Initialization request received. IAM Org ID: {}, IAM User ID: {}", iamOrgId, iamUserId);

        try {
            LOGGER.info("Org Init Action: Initialization starting for IAM Org ID: {}, IAM User ID: {}", iamOrgId, iamUserId);
            orgInitializer.initialize(crmUser, user);
            LOGGER.info("Org Init Action: Initialization ended for IAM Org ID: {}, IAM User ID: {}", iamOrgId, iamUserId);
        } catch (Throwable throwable) {
            organizationManager.updateInitializationStatus(iamOrgId, CrmOrg.InitializationStatus.FAILED);
            LOGGER.error("Org Init Action: Initialize action failed. IAM Org ID: {}, IAM User ID: {}", iamOrgId, iamUserId, throwable);
        }
    }
}
