package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.dropdown.contact.ContactLostReason;
import com.tlc.crm.core.dropdown.contact.ContactSource;
import com.tlc.crm.core.dropdown.contact.ContactSourceStaticOption;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.dropdown.DefaultOption;
import com.tlc.crm.entity.handler.DefaultOptionHandler;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An action performed by the contact org initializer to initialize organizational fields
 * with predefined values upon the creation of a new organization.
 *
 * @author Kishore J
 * @version 1.0
 */
@OrgInitiator(name = "contact")
public class ContactOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactOrgInitiator.class);

    private final FieldService fieldService;
    private final DefaultOptionHandler defaultOptionHandler;

    public ContactOrgInitiator() {
        fieldService = FieldService.getInstance();
        defaultOptionHandler = DefaultOptionHandler.getInstance();
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
        LOGGER.info("Org Init Action: Contact initialization started. Org ID: {}, User ID: {}", orgId, userId);

        final Map<String, FieldInfo> orgFields = fieldService.
                groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, ContactFields.CONTACT_SOURCE,
                        ContactFields.SUBSCRIPTION_STATUS, ContactFields.SUBSCRIPTION_TYPES,
                        ContactFields.TIMEZONE, ContactFields.UNSUBSCRIBE_REASON,
                        ContactFields.CONTACT_LOST_REASON);

        for (final FieldInfo fieldInfo : orgFields.values()) {

            switch (fieldInfo.aliasName()) {
                case "contactSource" -> {
                    final Collection<DefaultOption> defaultOptions = new ArrayList<>();

                    defaultOptions.addAll(ContactSourceStaticOption.getOptions());
                    defaultOptions.addAll(ContactSource.getOptions());

                    defaultOptionHandler.create(orgId, fieldInfo.id(), defaultOptions);
                }
                case "subscriptionStatus" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Contact.SubscriptionStatus.getOptions());
                case "subscriptionTypes" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Contact.SubscriptionTypes.getOptions());
                case "unsubscribeReason" -> defaultOptionHandler.create(orgId, fieldInfo.id(), Contact.UnsubscribeReason.getOptions());
                case "contactLostReason" -> defaultOptionHandler.create(orgId, fieldInfo.id(), List.of(ContactLostReason.values()));
                case "timeZone" -> {
                    final Collection<DefaultOption> defaultOptions = new ArrayList<>();

                    for (final TimezoneInfo timezoneInfo : TimezoneInfo.values()) {
                        final DefaultOption defaultOption = new DefaultOption() {
                            @Override
                            public String getName() {
                                return timezoneInfo.getDisplayName();
                            }

                            @Override
                            public boolean isEditable() {
                                return false;
                            }

                            @Override
                            public boolean isDefault() {
                                return false;
                            }
                        };
                        defaultOptions.add(defaultOption);
                    }
                    defaultOptionHandler.create(orgId, fieldInfo.id(), defaultOptions);
                }
            }
        }
        LOGGER.info("Org Init Action: Contact initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
