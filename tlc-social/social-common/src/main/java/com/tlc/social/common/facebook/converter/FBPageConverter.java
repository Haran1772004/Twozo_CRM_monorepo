package com.tlc.social.common.facebook.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.converter.SocialEntityConverter;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.facebook.model.FBPage;
import com.tlc.social.common.model.Channel;

/**
 * A converter which converts the facebook page related information
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBPageConverter implements SocialEntityConverter<FBPage> {

    private FBPageConverter() {
    }

    private static final class InstanceHolder {
        private static final FBPageConverter FACEBOOK_CONVERTER = new FBPageConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link FBPageConverter}
     * </p>
     *
     * @return the instance of {@link FBPageConverter}
     */
    public static FBPageConverter getInstance() {
        return InstanceHolder.FACEBOOK_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Represents the json data to be converted
     * @return the {@link FBPage}
     */
    @Override
    public FBPage convert(final JsonObject jsonObject) {
        final FBPage fbPage = new FBPage();
        final Channel channel = new Channel();
        final String pageName = jsonObject.getString(FBFields.NAME);

        fbPage.setExternalPageId(jsonObject.getLong(FBFields.ID));
        fbPage.setName(pageName);

        channel.setType(Channel.ChannelType.FB_PAGE);
        channel.setName(pageName);
        fbPage.setChannel(channel);

        return fbPage;
    }


    /**
     * {@inheritDoc}
     *
     * @param fbPage Represents the {@link FBPage}
     * @return the json data
     */
    @Override
    public JsonObject convert(final FBPage fbPage) {
        final JsonObject pageData = Json.object();

        pageData.put(FBFields.ID, fbPage.id());
        pageData.put(FBFields.NAME, fbPage.getName());

        return pageData;
    }
}
