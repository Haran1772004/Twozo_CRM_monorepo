package com.tlc.crm.messenger.api.channel.model;

import com.tlc.social.common.facebook.model.FBPage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * Facebook page fields pertaining to CRM.
 * </p>
 *
 * @author Maharaja.
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class CrmFBPage extends FBPage implements CrmSocialBusinessChannel {

    private Long orgId;
    private Long crmPageId;
    private ChannelState state;
}