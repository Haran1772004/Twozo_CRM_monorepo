package com.tlc.email.server.internal.provider.gmail.converter;

import com.google.api.services.gmail.model.Draft;
import com.tlc.email.common.converter.Converter;
import com.tlc.email.common.model.type.Email;

/**
 * <p>
 * Converts a Gmail {@link Draft} to the internal {@link Email} model.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class GmailDraftMessageConverter implements Converter<Draft, Email> {

    private final GmailMessageConverter gmailMessageConverter;

    private GmailDraftMessageConverter() {
        this.gmailMessageConverter = GmailMessageConverter.getInstance();
    }

    private static class Instance {
        private static final GmailDraftMessageConverter INSTANCE = new GmailDraftMessageConverter();
    }

    public static GmailDraftMessageConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the given Gmail {@link Draft} into an internal {@link Email} object.
     * </p>
     *
     * @param draft the Gmail draft object
     * @return converted draft object with draft metadata set
     */
    @Override
    public Email convert(final Draft draft) {
        final Email email = gmailMessageConverter.convert(draft.getMessage());

        email.getMetaData().setInternalDraftId(draft.getId());

        return email;
    }
}