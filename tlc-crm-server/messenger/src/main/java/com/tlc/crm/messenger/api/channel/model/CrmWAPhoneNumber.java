package com.tlc.crm.messenger.api.channel.model;

import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;

/**
 * A model which represents the crm WhatsApp business phone number
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CrmWAPhoneNumber extends WABusinessPhoneNumber implements CrmSocialBusinessChannel {

    private Long orgId;
    private Long crmPhoneNumberId;
    private Long crmWABusinessAccountId;
    private ChannelState state;

    public Long getCrmWABusinessAccountId() {
        return crmWABusinessAccountId;
    }

    public void setCrmWABusinessAccountId(final Long crmWABusinessAccountId) {
        this.crmWABusinessAccountId = crmWABusinessAccountId;
    }

    public Long getCrmPhoneNumberId() {
        return crmPhoneNumberId;
    }

    public void setCrmPhoneNumberId(final Long crmPhoneNumberId) {
        this.crmPhoneNumberId = crmPhoneNumberId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public ChannelState getState() {
        return state;
    }

    public void setState(final ChannelState state) {
        this.state = state;
    }

    /**
     * {@inheritDoc}
     *
     * @return the display name
     */
    @Override
    public String getName() {
        return super.getDisplayPhoneNumber();
    }

    /**
     * {@inheritDoc}
     *
     * @param name Represents the display name
     */
    @Override
    public void setName(final String name) {
        super.setDisplayPhoneNumber(name);
    }
}
