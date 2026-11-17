package com.tlc.social.common.instagram.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.converter.SocialEntityConverter;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.instagram.model.InstagramPage;
import com.tlc.social.common.model.Channel;

/**
 * <p>
 * Converts instagram page related details.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramPageConverter implements SocialEntityConverter<InstagramPage> {

    private InstagramPageConverter() {
    }

    private static final class InstanceHolder {
        private static final InstagramPageConverter INSTAGRAM_PAGE_CONVERTER = new InstagramPageConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramPageConverter}
     * </p>
     *
     * @return the instance of {@link InstagramPageConverter}
     */
    public static InstagramPageConverter getInstance() {
        return InstanceHolder.INSTAGRAM_PAGE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Represents the json data to be converted.
     * @return {@link InstagramPage}.
     */
    @Override
    public InstagramPage convert(final JsonObject jsonObject) {
        final InstagramPage instagramPage = new InstagramPage();
        final Channel channel = new Channel();
        final String pageName = jsonObject.getString(FBFields.NAME);

        instagramPage.setExternalPageId(jsonObject.getLong(FBFields.ID));
        instagramPage.setName(pageName);

        channel.setType(Channel.ChannelType.INSTAGRAM_PAGE);
        channel.setName(pageName);
        instagramPage.setChannel(channel);

        return instagramPage;
    }


    /**
     * {@inheritDoc}
     *
     * @param instagramPage {@link InstagramPage}.
     * @return converted data.
     */
    @Override
    public JsonObject convert(final InstagramPage instagramPage) {
        final JsonObject pageData = Json.object();

        pageData.put(FBFields.ID, instagramPage.id());
        pageData.put(FBFields.NAME, instagramPage.getName());

        return pageData;
    }
}
