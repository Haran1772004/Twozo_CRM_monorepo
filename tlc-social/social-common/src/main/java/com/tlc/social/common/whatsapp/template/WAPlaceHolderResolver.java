package com.tlc.social.common.whatsapp.template;

import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.notification.service.template.PlaceHolderResolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A resolver which resolves the whatsapp template placeholders
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAPlaceHolderResolver implements PlaceHolderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    private WAPlaceHolderResolver() {
    }

    /**
     * Holds the instance of {@link WAPlaceHolderResolver}
     */
    private static final class InstanceHolder {
        private static final WAPlaceHolderResolver WA_PLACE_HOLDER_RESOLVER = new WAPlaceHolderResolver();
    }

    /**
     * <p>
     * Gets the {@link WAPlaceHolderResolver} Instance
     * </p>
     *
     * @return {@link WAPlaceHolderResolver}
     */
    public static WAPlaceHolderResolver getInstance() {
        return InstanceHolder.WA_PLACE_HOLDER_RESOLVER;
    }

    /**
     * {@inheritDoc}
     *
     * @param content      Represents the data to be resolved
     * @param placeHolders Represents the {@link Collection} of {@link PlaceHolder}
     * @return the resolved string
     */
    @Override
    public String resolve(final String content, final Collection<PlaceHolder> placeHolders) {
        final Map<String, String> placeholderValueMap = new HashMap<>(placeHolders.size());

        for (final PlaceHolder placeHolder : placeHolders) {
            final Object value = placeHolder.getValue();

            if (Objects.nonNull(value)) {
                placeholderValueMap.put(placeHolder.getKey(), value.toString());
            }
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        final StringBuilder resolvedContent = new StringBuilder();

        while (matcher.find()) {
            final String value = placeholderValueMap.get(matcher.group());

            if (Objects.nonNull(value)) {
                matcher.appendReplacement(resolvedContent, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(resolvedContent);

        return resolvedContent.toString();
    }
}