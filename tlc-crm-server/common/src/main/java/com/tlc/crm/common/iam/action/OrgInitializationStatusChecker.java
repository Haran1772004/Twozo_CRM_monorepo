package com.tlc.crm.common.iam.action;

import com.tlc.crm.common.action.AbstractCrmAction;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.field.CrmOrgFields;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.iam.client.TokenResolver;
import com.tlc.iam.client.models.AuthRequest;
import com.tlc.iam.common.models.User;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web action to check whether the organization is initialized or not
 *
 * @author sundar
 * @version 1.0
 */
@WebAction(path = "/org/checkStatus")
public class OrgInitializationStatusChecker extends AbstractCrmAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgInitializationStatusChecker.class.getName());

    private final OrganizationService organizationService;

    public OrgInitializationStatusChecker() {
        this.organizationService = OrganizationService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @throws Exception
     */
    @Override
    public void process(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final AuthRequest authRequest = new AuthRequest(request.cookie("accessToken"), request.ipAddress());

        final EventCallback<User> authCallback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param user Response from the consumer
             */
            @Override
            public void onSuccess(final User user) {

                try {
                    final CrmOrg crmOrg = organizationService.getCrmOrg(organizationService.getOrgId(user.orgId()));

                    if (CrmOrg.InitializationStatus.COMPLETED == crmOrg.getInitializationStatus()) {
                        LOGGER.info("Org Init Check: Organization initialized successfully. Org ID: {}", crmOrg.id());
                        crmResponse.setStatus(HttpStatus.OK);
                        crmResponse.put(CrmOrgFields.IS_INITIALIZED.getName(), true);
                    } else {
                        LOGGER.info("Org Init Check: Organization initialization in progress. Org ID: {}", crmOrg.id());
                        crmResponse.setStatus(HttpStatus.PROCESSING);
                        crmResponse.put(CrmOrgFields.IS_INITIALIZED.getName(), false);
                    }
                    crmResponse.send();
                } catch (Throwable throwable) {
                    sendError(crmResponse, HttpStatus.OK, throwable);
                }
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, HttpStatus.UNAUTHORIZED, throwable);
            }
        };
        TokenResolver.get().resolve(authRequest, authCallback);
    }

}
