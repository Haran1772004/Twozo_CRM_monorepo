package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.dropdown.deal.DealLostReason;
import com.tlc.crm.core.dropdown.deal.DealSource;
import com.tlc.crm.core.dropdown.deal.PaymentStatus;
import com.tlc.crm.core.dropdown.deal.Type;
import com.tlc.crm.core.dropdown.deal.WonReason;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.dropdown.DealSourceStaticOption;
import com.tlc.crm.entity.dropdown.DefaultOption;
import com.tlc.crm.entity.handler.DefaultOptionHandler;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * An action performed by the deal org initializer to initialize organizational
 * fields with predefined values upon the creation of a new organization.
 *
 * @author Kishore J
 * @version 1.0
 */
@OrgInitiator(name = "deal")
public class DealOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealOrgInitiator.class);

    private final FieldService fieldService;
    private final DefaultOptionHandler defaultOptionHandler;

    public DealOrgInitiator() {
        defaultOptionHandler = DefaultOptionHandler.getInstance();
        fieldService = FieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        LOGGER.info("Org Init Action: Deal initialization started. Org ID: {}, User ID: {}", orgId, userId);

        final Map<String, FieldInfo> dealFields = fieldService.
                groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, DealFields.DEAL_SOURCE, DealFields.WON_REASON,
                        DealFields.TYPE, DealFields.LOST_REASON, DealFields.PAYMENT_STATUS,
                        DealFields.STATUS);

        for (final FieldInfo fieldInfo : dealFields.values()) {

            switch (fieldInfo.aliasName()) {
                case "dealSource" -> {
                    final Collection<DefaultOption> defaultOptions = new ArrayList<>();

                    defaultOptions.addAll(DealSourceStaticOption.getOptions());
                    defaultOptions.addAll(DealSource.getOptions());
                    defaultOptionHandler.create(orgId, fieldInfo.id(), defaultOptions);
                }
                case "type" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Type.getOptions());
                case "lostReason" -> defaultOptionHandler.create(orgId, fieldInfo.id(), DealLostReason.getOptions());
                case "paymentStatus" -> defaultOptionHandler.create(orgId, fieldInfo.id(), PaymentStatus.getOptions());
                case "wonReason" -> defaultOptionHandler.create(orgId, fieldInfo.id(), WonReason.getOptions());
            }
        }
        LOGGER.info("Org Init Action: Deal initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
