package com.tlc.iam.common.action;

import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;

/**
 * Processes the public requests
 *
 * @author Selvakumar
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
    public void process(final IamHttpRequest request, final IamHttpResponse response) throws Exception {
        pProcess(request, response);
    }

    /**
     * Public request handler
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception In case any operations failed
     */
    public abstract void pProcess(final IamHttpRequest request, final IamHttpResponse response) throws Exception;

}
