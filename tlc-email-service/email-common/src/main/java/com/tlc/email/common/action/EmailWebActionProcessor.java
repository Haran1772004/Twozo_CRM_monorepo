package com.tlc.email.common.action;

import com.tlc.email.common.action.req.EmailJsonRequest;
import com.tlc.email.common.action.req.EmailMultiPartRequest;
import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailJsonResponse;
import com.tlc.email.common.action.res.EmailResponse;
import com.tlc.email.common.action.res.EmailStreamResponse;

import com.tlc.web.WebExchange;
import com.tlc.web.action.AbstractWebAction;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Abstract base class for all Email web actions.
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public abstract class EmailWebActionProcessor extends AbstractWebAction<EmailRequest, EmailResponse> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected EmailRequest buildRequest(final WebExchange exchange, final byte[] body) {
        final Stream<String> header = exchange.header(WebExchange.Header.CONTENT_TYPE.getName());
        final String contentType = Objects.nonNull(header) ? header.findAny().orElse(null) : null;

        if (Objects.nonNull(contentType) && contentType.startsWith(WebExchange.ContentType.MULTIPART_CONTENT.getName())) {
            return new EmailMultiPartRequest(exchange);
        }

        return new EmailJsonRequest(exchange, body);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EmailResponse buildResponse(final WebExchange exchange) {
        final String accept = exchange.header(WebExchange.Header.ACCEPT.getName()).findAny().orElse(null);
        final Locale locale = exchange.getLocale();

        if (Objects.nonNull(accept)) {
            final boolean isImageType = accept.contains(WebExchange.ContentType.IMAGE_PNG.getName())
                    || accept.contains(WebExchange.ContentType.IMAGE_JPEG.getName())
                    || accept.contains(WebExchange.ContentType.IMAGE_JPG.getName());

            if (isImageType) {
                return new EmailStreamResponse(locale, exchange);
            }
        }

        return new EmailJsonResponse(locale, exchange);
    }
}