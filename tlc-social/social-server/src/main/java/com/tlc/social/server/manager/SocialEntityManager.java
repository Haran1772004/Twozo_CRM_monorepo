package com.tlc.social.server.manager;

import com.tlc.file.common.models.Attachment;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.server.sql.resource.MSOATTACHMENT;
import com.tlc.social.server.sql.resource.MSOCHANNEL;
import com.tlc.social.server.sql.resource.MSOCHAT;
import com.tlc.social.server.sql.resource.MSOMESSAGE;
import com.tlc.social.server.sql.resource.MSORECIPIENT;
import com.tlc.social.server.sql.resource.MSORECIPIENTPROFILE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.Objects;

/**
 * Manages the social entity related database actions
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class SocialEntityManager {

    /**
     * <p>
     * Gets the chat table
     * </p>
     *
     * @return the chat table
     */
    public Table getChatTable() {
        return Table.get(MSOCHAT.TABLE);
    }

    /**
     * <p>
     * Gets the message table
     * </p>
     *
     * @return the message table
     */
    protected Table getMessageTable() {
        return Table.get(MSOMESSAGE.TABLE);
    }

    /**
     * <p>
     * Gets the attachment table
     * </p>
     *
     * @return the attachment table
     */
    protected Table getAttachmentTable() {
        return Table.get(MSOATTACHMENT.TABLE);
    }

    /**
     * <p>
     * Gets the channel table
     * </p>
     *
     * @return the channel table
     */
    protected Table getChannelTable() {
        return Table.get(MSOCHANNEL.TABLE);
    }

    /**
     * <p>
     * Gets the recipient profile table
     * </p>
     *
     * @return the recipient profile table
     */
    public Table getRecipientProfileTable() {
        return Table.get(MSORECIPIENTPROFILE.TABLE);
    }

    /**
     * <p>
     * Gets the recipient table
     * </p>
     *
     * @return the recipient profile table
     */
    public Table getRecipientTable() {
        return Table.get(MSORECIPIENT.TABLE);
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return the {@link AdminDataStore}
     */
    protected AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Gets the {@link ReadableDataStore}
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    protected ReadableDataStore readableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Checks whether the provided ids of the social entity exist or not
     * </p>
     *
     * @param ids Represents the ids of the model
     * @return true if it presents, false otherwise
     */
    public abstract boolean exists(final Collection<Long> ids);

    /**
     * <p>
     * Loads the recipient profile into the container
     * </p>
     *
     * @param recipientProfile    Represents the {@link RecipientProfile}
     * @param dataContainer       Represents the container which contains existing profile details
     * @param targetDataContainer Represents the target container
     */
    public void loadRecipientProfile(final RecipientProfile recipientProfile, final DataContainer dataContainer, final DataContainer targetDataContainer) {
        final Table recipientProfileTable = getRecipientProfileTable();
        final Row profileRow = dataContainer.getOrInitRow(recipientProfileTable, recipientProfile.id());
        final Attachment profilePicture = recipientProfile.getProfilePicture();

        if (profileRow.isNewRow()) {
            recipientProfile.setId(profileRow.getPKValue());
        }

        if (Objects.nonNull(profilePicture)) {
            profileRow.set(MSORECIPIENTPROFILE.PROFILE_PIC_ID, profilePicture.id());
        }
        profileRow.set(MSORECIPIENTPROFILE.NAME, recipientProfile.getName());

        targetDataContainer.updateRow(profileRow);
    }
}
