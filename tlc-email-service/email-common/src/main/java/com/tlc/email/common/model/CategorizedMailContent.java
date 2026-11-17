package com.tlc.email.common.model;

import com.tlc.email.common.model.type.Email;

/**
 * Represents the mail content along with conversation and its category.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public record CategorizedMailContent(Long conversationId, String subject, String content, Email.MessageCategory messageCategory) {
}
