package com.tlc.crm.plan.and.billing.action;

import com.tlc.commons.data.DataProcessorProvider;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.fields.SubscriptionPlanFields;
import com.tlc.crm.plan.and.billing.util.TransactionIdUtil;

import com.tlc.payment.gateway.models.PaymentStatus;
import com.tlc.payment.gateway.webhook.model.PaymentWebhookData;

import com.tlc.web.WebAction;

import java.util.Objects;
import java.util.Optional;

/**
 * Action class that handles payment webhook processing.
 *
 * Retrieves the appropriate webhook processor based on the provider name,
 * processes the incoming webhook request data, and upgrades the organization's plan
 * based on the transaction ID extracted from the webhook data.
 *
 * Annotated as a WebAction for endpoint routing.
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/{provider}/payment/webhook")
public class PaymentWebhookAction extends CrmPublicAction {

    private final OrgPlanManager orgPlanManager;

    public PaymentWebhookAction() {
        orgPlanManager = OrgPlanManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String providerName = request.getParameter(SubscriptionPlanFields.PROVIDER);

        if (Objects.nonNull(providerName)) {
            final DataProcessor<PaymentWebhookData> dataProcessor = DataProcessorProvider.getProcessor(providerName);
            final RequestData<JsonObject> requestData = new RequestData<>(request.getRequestJson(), request.getParams());

            dataProcessor.process(requestData, webhookData -> {
                if (Objects.equals(request.getDomain(), webhookData.getAllowedOrigin())) {
                    final PaymentStatus paymentStatus = webhookData.getPaymentStatus();

                    switch (paymentStatus) {
                        case SUCCESS -> {
                            final Optional<Long> optionalOrgId = TransactionIdUtil.extractOrgId(webhookData.getTransactionId());

                            optionalOrgId.ifPresent(orgId -> orgPlanManager.activatePlan(orgId, webhookData.getLocalTransactionId()));
                        }
                        case FAILED -> {
                            final Optional<Long> optionalOrgId = TransactionIdUtil.extractOrgId(webhookData.getTransactionId());

                            optionalOrgId.ifPresent(orgId -> orgPlanManager.updateTransactionStatus(orgId, webhookData.getLocalTransactionId(), paymentStatus));
                        }
                    }
                }
            });
        }
    }
}