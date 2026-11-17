package com.tlc.email.server.internal.provider.jakarta;

import com.sun.mail.imap.IMAPFolder;
import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.server.internal.provider.jakarta.models.EmailFolder;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Resolves logical email folders to provider-specific IMAP folders.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ImapFolderResolver {

    private static final String ATTR_INBOX = "\\inbox";
    private static final String ATTR_SENT = "\\sent";
    private static final String ATTR_DRAFTS = "\\drafts";
    private static final String ATTR_TRASH = "\\trash";
    private static final String ATTR_SPAM = "\\spam";
    private static final String ATTR_JUNK = "\\junk";
    private static final String ATTR_ARCHIVE = "\\archive";

    private static final String NAME_INBOX = "inbox";
    private static final String NAME_SENT = "sent";
    private static final String NAME_DRAFT = "draft";
    private static final String NAME_TRASH = "trash";
    private static final String NAME_DELETED = "deleted";
    private static final String NAME_SPAM = "spam";
    private static final String NAME_JUNK = "junk";
    private static final String NAME_BULK = "bulk";
    private static final String NAME_ARCHIVE = "archive";

    private static final String FOLDER_WILDCARD = "*";

    private final EnumMap<EmailFolder, String> folderCache = new EnumMap<>(EmailFolder.class);
    private final boolean enableHeuristics;

    /**
     * <p>
     * Creates a resolver and discovers folders from the given store.
     * </p>
     *
     * @param enableHeuristics Enables heuristic-based folder detection.
     */
    private ImapFolderResolver(final boolean enableHeuristics) {
        this.enableHeuristics = enableHeuristics;
    }

    /**
     * <p>
     * Returns the actual IMAP folder name for the given logical folder.
     * </p>
     *
     * @param folder Represents the logical {@link EmailFolder}.
     * @return The resolved IMAP folder name.
     */
    public String getFolderName(final EmailFolder folder) {
        return folderCache.get(folder);
    }

    /**
     * <p>
     * Returns the folder names of cached folders that are not present
     * in the provided collection of email folders.
     * </p>
     *
     * @param folders A collection of {@link EmailFolder} to exclude.
     * @return A {@link Collection} of folder names {@link String} that are in the cache but not in the input collection.
     */
    public Collection<String> getFolderNamesExcluding(final Collection<EmailFolder> folders) {
        if (Objects.isNull(folders)) {
            return new ArrayList<>(folderCache.values());
        }
        final Collection<String> result = new ArrayList<>();

        for (final Map.Entry<EmailFolder, String> entry : folderCache.entrySet()) {

            if (!folders.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }

        return result;
    }

    /**
     * <p>
     * Builder for constructing {@link ImapFolderResolver} instances.
     * </p>
     */
    public static class Builder {

        private Store store;
        private boolean heuristics = true;

        /**
         * <p>
         * Sets the IMAP store used for folder discovery.
         * </p>
         *
         * @param store Represents the connected {@link Store}.
         * @return The current {@link Builder} instance.
         */
        public Builder store(final Store store) {
            this.store = store;

            return this;
        }

        /**
         * <p>
         * Enables or disables heuristic folder name detection.
         * </p>
         *
         * @param enable Indicates whether heuristics are enabled.
         * @return The current {@link Builder} instance.
         */
        public Builder enableHeuristics(final boolean enable) {
            this.heuristics = enable;

            return this;
        }

        /**
         * <p>
         * Builds and returns an {@link ImapFolderResolver}.
         * </p>
         *
         * @return A configured {@link ImapFolderResolver}.
         */
        public ImapFolderResolver build() {
            try {
                if (Objects.isNull(store)) {
                    throw ErrorCode.get(EmailErrorCodes.SERVER_CONNECT_ACTION_FAILED);
                }
                final ImapFolderResolver resolver = new ImapFolderResolver(heuristics);

                try {
                    resolver.discoverFolders(
                            store.getDefaultFolder().list(FOLDER_WILDCARD)
                    );

                    return resolver;
                } catch (Exception exception) {
                    throw ErrorCode.get(EmailErrorCodes.SERVER_CONNECT_ACTION_FAILED, exception);
                }
            } catch (Exception exception) {
                throw ErrorCode.get(EmailErrorCodes.FOLDER_RESOLVER_BUILD_ACTION_FAILED);
            }
        }
    }

    /**
     * <p>
     * Discovers and maps folders from the IMAP server.
     * </p>
     *
     * @param folders Represents the folders retrieved from the IMAP server.
     */
    private void discoverFolders(final Folder[] folders) throws MessagingException {
        if (Objects.isNull(folders)) {
            return;
        }

        for (final Folder folder : folders) {
            final String fullName = folder.getFullName();
            final String lName = fullName.toLowerCase();

            if (folder instanceof IMAPFolder imapFolder) {
                final String[] attrs = imapFolder.getAttributes();

                if (Objects.nonNull(attrs)) {

                    for (final String attr : attrs) {
                        switch (attr.toLowerCase()) {
                            case ATTR_INBOX -> folderCache.putIfAbsent(EmailFolder.INBOX, fullName);
                            case ATTR_SENT -> folderCache.putIfAbsent(EmailFolder.SENT, fullName);
                            case ATTR_DRAFTS -> folderCache.putIfAbsent(EmailFolder.DRAFT, fullName);
                            case ATTR_TRASH -> folderCache.putIfAbsent(EmailFolder.TRASH, fullName);
                            case ATTR_SPAM, ATTR_JUNK -> folderCache.putIfAbsent(EmailFolder.SPAM, fullName);
                            case ATTR_ARCHIVE -> folderCache.putIfAbsent(EmailFolder.ARCHIVE, fullName);
                        }
                    }
                }
            }

            if (enableHeuristics) {
                if (!folderCache.containsKey(EmailFolder.SENT) && lName.contains(NAME_SENT)) {
                    folderCache.put(EmailFolder.SENT, fullName);
                }
                if (!folderCache.containsKey(EmailFolder.DRAFT) && lName.contains(NAME_DRAFT)) {
                    folderCache.put(EmailFolder.DRAFT, fullName);
                }
                if (!folderCache.containsKey(EmailFolder.TRASH) &&
                        (lName.contains(NAME_TRASH) || lName.contains(NAME_DELETED))) {
                    folderCache.put(EmailFolder.TRASH, fullName);
                }
                if (!folderCache.containsKey(EmailFolder.SPAM) &&
                        (lName.contains(NAME_SPAM) || lName.contains(NAME_JUNK) || lName.contains(NAME_BULK))) {
                    folderCache.put(EmailFolder.SPAM, fullName);
                }
                if (!folderCache.containsKey(EmailFolder.INBOX) && lName.equals(NAME_INBOX)) {
                    folderCache.put(EmailFolder.INBOX, fullName);
                }
                if (!folderCache.containsKey(EmailFolder.ARCHIVE) && lName.contains(NAME_ARCHIVE)) {
                    folderCache.put(EmailFolder.ARCHIVE, fullName);
                }
            }
        }

        for (final EmailFolder folder : EmailFolder.values()) {
            folderCache.putIfAbsent(folder, folder.name());
        }
    }
}