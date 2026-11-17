package com.tlc.file.common.action;

import com.tlc.file.common.action.req.FileRequest;
import com.tlc.file.common.action.req.JsonFileRequest;
import com.tlc.file.common.action.req.FileMultiPartRequest;
import com.tlc.file.common.action.res.FileResponse;
import com.tlc.file.common.action.res.FileJsonResponse;
import com.tlc.file.common.action.res.FileStreamResponse;

import com.tlc.web.WebExchange;
import com.tlc.web.action.AbstractWebAction;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * Abstract base class for all File web actions.
 * </p>
 *
 * @author ThalaimalaiPandiyanT.
 * @version 1.0.
 */
public abstract class WebActionProcessor extends AbstractWebAction<FileRequest, FileResponse> {

    /**
     * <p>
     * Processes the incoming request.
     * </p>
     *
     * @param request  Http request.
     * @param response Http response.
     * @throws Exception In case any error occurs.
     */
    public abstract void process(final FileRequest request, final FileResponse response) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected FileRequest buildRequest(final WebExchange exchange, final byte[] body) {
        final Stream<String> header = exchange.header(WebExchange.Header.CONTENT_TYPE.getName());
        final String contentType = Objects.nonNull(header) ? header.findAny().orElse(null) : null;

        if (Objects.nonNull(contentType) && contentType.startsWith(WebExchange.ContentType.MULTIPART_CONTENT.getName())) {
            return new FileMultiPartRequest(exchange);
        }

        return new JsonFileRequest(exchange, body);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FileResponse buildResponse(final WebExchange exchange) {
        final String accept = exchange.header(WebExchange.Header.ACCEPT.getName()).findAny().orElse(null);
        final Locale locale = exchange.getLocale();

        if (Objects.nonNull(accept)) {
            final boolean isImageType = accept.contains(WebExchange.ContentType.IMAGE_PNG.getName())
                    || accept.contains(WebExchange.ContentType.IMAGE_JPEG.getName())
                    || accept.contains(WebExchange.ContentType.IMAGE_JPG.getName());

            if (isImageType) {
                return new FileStreamResponse(locale, exchange);
            }
        }

        return new FileJsonResponse(locale, exchange);
    }
}
