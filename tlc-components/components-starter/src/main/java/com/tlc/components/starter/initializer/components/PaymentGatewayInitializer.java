package com.tlc.components.starter.initializer.components;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;

import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.payment.gateway.internal.service.juspay.JusPayService;
import com.tlc.payment.gateway.internal.registry.PaymentServiceRegistry;
import com.tlc.payment.gateway.internal.service.payu.PayUService;
import com.tlc.payment.gateway.service.PaymentService;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Responsible for triggering the initialization process of the payment gateway services.
 *
 * @author Sathish
 * @version 1.0
 */
@Component
final class PaymentGatewayInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<PaymentService> paymentServiceRegistry;

    private PaymentGatewayInitializer() {
        this.paymentServiceRegistry = PaymentServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.PAYMENT_GATEWAY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return paymentServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(ComponentsStarterFields.PAYMENT_GATEWAY);
            final String paymentGatewayProvider = configurations.getConfig(ComponentsStarterFields.PROVIDER);

            final Optional<PaymentService> optionalPaymentService = Optional.ofNullable(switch (paymentGatewayProvider) {
                case "JusPay" -> new JusPayService();
                case "PayU" -> new PayUService();
                default -> null;
            });

            optionalPaymentService.ifPresent(paymentService -> {
                paymentService.start(configurations);
                paymentServiceRegistry.register(paymentService);
            });
        }
    }
}
