package com.tlc.crm.messenger.api.channel.model;

import com.tlc.social.common.instagram.model.InstagramPage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * Instagram page fields pertaining to CRM.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class CrmInstagramPage extends InstagramPage implements CrmSocialBusinessChannel {

    private Long orgId;
    private Long crmPageId;
    private ChannelState state;
}