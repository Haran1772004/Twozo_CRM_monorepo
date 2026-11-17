package com.tlc.email.common.action;

import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;

/**
 * Processes the email related public request.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EmailWebActionProcessor
 */
public abstract class PublicAction extends EmailWebActionProcessor {

    /**
     * Does the base level operations before processing the public requests
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception In case any operations failed
     */
    @Override
    public void process(final EmailRequest request, final EmailResponse response) throws Exception {
        pProcess(request, response);
    }

    /**
     * Public request handler
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception In case any operations failed
     */
    public abstract void pProcess(final EmailRequest request, final EmailResponse response) throws Exception;
}

