package com.tlc.social.client.service.facebook;

import com.tlc.social.client.service.PageService;
import com.tlc.social.client.service.facebook.internal.FBPageServiceImpl;
import com.tlc.social.common.facebook.model.FBPage;

/**
 * Provides the client services related to the facebook page
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface FBPageService extends PageService<FBPage> {

    /**
     * <p>
     * Gets the instance of {@link FBPageService}
     * </p>
     *
     * @return the instance of {@link FBPageService}
     */
    static FBPageService getInstance() {
        return FBPageServiceImpl.getInstance();
    }
}
