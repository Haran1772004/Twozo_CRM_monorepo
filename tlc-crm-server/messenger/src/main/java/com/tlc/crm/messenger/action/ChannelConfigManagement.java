package com.tlc.crm.messenger.action;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;

import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.social.common.model.Platform;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;

import com.tlc.web.WebAction;

/**
 * Manages the social platform related config actions
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/messenger/{platform}/config/mgmt")
public class ChannelConfigManagement extends CrmPublicAction {

    private final Configurations configurations;

    public ChannelConfigManagement() {
        configurations = ConfHolder.getInstance().getProperties(MessengerFields.SYNC_SERVICE);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void pProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Platform platform = Platform.get(crmRequest.getParameter(MessengerFields.PLATFORM));

        switch (platform) {
            case WHATSAPP -> {
                final Configurations whatsAppConfigs = configurations.getNestedConfigs(MessengerFields.WHATSAPP);

                crmResponse.put(MessengerFields.APP_ID, whatsAppConfigs.getConfig(SyncServiceFields.APP_ID));
                crmResponse.put(MessengerFields.CONFIG_ID, whatsAppConfigs.getConfig(SyncServiceFields.CONFIG_ID));
            }
        }
    }
}