package com.tlc.crm.entity.features.api.note.model;

import java.util.Collection;

/**
 * <p>
 * Model which represents the Note
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */

public class Note extends Message {

    private Long commentsCount;
    private Collection<Comment> comments;

    public Long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(final long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Collection<Comment> getComments() {
        return comments;
    }

    public void setComments(final Collection<Comment> comments) {
        this.comments = comments;
    }
}