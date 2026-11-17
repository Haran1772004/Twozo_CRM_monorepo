package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.commons.util.StringUtil;
import com.tlc.entity.model.TwozoApplication;
import com.tlc.payment.gateway.models.PayerLocalDetails;

/**
 * Implementation of {@link PayerLocalDetails} specific to the CRM application.
 * <p>
 * This class encapsulates payer identity details within the CRM context,
 * using a combination of organization ID and user ID joined by an underscore
 * as the local identity, and a fixed application name "CRM".
 * </p>
 * <p>
 * Typically used for uniquely identifying a payer within the CRM's payment gateway interactions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CRMPayerLocalDetails implements PayerLocalDetails {

    private static final TwozoApplication APPLICATION;
    private static final IdentityType IDENTITY_TYPE;

    private final String localIdentity;

    static {
        IDENTITY_TYPE = () -> "USER";
        APPLICATION = new TwozoApplication();

        APPLICATION.setName("CRM");
    }

    public CRMPayerLocalDetails(final Long orgId, final Long userId) {
        this.localIdentity = StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, orgId.toString(), userId.toString());
    }

    @Override
    public TwozoApplication getApplication() {
        return APPLICATION;
    }

    @Override
    public IdentityType getIdentityType() {
        return IDENTITY_TYPE;
    }

    @Override
    public String getLocalIdentity() {
        return localIdentity;
    }
}
