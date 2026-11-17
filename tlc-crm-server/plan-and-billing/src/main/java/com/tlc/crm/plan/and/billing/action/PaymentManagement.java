package com.tlc.crm.plan.and.billing.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.plan.and.billing.api.models.CRMPayerLocalDetails;
import com.tlc.crm.plan.and.billing.fields.PaymentFields;

import com.tlc.payment.gateway.manager.PaymentManager;
import com.tlc.payment.gateway.models.Payment;
import com.tlc.payment.gateway.models.PaymentMethodInfo;
import com.tlc.payment.gateway.models.PaymentResponse;
import com.tlc.payment.gateway.models.PaymentMetaData;
import com.tlc.payment.gateway.models.PaymentMethod;
import com.tlc.payment.gateway.service.PaymentService;
import com.tlc.payment.gateway.service.provider.PaymentServiceProvider;

import com.tlc.web.WebAction;

import java.util.Collection;

/**
 * Controller class responsible for managing payment-related web actions.
 * <p>
 * Provides endpoints under the path "/payment/mgmt" to handle requests related to
 * fetching payment methods, retrieving stored payment methods for a payer, and confirming payments.
 * </p>
 * <p>
 * Uses {@link PaymentService} and {@link PaymentManager} for backend payment processing and data access.
 * </p>
 * <p>
 * Extends {@link AuthorizationController} to ensure that only authorized requests are processed.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/payment/mgmt")
public class PaymentManagement extends AuthorizationController {

    private final PaymentService paymentService;

    public PaymentManagement() {
        paymentService = PaymentServiceProvider.get();
    }


    /**
     * Processes authorized requests for payment management actions.
     *
     * @param request incoming CRM request containing JSON data and context
     * @param crmResponse response object to populate with results
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(PaymentFields.TYPE);

        switch (type) {
            case "availablePaymentMethods" -> {
                final Collection<PaymentMethod> paymentMethods = paymentService.getPaymentMethods();

                crmResponse.put(PaymentFields.PAYMENT_METHODS, convertPaymentMethodsIntoJson(paymentMethods));
            }
            case "userPreferredPaymentMethods" -> {
                final CRMPayerLocalDetails payerLocalDetails = new CRMPayerLocalDetails(request.orgId(), request.userId());
                final Collection<PaymentMethodInfo> paymentMethodInfoList = paymentService.getUsersPaymentMethods(payerLocalDetails);

                crmResponse.put(PaymentFields.PAYMENT_METHODS, convertPaymentTypeInfoIntoJson(paymentMethodInfoList));
            }
            case "verifyPayment" -> {
                final JsonObject data = requestJson.getJsonObject(PaymentFields.DATA);
                final Payment payment = new Payment();

                payment.setTransactionId(data.getString(PaymentFields.TRANSACTION_ID));
                final PaymentResponse paymentResponse = paymentService.confirmPayment(payment);

                crmResponse.put(PaymentFields.MESSAGE, paymentResponse.getStatus().name());
            }
        }
    }

    /**
     * Converts a collection of {@link PaymentMethodInfo} objects into a {@link JsonArray}.
     *
     * @param paymentMethodInfoList the collection of payment type info objects
     * @return JsonArray representation of payment type info
     */
    private JsonArray convertPaymentTypeInfoIntoJson(final Collection<PaymentMethodInfo> paymentMethodInfoList) {
        final JsonArray paymentTypeInfoArray = Json.array();

        for (final PaymentMethodInfo paymentMethodInfo : paymentMethodInfoList) {
            final JsonObject paymentTypeInfoJson = Json.object();

            paymentTypeInfoJson.put(PaymentFields.NAME, paymentMethodInfo.getToken());
            paymentTypeInfoJson.put(PaymentFields.STATUS, paymentMethodInfo.getStatus().name());
            paymentTypeInfoJson.put(PaymentFields.PAYMENT_TYPE, paymentMethodInfo.getPaymentType().getName());
            paymentTypeInfoArray.put(paymentTypeInfoJson);
        }
        return paymentTypeInfoArray;
    }

    /**
     * Converts a collection of {@link PaymentMethod} objects into a {@link JsonArray}.
     *
     * @param paymentMethods the collection of payment methods
     * @return JsonArray representation of payment methods
     */
    private JsonArray convertPaymentMethodsIntoJson(final Collection<PaymentMethod> paymentMethods) {
        final JsonArray paymentMethodsArray = Json.array();

        for (final PaymentMethod paymentMethod : paymentMethods) {
            final JsonObject paymentMethodJson = Json.object();

            paymentMethodJson.put(PaymentFields.NAME, paymentMethod.getName());
            paymentMethodJson.put(PaymentFields.DESCRIPTION, paymentMethod.getDescription());
            paymentMethodJson.put(PaymentFields.TYPE, paymentMethod.getType().name());
            paymentMethodsArray.put(paymentMethodJson);
        }
        return paymentMethodsArray;
    }
}
