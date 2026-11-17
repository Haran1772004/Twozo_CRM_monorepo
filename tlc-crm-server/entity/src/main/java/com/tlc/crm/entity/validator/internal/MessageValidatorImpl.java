package com.tlc.crm.entity.validator.internal;

import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.features.api.note.model.Message;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Validator implementation for the {@link MessageValidator} annotation
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public class MessageValidatorImpl implements ConstraintValidator<MessageValidator, Message> {

    private static final HtmlParser HTML_PARSER = HtmlParser.getInstance();
    private static final int MAXIMUM_MENTIONABLE_USERS = NumericalConstant.ONE_HUNDRED;
    private static final int MAXIMUM_CONTENT_LENGTH = NumericalConstant.TEN_THOUSAND;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(final Message message, final ConstraintValidatorContext constraintValidatorContext) {
        final String content = message.getContent();

        if (Objects.isNull(content)) {
            return true;
        }
        final String plainTextContent = HTML_PARSER.isHtml(content) ? HTML_PARSER.parse(content).text() : content;
        final Collection<CrmUser> mentionedUsers = message.getMentionedUsers();
        final boolean isContentValid = plainTextContent.length() <= MAXIMUM_CONTENT_LENGTH;
        final boolean isMentionedUsersValid = Objects.isNull(mentionedUsers) || mentionedUsers.size() <= MAXIMUM_MENTIONABLE_USERS;

        if (isContentValid && isMentionedUsersValid) {
            return true;
        }
        constraintValidatorContext.disableDefaultConstraintViolation();

        if (!isContentValid) {
            constraintValidatorContext.buildConstraintViolationWithTemplate("i18n.note.max.size")
                    .addConstraintViolation();
        } else if (!isMentionedUsersValid) {
            constraintValidatorContext.buildConstraintViolationWithTemplate("i18n.message.mentioned.users.max.size")
                    .addConstraintViolation();
        }

        return false;
    }
}
