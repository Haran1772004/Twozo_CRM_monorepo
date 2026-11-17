package com.tlc.social.common.whatsapp.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.converter.SocialEntityConverter;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;

import java.util.Objects;

/**
 * A converter which converts the WhatsApp business phone number related information
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WABusinessPhoneNumberConverter implements SocialEntityConverter<WABusinessPhoneNumber> {

    private WABusinessPhoneNumberConverter() {
    }

    /**
     * Holds the instance of {@link WABusinessPhoneNumberConverter}
     */
    private static final class InstanceHolder {
        private static final WABusinessPhoneNumberConverter WA_BUSINESS_PHONE_NUMBER_CONVERTER = new WABusinessPhoneNumberConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WABusinessPhoneNumberConverter}
     * </p>
     *
     * @return the instance of {@link WABusinessPhoneNumberConverter}
     */
    public static WABusinessPhoneNumberConverter getInstance() {
        return InstanceHolder.WA_BUSINESS_PHONE_NUMBER_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the json data to be converted
     * @return the {@link WABusinessPhoneNumber}
     */
    @Override
    public WABusinessPhoneNumber convert(final JsonObject data) {
        final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();
        final String displayPhoneNumber = data.getString(WAFields.DISPLAY_PHONE_NUMBER);
        final Channel channel = new Channel();

        channel.setType(Channel.ChannelType.WHATSAPP_BUSINESS);
        channel.setName(displayPhoneNumber);
        waBusinessPhoneNumber.setExternalPhoneNumberId(data.getLong(WAFields.ID));
        waBusinessPhoneNumber.setDisplayPhoneNumber(displayPhoneNumber);
        waBusinessPhoneNumber.setChannel(channel);

        return waBusinessPhoneNumber;
    }

    /**
     * {@inheritDoc}
     *
     * @param waBusinessPhoneNumber Represents the {@link WABusinessPhoneNumber}
     * @return the converted json
     */
    @Override
    public JsonObject convert(final WABusinessPhoneNumber waBusinessPhoneNumber) {
        final JsonObject phoneNumberData = Json.object();
        final Long templateCount = waBusinessPhoneNumber.getTemplateCount();

        phoneNumberData.put(WAFields.ID, waBusinessPhoneNumber.id());
        phoneNumberData.put(WAFields.DISPLAY_NUMBER, waBusinessPhoneNumber.getDisplayPhoneNumber());
        phoneNumberData.put(WAFields.IS_REGISTERED, waBusinessPhoneNumber.isRegistered());

        if (Objects.nonNull(templateCount)) {
            phoneNumberData.put(WAFields.TEMPLATE_COUNT, templateCount);
        }

        return phoneNumberData;
    }
}