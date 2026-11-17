package com.tlc.crm.core.model;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A model that stores the data for mapping the tags with the entities
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SourceTags {

    private final Source source;
    private final Set<Long> entityIds;
    private Long userId;
    private Set<Long> tagIds;
    private Collection<Tag> newTags;

    public SourceTags(final String sourceName) {
        this.source = Source.get(sourceName);
        this.entityIds = new HashSet<>();
    }

    public Source getSource() {
        return source;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void addTagId(final Long tagId) {
        this.tagIds = Objects.isNull(tagIds) ? new HashSet<>() : this.tagIds;

        tagIds.add(tagId);
    }

    public Set<Long> getTagIds() {
        return tagIds;
    }

    public void addEntityId(final Long entityId) {
        entityIds.add(entityId);
    }

    public Set<Long> getEntityIds() {
        return entityIds;
    }

    public void addNewTag(final Tag tag) {
        this.newTags = Objects.isNull(newTags) ? new HashSet<>() : this.newTags;

        newTags.add(tag);
    }

    public Collection<Tag> getNewTags() {
        return newTags;
    }

    public boolean areTagIdsEmpty() {
        return Objects.isNull(tagIds) || tagIds.isEmpty();
    }

    public boolean areNewTagsEmpty() {
        return Objects.isNull(newTags) || newTags.isEmpty();
    }
}