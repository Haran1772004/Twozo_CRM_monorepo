package com.tlc.crm.messenger.api.channel.model;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Pertains to Crm channel structure.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface CrmSocialBusinessChannel extends TlcModel {

    ChannelState getState();

    String getName();

    void setName(final String name);
}