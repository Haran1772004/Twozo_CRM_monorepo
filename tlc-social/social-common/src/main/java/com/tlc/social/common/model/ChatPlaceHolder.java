package com.tlc.social.common.model;

import com.tlc.notification.model.template.PlaceHolder;

/**
 * A model which represents the chat related placeholder
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ChatPlaceHolder extends PlaceHolder {

    public ChatPlaceHolder() {
    }

    public ChatPlaceHolder(final String key, final Object value) {
        super.setKey(key);
        super.setValue(value);
    }
}