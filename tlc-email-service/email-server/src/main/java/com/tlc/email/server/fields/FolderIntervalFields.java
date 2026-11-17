package com.tlc.email.server.fields;

import com.tlc.email.common.model.config.FolderInterval;

/**
 * <p>
 * Represents the {@link FolderInterval} related fields.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class FolderIntervalFields {

    private FolderIntervalFields() {
    }

    public static final String ID = "id";
    public static final String INBOX = "inbox";
    public static final String SENT = "sent";
    public static final String DRAFT = "draft";
    public static final String ARCHIVE = "archive";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
}
