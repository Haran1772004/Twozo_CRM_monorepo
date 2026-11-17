package com.tlc.social.client.service.instagram;

import com.tlc.social.client.service.PageService;
import com.tlc.social.client.service.instagram.internal.InstagramPageServiceImpl;
import com.tlc.social.common.instagram.model.InstagramPage;

/**
 * <p>
 * Facilitates services related to Instagram page.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface InstagramPageService extends PageService<InstagramPage> {

    /**
     * <p>
     * Gets {@link InstagramPageService}.
     * </p>
     *
     * @return {@link InstagramPageService}.
     */
    static InstagramPageService getInstance() {
        return InstagramPageServiceImpl.getInstance();
    }
}