package com.tlc.file.client.action;

import com.tlc.file.common.action.PublicAction;
import com.tlc.file.common.action.req.FileRequest;
import com.tlc.file.common.action.res.FileResponse;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;

/**
 * A public action that provides a health check endpoint for the file service.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see PublicAction
 */
@WebAction(path = "/healthcheck", method = HttpMethod.GET)
public class HealthCheckAction extends PublicAction {

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the {@link FileRequest}.
     * @param response Refers the {@link FileResponse}.
     */
    @Override
    public void pProcess(final FileRequest request, final FileResponse response) throws Exception {
        response.setStatus(HttpStatus.OK);
        response.send();
    }
}
