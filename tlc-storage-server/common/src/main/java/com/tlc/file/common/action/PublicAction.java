package com.tlc.file.common.action;

import com.tlc.file.common.action.req.FileRequest;
import com.tlc.file.common.action.res.FileResponse;

/**
 * Processes the public requests.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class PublicAction extends WebActionProcessor {

    /**
     * Does the base level operations before processing the public requests
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception In case any operations failed
     */
    @Override
    public void process(final FileRequest request, final FileResponse response) throws Exception {
        pProcess(request, response);
    }

    /**
     * Public request handler
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception In case any operations failed
     */
    public abstract void pProcess(final FileRequest request, final FileResponse response) throws Exception;

}
