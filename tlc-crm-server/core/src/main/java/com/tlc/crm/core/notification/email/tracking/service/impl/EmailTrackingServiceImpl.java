package com.tlc.crm.core.notification.email.tracking.service.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.parser.html.HtmlTagFields;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.tracking.fields.EmailTrackingFields;
import com.tlc.crm.core.notification.email.tracking.service.EmailTrackingService;

import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.web.HttpUtil;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Sets the email tracking related details into {@link EntityEmail}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class EmailTrackingServiceImpl implements EmailTrackingService {

    private static final String TRACKING_REDIRECT_URL_KEY = "server.redirectUrl.tracking";
    private static final String UNSUBSCRIBE_REDIRECT_URL_KEY = "server.redirectUrl.unsubscribe";
    private static final String TRACKER_PATTERN = "\\btracking\\b";

    private final RandomIdGenerator randomIdGenerator;
    private final HtmlParser htmlParser;
    private final Configurations configurations;

    private EmailTrackingServiceImpl() {
        randomIdGenerator = RandomIdGenerator.getInstance();
        htmlParser = HtmlParser.getInstance();
        configurations = ConfHolder.getInstance().getProperties(EmailTrackingFields.SYNC_SERVICE.getName());
    }

    private static class Instance {
        private static final EmailTrackingServiceImpl INSTANCE = new EmailTrackingServiceImpl();
    }

    /**
     * Gets instance of {@link EmailTrackingServiceImpl}
     *
     * @return The {@link EmailTrackingServiceImpl} instance
     */
    public static EmailTrackingServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param entityEmail Represents the mail details
     * @param requestData Represent the request json
     */
    @Override
    public void setTrackingDetails(final EntityEmail entityEmail, final JsonObject requestData) {
        final EntityOutboundEmail outboundEmail = (EntityOutboundEmail) entityEmail;
        final EmailTrackerSettings emailTrackerSettings = outboundEmail.getEmailTrackerSettings();
        final EmailRecipients emailRecipients = outboundEmail.getEmailRecipients();
        final Collection<EmailAddress> receivers = emailRecipients.toRecipients();
        final String content = outboundEmail.getContent();

        if (Objects.nonNull(content)) {
            final HtmlDocument htmlDocument = htmlParser.parse(content);
            final String url = HttpUtil.buildAbsoluteUrl(configurations.getConfig(TRACKING_REDIRECT_URL_KEY));
            final JsonArray emailTrackingDetailsArray = Json.array();

            if (emailTrackerSettings.isLinkTrackerEnabled()) {
                final HtmlElements htmlElements = htmlDocument.getElementsByTag("a");

                if (!htmlElements.isEmpty()) {
                    final Collection<HtmlElement> elements = htmlElements.getElements();

                    for (final HtmlElement htmlElement : elements) {
                        final EmailTrackingDetail emailTrackingDetail = new EmailTrackingDetail();
                        final JsonObject emailTrackingJson = Json.object();
                        final EmailTrackingDetail.Type clicked = EmailTrackingDetail.Type.CLICKED;
                        final String trackingId = randomIdGenerator.generateUniqueId().replace("-", "_");

                        htmlElement.attribute("href", String.format("%s%s%s%s%s%s", url, "/click/", trackingId,
                                "/tracker.png", "?source=twozo&link=", htmlElement.attribute("href")));
                        htmlElement.attribute("class", "tracking");
                        emailTrackingDetail.setTrackingId(trackingId);
                        emailTrackingDetail.setType(clicked);
                        emailTrackingJson.put(EmailTrackingFields.TRACKER_ID.getName(), trackingId);
                        emailTrackingJson.put(EmailTrackingFields.TRACKER_TYPE.getName(), clicked.id);
                        outboundEmail.addEmailTrackingDetail(emailTrackingDetail);
                        emailTrackingDetailsArray.put(emailTrackingJson);
                    }
                }
            }

            if (emailTrackerSettings.isEmailTrackerEnabled()) {
                final EmailTrackingDetail emailTrackingDetail = new EmailTrackingDetail();
                final JsonObject emailTrackingJson = Json.object();
                final EmailTrackingDetail.Type opened = EmailTrackingDetail.Type.OPENED;
                final String openedTrackingId = randomIdGenerator.generateUniqueId().replace("-", "_");
                final HtmlElement htmlElement = htmlDocument.appendElement("img");

                htmlElement.attribute("src", String.format("%s%s%s%s", url, "/open/", openedTrackingId,
                        "/tracker.png"));
                htmlElement.attribute("id", "trackingPixel");
                htmlElement.attribute("width", "1");
                htmlElement.attribute("height", "1");
                htmlElement.attribute("border", "0");
                htmlElement.attribute("alt", "");
                htmlElement.attribute("style", "display:block;width:1px;height:1px;border:0;");

                emailTrackingDetail.setTrackingId(openedTrackingId);
                emailTrackingDetail.setType(opened);
                emailTrackingJson.put(EmailTrackingFields.TRACKER_ID.getName(), openedTrackingId);
                emailTrackingJson.put(EmailTrackingFields.TRACKER_TYPE.getName(), opened.id);
                outboundEmail.addEmailTrackingDetail(emailTrackingDetail);
                emailTrackingDetailsArray.put(emailTrackingJson);
            }

            if (outboundEmail.hasUnsubscribeLink()) {
                final EmailTrackingDetail emailTrackingDetail = new EmailTrackingDetail();
                final JsonObject emailTrackingJson = Json.object();
                final EmailTrackingDetail.Type unsubscribed = EmailTrackingDetail.Type.UNSUBSCRIBED;
                final String trackingId = randomIdGenerator.generateUniqueId().replace("-", "_");

                htmlDocument.appendElement("br br");
                final HtmlElement htmlElement = htmlDocument.appendElement("a");

                htmlElement.attribute("href", String.format("%s%s%s%s%s%s", url, "/unsubscribe/", trackingId,
                        "/tracker.png", "?source=twozo&link=", HttpUtil.buildAbsoluteUrl(configurations.getConfig(UNSUBSCRIBE_REDIRECT_URL_KEY))));
                htmlElement.text("Unsubscribe");

                emailTrackingDetail.setTrackingId(trackingId);
                emailTrackingDetail.setType(unsubscribed);
                emailTrackingJson.put(EmailTrackingFields.TRACKER_ID.getName(), trackingId);
                emailTrackingJson.put(EmailTrackingFields.TRACKER_TYPE.getName(), unsubscribed.id);
                outboundEmail.addEmailTrackingDetail(emailTrackingDetail);
                emailTrackingDetailsArray.put(emailTrackingJson);
            }
            requestData.put(EmailTrackingFields.EMAIL_TRACKING.getName(), emailTrackingDetailsArray);
            outboundEmail.setContent(htmlDocument.html());
            emailRecipients.addRecipients(AddressType.TO, receivers);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param mail Represents the email details
     */
    @Override
    public void removeTrackingDetails(final EntityEmail mail) {
        final EmailTrackerSettings emailTrackerSettings = mail.getEmailTrackerSettings();
        final String content = mail.getContent();

        if (Objects.nonNull(content)) {
            final HtmlDocument htmlDocument = htmlParser.parse(content);

            if (emailTrackerSettings.isEmailTrackerEnabled()) {
                removeOpenTracker(htmlDocument);
            }

            if (emailTrackerSettings.isLinkTrackerEnabled()) {
                removeClickTrackers(htmlDocument);
            }

            mail.setContent(htmlDocument.html());
        }
    }

    /**
     * <p>
     * Removes tracking anchors and tracking pixel images from the provided HTML content.
     * This targets the tracker endpoints used by the system (e.g. /api/notifications/email/tracker)
     * and class/id based tracking wrappers.
     * </p>
     *
     * @param content raw HTML content
     * @return cleaned HTML content with tracking anchors/images removed (returns original if null)
     */
    @Override
    public String removeTrackingDetails(final String content) {

        if (Objects.isNull(content) || content.isBlank()) {
            return content;
        }
        final HtmlDocument htmlDocument = htmlParser.parse(content);

        removeOpenTracker(htmlDocument);
        removeClickTrackers(htmlDocument);

        return htmlDocument.html();
    }

    /**
     * <p>
     * Removes the open tracker from the HTML content
     * </p>
     *
     * @param htmlDocument Represents the HTML content of the email
     */
    private void removeOpenTracker(final HtmlDocument htmlDocument) {
        final String selector = String.format("#%s", EmailTrackingFields.TRACKING_PIXEL.getName());
        final HtmlElements pixelElements = htmlDocument.select(selector);

        if (Objects.nonNull(pixelElements) && !pixelElements.isEmpty()) {
            pixelElements.remove();
        }
    }


    /**
     * <p>
     * Removes the click tracker from the HTML content
     * </p>
     *
     * @param htmlDocument Represents the HTML content of the email
     */
    private void removeClickTrackers(final HtmlDocument htmlDocument) {
        final HtmlElements htmlElements = htmlDocument.getElementsByTag(HtmlTagFields.ANCHOR_TAG);

        if (!htmlElements.isEmpty()) {

            for (final HtmlElement htmlElement : htmlElements.getElements()) {
                final String href = htmlElement.attribute(HtmlTagFields.HREF);

                if (href == null || href.isBlank()) {
                    continue;
                }
                final int index = href.indexOf(HtmlTagFields.LINK);

                if (index == NumericalConstant.NEGATIVE_ONE) {
                    final String classAttribute = htmlElement.attribute(HtmlTagFields.CLASS_ATTRIBUTE);

                    if (Objects.nonNull(classAttribute) && !classAttribute.isBlank()
                            && classAttribute.contains(EmailTrackingFields.TRACKING.getName())) {
                        htmlElement.attribute(HtmlTagFields.CLASS_ATTRIBUTE,
                                classAttribute.replaceAll(TRACKER_PATTERN, StringUtil.EMPTY_STRING).trim());
                    }
                    continue;
                }
                String originalHref = href.substring(index + HtmlTagFields.LINK.length());

                try {
                    originalHref = URLDecoder.decode(originalHref, StandardCharsets.UTF_8);
                } catch (final Exception ignored) {

                }
                htmlElement.attribute(HtmlTagFields.HREF, originalHref);
                final String classAttribute = htmlElement.attribute(HtmlTagFields.CLASS_ATTRIBUTE);

                if (Objects.nonNull(classAttribute) && !classAttribute.isBlank()) {
                    htmlElement.attribute(HtmlTagFields.CLASS_ATTRIBUTE,
                            classAttribute.replaceAll(TRACKER_PATTERN, StringUtil.EMPTY_STRING).trim());
                }
            }
        }
    }
}