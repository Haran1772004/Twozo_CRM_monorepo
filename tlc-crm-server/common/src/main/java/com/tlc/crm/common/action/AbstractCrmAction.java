package com.tlc.crm.common.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.req.CrmJsonRequest;
import com.tlc.crm.common.action.req.CrmMultipartRequest;
import com.tlc.crm.common.action.req.CrmFormUrlEncodedRequest;
import com.tlc.crm.common.action.res.CrmJsonResponse;
import com.tlc.crm.common.action.res.CrmStreamResponse;
import com.tlc.crm.common.action.res.CrmXmlResponse;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebExchange;
import com.tlc.web.WebExchange.ContentType;
import com.tlc.web.WebExchange.Header;
import com.tlc.web.action.AbstractWebAction;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>
 * Abstract base class for all CRM web actions.
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public abstract class AbstractCrmAction extends AbstractWebAction<CrmRequest, CrmResponse> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected CrmRequest buildRequest(final WebExchange exchange, final byte[] body) {
        final Supplier<CrmJsonRequest> crmJsonRequestCreator = () -> new CrmJsonRequest(exchange, body);
        final String contentType = exchange.header(Header.CONTENT_TYPE.getName()).findAny().orElse(null);

        if (Objects.isNull(contentType)) {
            return crmJsonRequestCreator.get();
        }

        if (contentType.startsWith(ContentType.MULTIPART_CONTENT.getName())) {
            return new CrmMultipartRequest(exchange);
        } else if (contentType.contains(ContentType.URL_ENCODED_CONTENT.getName())) {
            return new CrmFormUrlEncodedRequest(exchange, body);
        }

        return crmJsonRequestCreator.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CrmResponse buildResponse(final WebExchange exchange) {
        final String accept = exchange.header(Header.ACCEPT.getName()).findAny().orElse(null);
        final Locale locale = exchange.getLocale();

        if (Objects.nonNull(accept)) {
            final boolean isImageType = accept.contains("image/apng") || accept.contains("image/png") || accept.contains("image/jpg")
                    || accept.contains("image/jpeg") || accept.contains("application/octet-stream");

            if (isImageType) {
                return new CrmStreamResponse(locale, exchange);
            }
        }
        final boolean isXmlType = exchange.header(Header.CONTENT_TYPE.getName())
                .anyMatch(contentType -> Objects.equals("application/xml", contentType));

        if (isXmlType) {
            return new CrmXmlResponse(locale, exchange);
        }

        return new CrmJsonResponse(locale, exchange);
    }

    /**
     * <p>
     * Sets {@link ErrorDescriptor}.
     * </p>
     *
     * @param crmResponse {@link CrmResponse}.
     * @param errorCode   {@link ErrorCode}.
     * @param detail      Represents the more detailed explanation of the error, often useful for debugging.
     * @param target      Represents the specific target associated with the error.
     */
    protected void setErrorDescriptor(final CrmResponse crmResponse, final ErrorCode errorCode, final String detail,
                                      final String target) {
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, detail, target)));
    }
}