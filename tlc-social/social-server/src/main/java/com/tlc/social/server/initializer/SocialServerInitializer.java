package com.tlc.social.server.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.server.facebook.internal.webhook.controller.FBWebhookPayloadProcessor;
import com.tlc.social.server.facebook.internal.service.FBService;
import com.tlc.social.server.facebook.internal.service.FBWebhookService;
import com.tlc.social.server.instagram.internal.webhook.controller.InstagramWebhookDataController;
import com.tlc.social.server.instagram.internal.service.InstagramService;
import com.tlc.social.server.instagram.internal.service.InstagramWebhookService;
import com.tlc.social.server.whatsapp.internal.webhook.controller.WATemplateDataProcessService;
import com.tlc.social.server.service.SocialEventRequestService;
import com.tlc.social.server.service.internal.SocialEventRequestServiceImpl;
import com.tlc.social.server.whatsapp.internal.webhook.controller.WAWebhookDataProcessService;
import com.tlc.social.server.whatsapp.internal.service.WAService;
import com.tlc.social.server.whatsapp.internal.service.WAWebhookService;

/**
 * An initializer which initializes the social server
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SocialServerInitializer implements ServiceInitializer {

    private final SocialEventRequestService socialEventRequestService;

    private SocialServerInitializer() {
        socialEventRequestService = new SocialEventRequestServiceImpl();
    }

    private static final class InstanceHolder {
        private static final SocialServerInitializer SOCIAL_SERVER_INITIALIZER = new SocialServerInitializer();
    }

    /**
     * <p>
     * Gets the instance of {@link SocialServerInitializer}
     * </p>
     *
     * @return the instance of {@link SocialServerInitializer}
     */
    public static SocialServerInitializer getInstance() {
        return InstanceHolder.SOCIAL_SERVER_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        FBWebhookPayloadProcessor.getInstance().start(configurations);
        InstagramWebhookDataController.getInstance().start(configurations);
        WAWebhookDataProcessService.getInstance().start(configurations);
        WATemplateDataProcessService.getInstance().start(configurations);
        socialEventRequestService.start(configurations);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        final Configurations facebookConfigurations = configurations.getNestedConfigs(FBFields.FACEBOOK);
        final Configurations whatsappConfigurations = configurations.getNestedConfigs(WAFields.WHATSAPP);
        final Configurations instagramConfigurations = configurations.getNestedConfigs(FBFields.INSTAGRAM);

        FBService.getInstance().setProperties(facebookConfigurations);
        FBWebhookService.getInstance().setProperties(facebookConfigurations);
        WAService.getInstance().setProperties(whatsappConfigurations);
        WAWebhookService.getInstance().setProperties(whatsappConfigurations);
        InstagramService.getInstance().setProperties(instagramConfigurations);
        InstagramWebhookService.getInstance().setProperties(instagramConfigurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        FBWebhookPayloadProcessor.getInstance().stop();
        WAWebhookDataProcessService.getInstance().stop();
        WATemplateDataProcessService.getInstance().stop();
        InstagramWebhookDataController.getInstance().stop();
        socialEventRequestService.stop();
    }
}
