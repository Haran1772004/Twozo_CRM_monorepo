package com.tlc.email.common.model.config;

import java.util.Map;

/**
 * <p>
 * Configures the retrieval of emails. Holds the settings related to the email retrieval process.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailConfiguration
 */
public class EmailReceiverConfiguration extends EmailConfiguration {

    private Map<FolderType, EmailInterval> emailIntervals;

    public Map<FolderType, EmailInterval> getEmailIntervals() {
        return emailIntervals;
    }

    public void setEmailIntervals(final Map<FolderType, EmailInterval> emailIntervals) {
        this.emailIntervals = emailIntervals;
    }

    public FolderType getFirstFolderType() {
        return getFolderTypes().stream().findFirst().orElseGet(() -> null);
    }
}
