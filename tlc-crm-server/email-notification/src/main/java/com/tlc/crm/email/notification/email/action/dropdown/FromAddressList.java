package com.tlc.crm.email.notification.email.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.email.fields.EmailFields;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;

import java.util.Collection;

/**
 * <p>
 * Represents the dropdown list of {@link FromAddressInfo} options in an asynchronous manner
 * and provides methods to asynchronously handle actions related to the dropdown list.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
@AsyncDropdown(name = "fromAddressList")
public class FromAddressList implements AsyncDropdownAction {

    private final MailManager mailManager;

    public FromAddressList() {
        this.mailManager = MailManager.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </P>
     *
     * @param dropdownRequest Represents the {@link DropdownRequest}
     * @param crmResponse     Represents the {@link CrmResponse}
     * @return the {@link OptionContainer}
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final long orgId = dropdownRequest.getOrgId();
        final long userId = dropdownRequest.getUserId();
        final String emailAddressKey = EmailFields.EMAIL_ADDRESS.getName();
        final String fromAddressesKey = EmailFields.FROM_ADDRESSES.getName();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Collection<FromAddressInfo> fromAddresses = mailManager.getFromAddresses(orgId,
                userId, dropdownLimit.start(), dropdownLimit.limit(), dropdownRequest.getCriteria()
        );
        final String idKey = EmailFields.ID.getName();
        final String typeKey = EmailFields.TYPE.getName();
        final String verifiedKey = EmailFields.IS_VERIFIED.getName();
        final OptionContainer optionContainer = new OptionContainer();

        for (final FromAddressInfo fromAddress : fromAddresses) {
            final JsonObject fromJson = Json.object();

            fromJson.put(emailAddressKey, fromAddress.getEmailAddress());
            fromJson.put(idKey, fromAddress.id());
            fromJson.put(typeKey, crmResponse.resolveI18nString(fromAddress.getType().getI18nKey()));
            fromJson.put(verifiedKey, fromAddress.isVerified());

            optionContainer.addOption(fromJson);
        }
        crmResponse.put(fromAddressesKey, optionContainer.getOptions());
        crmResponse.send();
    }
}
