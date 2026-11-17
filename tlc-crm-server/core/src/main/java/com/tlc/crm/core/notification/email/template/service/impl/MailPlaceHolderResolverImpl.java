package com.tlc.crm.core.notification.email.template.service.impl;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;
import com.tlc.crm.core.notification.email.template.service.MailPlaceHolderResolver;
import com.tlc.notification.model.template.PlaceHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * <p>
 * A class which handles the placeholder resolving functionality for email
 * </p>
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
public class MailPlaceHolderResolverImpl implements MailPlaceHolderResolver {

    private static final String INPUT_PLACEHOLDER_TAG = "input[type]";
    private static final String EMPTY_SPACE = "       ";

    private final HtmlParser htmlParser;
    private MailPlaceHolderExtractor placeHolderExtractor;

    private MailPlaceHolderResolverImpl() {
        htmlParser = HtmlParser.getInstance();
        placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
    }

    private static class Instance {
        private static final MailPlaceHolderResolverImpl INSTANCE = new MailPlaceHolderResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link MailPlaceHolderResolverImpl} Instance
     * </p>
     *
     * @return {@link MailPlaceHolderResolverImpl}
     */
    public static MailPlaceHolderResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the content with {@link PlaceHolder} values provided
     * </p>
     *
     * @param content      Refers to the content to be resolved
     * @param placeHolders Represents the {@link Collection} of {@link PlaceHolder}
     * @return {@link String} resolved subject content
     */
    @Override
    public String resolve(final String content, final Collection<PlaceHolder> placeHolders) {
        final Map<String, String> placeholderValue = new HashMap<>(placeHolders.size());

        for (final PlaceHolder placeHolder : placeHolders) {
            final Object value = placeHolder.getValue();

            placeholderValue.put(placeHolder.getKey(), Objects.nonNull(value) ? value.toString() : EMPTY_SPACE);
        }
        final Matcher matcher = placeHolderExtractor.getPattern().matcher(content);
        final StringBuilder resolvedSubject = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(resolvedSubject, placeholderValue.get(matcher.group(NumericalConstant.ONE)));
        }
        matcher.appendTail(resolvedSubject);

        return resolvedSubject.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @param content      Refers to the html content with input tags to be resolved
     * @param placeHolders Represents the {@link Collection} of {@link PlaceHolder}
     * @return {@link String} resolved content
     */
    @Override
    public String resolveWithInputTags(final String content, final Collection<PlaceHolder> placeHolders) {
        final HtmlDocument document = htmlParser.parse(content);

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

            for (final PlaceHolder placeHolder : placeHolders) {
                updateContentWithPlaceHolder(document, (MailPlaceHolder) placeHolder);
            }

            return document.html();
        } else {
            final HtmlElements inputElements = document.select(INPUT_PLACEHOLDER_TAG);

            inputElements.remove();

            return document.text();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param content Refers to the content for which tags are to be removed
     * @return {@link String} resolved content
     */
    @Override
    public String removePlaceHolderTag(final String content) {
        final HtmlDocument document = htmlParser.parse(content);
        final HtmlElements htmlElements = document.select(INPUT_PLACEHOLDER_TAG);

        for (final HtmlElement element : htmlElements.getElements()) {
            element.unwrap();
        }

        return document.html();
    }

    /**
     * <p>
     * Updates the content with placeholder values.
     * </p>
     *
     * @param document    Represents the HTML document
     * @param placeHolder Represents the {@link MailPlaceHolder}
     */
    private void updateContentWithPlaceHolder(final HtmlDocument document, final MailPlaceHolder placeHolder) {
        final Object value = placeHolder.getValue();
        final HtmlElements inputElements = document.select(String.format(placeHolder.pattern(), placeHolder.getKey()));

        for (final HtmlElement inputElement : inputElements.getElements()) {

            if (Objects.isNull(value)) {
                inputElement.remove();
            } else {
                inputElement.text(value.toString());
                inputElement.unwrap();
            }
        }
    }
}