package com.tlc.iam.common.action;

import com.tlc.iam.common.action.req.IamHttpJsonRequest;
import com.tlc.iam.common.action.req.IamHttpMultiPartRequest;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpJsonResponse;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.action.res.IamHttpStreamResponse;
import com.tlc.web.WebExchange;
import com.tlc.web.action.AbstractWebAction;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import static com.tlc.web.WebExchange.Header.CONTENT_TYPE;

/**
 * <p>
 * Abstract base class for all IAM web actions.
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class WebActionProcessor extends AbstractWebAction<IamHttpRequest, IamHttpResponse> {

    /**
     * <p>
     * Processes the incoming request.
     * </p>
     *
     * @param request  Http request.
     * @param response Http response.
     * @throws Exception In case any error occurs.
     */
    public abstract void process(final IamHttpRequest request, final IamHttpResponse response) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected IamHttpRequest buildRequest(final WebExchange exchange, final byte[] body) {
        final Stream<String> header = exchange.header(CONTENT_TYPE.getName());
        final String contentType = Objects.nonNull(header) ? header.findAny().orElse(null) : null;

        if (Objects.nonNull(contentType) && contentType.startsWith(WebExchange.ContentType.MULTIPART_CONTENT.getName())) {
            return new IamHttpMultiPartRequest(exchange);
        }

        return new IamHttpJsonRequest(exchange, body);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IamHttpResponse buildResponse(final WebExchange exchange) {
        final String accept = exchange.header(WebExchange.Header.ACCEPT.getName()).findAny().orElse(null);
        final Locale locale = exchange.getLocale();

        if (Objects.nonNull(accept)) {
            final boolean isImageType = accept.contains(WebExchange.ContentType.IMAGE_PNG.getName())
                    || accept.contains(WebExchange.ContentType.IMAGE_JPEG.getName())
                    || accept.contains(WebExchange.ContentType.IMAGE_JPG.getName());

            if (isImageType) {
                return new IamHttpStreamResponse(locale, exchange);
            }
        }

        return new IamHttpJsonResponse(locale, exchange);
    }
}