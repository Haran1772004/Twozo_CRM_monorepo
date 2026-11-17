package com.tlc.web.action.res;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.util.StringUtil;
import com.tlc.web.WebExchange;
import com.tlc.web.fields.HeaderAttributes;
import com.tlc.web.secure.session.CSRFSession;
import com.tlc.web.status.WebErrorCodes;

import java.util.Locale;
import java.util.Objects;

/**
 * Provides the functionalities of html HTTP response holder
 *
 * @author sundar
 * @version 1.0
 */
public class HTMLResponse extends AbstractHttpResponse {

    private static final String CONTENT_TYPE = "text/html";

    private HtmlDocument htmlDocument;

    protected HTMLResponse(final Locale locale, final WebExchange webExchange) {
        super(locale, webExchange);
        addHeader(WebExchange.Header.CONTENT_TYPE.getName(), CONTENT_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String key, final Object value) {
        throw ErrorCode.get(WebErrorCodes.UNSUPPORTED_ACTION, "Unsupported response action");
    }

    /**
     * {@inheritDoc}
     */
    public void put(final HtmlDocument htmlDocument) {
        this.htmlDocument = Objects.requireNonNull(htmlDocument);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBytes() {

        if (Objects.nonNull(htmlDocument)) {
            return htmlDocument.html().getBytes();
        }
        return new byte[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void protectWithCSRFToken(final CSRFSession csrfSession) {
        if (Objects.nonNull(htmlDocument)) {
            final HtmlElements formElements = htmlDocument.getElementsByTag("form");

            formElements.getElements().stream().findFirst().ifPresent(form ->
                    form.appendElement("input")
                            .attribute("type", "hidden")
                            .attribute("id", "csrfToken")
                            .attribute("value", csrfSession.getToken()));
        }
    }

    /**
     * <p>
     * Enables support for embedding the application in an iframe by configuring
     * the necessary security headers
     * </p>
     */
    public void enableIFrameSupport() {
        addHeader(HeaderAttributes.CONTENT_SECURITY_POLICY, "frame-ancestors https:");
        addHeader(HeaderAttributes.X_FRAME_OPTIONS, StringUtil.EMPTY_STRING);
    }
}
