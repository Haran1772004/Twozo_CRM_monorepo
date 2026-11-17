package com.tlc.crm.messenger.api.channel.model;

import com.tlc.social.common.whatsapp.model.WABusinessAccount;

import java.util.Collection;

/**
 * A model which represents the crm Whatsapp business account
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CrmWABusinessAccount extends WABusinessAccount {

    private Long orgId;
    private Long crmBusinessAccountId;
    private Collection<CrmWAPhoneNumber> crmWAPhoneNumbers;

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getCrmBusinessAccountId() {
        return crmBusinessAccountId;
    }

    public void setCrmBusinessAccountId(final Long crmBusinessAccountId) {
        this.crmBusinessAccountId = crmBusinessAccountId;
    }

    public Collection<CrmWAPhoneNumber> getCrmWAPhoneNumbers() {
        return crmWAPhoneNumbers;
    }

    public void setCrmWAPhoneNumbers(final Collection<CrmWAPhoneNumber> crmWAPhoneNumbers) {
        this.crmWAPhoneNumbers = crmWAPhoneNumbers;
    }
}
