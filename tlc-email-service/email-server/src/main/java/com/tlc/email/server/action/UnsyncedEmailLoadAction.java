package com.tlc.email.server.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.action.PublicAction;
import com.tlc.email.common.action.req.EmailRequest;
import com.tlc.email.common.action.res.EmailResponse;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.server.fields.EmailFields;
import com.tlc.email.server.internal.handler.UnsyncedEmailLoaderService;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Handles the loading of unsynced emails.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
@WebAction(path = "/unsynced/load", method = HttpMethod.POST)
public class UnsyncedEmailLoadAction extends PublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsyncedEmailLoadAction.class);

    private final UnsyncedEmailLoaderService unsyncedEmailLoaderService;

    public UnsyncedEmailLoadAction() {
        this.unsyncedEmailLoaderService = UnsyncedEmailLoaderService.getInstance();
    }

    /**
     * <p>
     * Processes the request to load unsynced emails for a user. If an email address is provided in the request,
     * it loads unsynced emails for that specific email address and folder. If no email address is provided, it loads unsynced emails for all email addresses.
     * </p>
     *
     * @param emailRequest  The incoming email request containing the details for loading unsynced emails.
     * @param emailResponse The response object to send back the result of the operation.
     */
    @Override
    public void pProcess(final EmailRequest emailRequest, final EmailResponse emailResponse) {
        final JsonObject requestJson = emailRequest.getRequestJson();

        if (requestJson.containsKey(EmailFields.EMAIL_ADDRESS)) {
            final String emailAddress = requestJson.getString(EmailFields.EMAIL_ADDRESS);
            final FolderType folderType = FolderType.get(requestJson.getShort(EmailFields.FOLDER_ID));
            final long startTime = requestJson.getLong(EmailFields.START_TIME);
            final long endTime = requestJson.getLong(EmailFields.END_TIME);

            LOGGER.info("UnsyncedEmailLoadAction: Loading unsynced emails for email address: {}, folderId: {}, startTime: {}, endTime: {}",
                    emailAddress, folderType, startTime, endTime);
            emailResponse.send();
            unsyncedEmailLoaderService.loadEmails(emailAddress, folderType, startTime, endTime);
            LOGGER.info("UnsyncedEmailLoadAction: Completed loading unsynced emails for email address: {}, folderId: {}, startTime: {}, endTime: {}",
                    emailAddress, folderType, startTime, endTime);
        } else {
            LOGGER.info("UnsyncedEmailLoadAction: Loading unsynced emails for all email addresses.");
            emailResponse.send();
            unsyncedEmailLoaderService.loadEmails();
            LOGGER.info("UnsyncedEmailLoadAction: Completed loading unsynced emails for all email addresses.");
        }

    }
}
