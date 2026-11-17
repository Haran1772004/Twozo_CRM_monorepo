package com.tlc.crm.common.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.web.secure.CSRFTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Abstract class for CRM actions that require CSRF protection
 *
 * @author sundar
 * @version 1.0
 */
public abstract class CSRFProtectedAction extends AbstractCrmAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSRFProtectedAction.class);

    private final CSRFTokenService csrfTokenService;

    protected CSRFProtectedAction() {
        csrfTokenService = CSRFTokenService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     */
    @Override
    public void process(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {

        try {
            final String token = crmRequest.header(CommonFields.CSRF_TOKEN);
            final String csrfToken;

            if (Objects.nonNull(token)) {
                csrfToken = token;
            } else {
                final JsonObject requestJson = crmRequest.getRequestJson();
                final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);

                csrfToken = Objects.isNull(data) ? null : data.optString(CommonFields.CSRF, null);
            }

            if (Objects.isNull(csrfToken)) {
                crmResponse.setErrorCode(ErrorCode.get(CommonErrorCodes.AUTHENTICATION_FAILED));
                crmResponse.send();
            }

            if (csrfTokenService.verify(csrfToken)) {
                secureProcess(crmRequest, crmResponse);
            } else {
                LOGGER.warn("CSRF Action: token verification failed");
                sendError(crmResponse);
            }
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "CSRF Action: Verification failed due to", throwable.getMessage());
            sendError(crmResponse);
            throwable.printStackTrace();
        }
    }

    /**
     * <p>
     * Sends an error response indicating that CSRF protection was violated
     * </p>
     *
     * @param response Holds the {@link CrmResponse}
     */
    private void sendError(final CrmResponse response) {
        response.setErrorCode(ErrorCode.get(CommonErrorCodes.CSRF_TOKEN_INVALID));
        response.send();
    }

    /**
     * <p>
     * Process the secure processing logic for the given CRM request and response.
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     */
    public abstract void secureProcess(final CrmRequest crmRequest, final CrmResponse crmResponse);
}
