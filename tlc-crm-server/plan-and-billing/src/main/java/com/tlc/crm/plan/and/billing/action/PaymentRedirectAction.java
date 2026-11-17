package com.tlc.crm.plan.and.billing.action;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.cache.service.TransactionCompletionStatusService;
import com.tlc.crm.plan.and.billing.fields.PaymentFields;

import com.tlc.payment.gateway.models.PaymentStatus;

import com.tlc.web.WebAction;

import java.util.Objects;
import java.util.Optional;

/**
 * A publicly accessible {@code WebAction} responsible for handling the final redirection phase after an external payment gateway interaction.
 * <p>
 * This endpoint is designed to receive a final destination URL as a query parameter and immediately issue a permanent
 * HTTP redirect (301 or 308) to that location. This is a common pattern for safely completing the transaction flow
 * initiated by third-party payment services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/payment/redirect")
public class PaymentRedirectAction extends CrmPublicAction {

    private static final String REDIRECTION_URL_FORMAT = "%s&status=%s";

    private final OrgPlanManager planManager;
    private final TransactionCompletionStatusService completionStatusService;

    public PaymentRedirectAction() {
        planManager = OrgPlanManager.getInstance();
        completionStatusService = TransactionCompletionStatusService.getInstance();
    }

    /**
     * Processes the incoming request by extracting the target redirection URL and setting it as a permanent redirect response.
     * <p>
     * The method looks for the final destination URL specified by {@link PaymentFields#URL} in the request parameters.
     * If found, it uses {@code crmResponse.setPermanentRedirectUrl()} to instruct the client's browser to navigate to the new location.
     * </p>
     *
     * @param request     The incoming CRM request, expected to contain the redirection URL as a parameter.
     * @param crmResponse The response object used to set the permanent HTTP redirect URL.
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String redirectionUrl = request.getParameter(PaymentFields.URL);
        final String transactionId = request.getParameter(PaymentFields.TRANSACTION_ID);

        if (Objects.nonNull(redirectionUrl) && Objects.nonNull(transactionId)) {
            final long id = NumberUtils.parseNumber(transactionId).longValue();
            final Optional<PaymentStatus> optionalPaymentStatus = planManager.getPaymentStatus(id);

            optionalPaymentStatus.ifPresentOrElse(paymentStatus -> {
                Boolean completionStatus = completionStatusService.getCompletionStatus(id);

                if (Objects.isNull(completionStatus)) {
                    crmResponse.setPermanentRedirectUrl(redirectionUrl);
                    return;
                }

                do {
                    completionStatus = completionStatusService.getCompletionStatus(id);

                    if (completionStatus) {
                        crmResponse.setPermanentRedirectUrl(String.format(REDIRECTION_URL_FORMAT, redirectionUrl, paymentStatus));
                    }
                } while (!completionStatus);

            }, () -> crmResponse.setPermanentRedirectUrl(redirectionUrl));
        }
    }
}
