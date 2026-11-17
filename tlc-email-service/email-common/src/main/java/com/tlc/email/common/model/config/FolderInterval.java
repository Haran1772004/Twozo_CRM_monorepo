package com.tlc.email.common.model.config;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Represents the {@link EmailInterval} based on {@link FolderType}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class FolderInterval implements Comparable<FolderInterval> {

    private FolderType folder;
    private Collection<EmailInterval> emailIntervals;

    public FolderType getFolder() {
        return folder;
    }

    public void setFolder(final FolderType folder) {
        this.folder = folder;
    }

    public Collection<EmailInterval> getEmailIntervals() {
        return emailIntervals;
    }

    public void setEmailIntervals(final Collection<EmailInterval> emailIntervals) {
        this.emailIntervals = emailIntervals;
    }

    public void addEmailInterval(final EmailInterval emailInterval) {
        this.emailIntervals = this.emailIntervals == null ? new ArrayList<>() : emailIntervals;

        this.emailIntervals.add(emailInterval);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param folderInterval the object to be compared.
     * @return
     */
    @Override
    public int compareTo(final FolderInterval folderInterval) {
        return Integer.compare(getFolderPriority(this.folder), getFolderPriority(folderInterval.folder));
    }

    /**
     * <p>
     * Gets the folder priority based on provided {@link FolderType}.
     * </p>
     *
     * @param folderType Represents the {@link FolderType}
     * @return the folder priority order
     */
    private int getFolderPriority(final FolderType folderType) {
        return switch (folderType) {
            case INBOX -> 1;
            case SENT -> 2;
            case DRAFT -> 3;
            case ARCHIVE -> 4;
        };
    }
}
