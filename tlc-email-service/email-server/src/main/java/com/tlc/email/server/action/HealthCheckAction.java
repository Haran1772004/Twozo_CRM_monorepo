package com.tlc.email.server.action;

import com.tlc.email.common.action.PublicAction;
import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;

/**
 * A public action that provides a health check endpoint for the file service.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see PublicAction
 */
@WebAction(path = "/healthcheck", method = HttpMethod.GET)
public class HealthCheckAction extends PublicAction {

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the {@link EmailRequest}.
     * @param response Refers the {@link EmailResponse}.
     */
    @Override
    public void pProcess(final EmailRequest request, final EmailResponse response) throws Exception {
        response.setStatus(HttpStatus.OK);
        response.send();
    }
}
