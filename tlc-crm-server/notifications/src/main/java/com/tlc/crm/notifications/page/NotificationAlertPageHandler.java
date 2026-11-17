package com.tlc.crm.notifications.page;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.service.NotificationAlertManager;

/**
 * <p>
 * Page handler for the {@link NotificationAlert}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class NotificationAlertPageHandler extends PageHandler<NotificationAlert> {

    private final Long orgId;
    private final Long userId;
    private final long time;
    private final NotificationAlertManager notificationAlertManager;

    public NotificationAlertPageHandler(final int start, final int limit, final long time, final Long orgId, final Long userId) {
        super(start, limit);
        this.orgId = orgId;
        this.userId = userId;
        this.time = time;
        notificationAlertManager = NotificationAlertManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return chat page.
     */
    @Override
    public Page<NotificationAlert> fetchPageData() {
        final Page<NotificationAlert> page = new Page<>();

        page.setData(notificationAlertManager.get(orgId, recordStartingIndex, recordsPerPage, time, userId));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return chat count.
     */
    @Override
    public long totalRecordsCount() {
        return notificationAlertManager.getTotalCount(orgId, time, userId);
    }
}