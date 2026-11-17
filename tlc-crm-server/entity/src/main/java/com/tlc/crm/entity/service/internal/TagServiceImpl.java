package com.tlc.crm.entity.service.internal;

import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.service.TagService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides definitions for the {@link TagService} that responsible for retrieving tags associated with an organization
 *
 * @author sundar
 * @version 1.0
 */
public class TagServiceImpl implements TagService {

    private final TagManager tagManager;

    private TagServiceImpl() {
        this.tagManager = TagManager.getInstance();
    }

    private static final class Instance {
        private static final TagService TAG_SERVICE = new TagServiceImpl();
    }

    /**
     * Gets the instance of {@link TagService}
     *
     * @return the {@link TagService} instance
     */
    public static TagService getInstance() {
        return Instance.TAG_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization
     * @param tagNames Represents the tag names
     * @return true, if already present
     */
    @Override
    public boolean exists(final long orgId, final Collection<String> tagNames) {
        final Collection<Tag> tags = tagManager.get(orgId);
        final Collection<String> tagsByNames = tags.stream().map(tag -> tag.getName().toLowerCase()).collect(Collectors.toSet());

        for (final String tagName : tagNames) {
            if (Objects.nonNull(tagName) && tagsByNames.contains(tagName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization
     * @param tagNames   Represents the tag name
     * @param tagIds  Represents the tag ID to be excluded from the check
     * @return true, if the tag name exists for another tag
     */
    @Override
    public boolean exists(final long orgId, final Collection<String> tagNames, final Collection<Long> tagIds) {
        final Collection<String> names = tagNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
        final Collection<Tag> existingTags = tagManager.get(orgId);

        return existingTags.stream().filter(tag -> names.contains(tag.getName().toLowerCase())).anyMatch(tag -> !tagIds.contains(tag.id()));
    }

    /**
     * <p>
     * Resolves the ID of an existing tag with the same name for the given organization.
     * The tag ID is updated only when the provided tag does not already have an ID.
     * </p>
     *
     * @param orgId Represents an organization
     * @param tag   Represents the tag whose existing ID needs to be resolved
     */
    @Override
    public void resolveExistingTag(final long orgId, final Tag tag) {
        if (Objects.isNull(tag.id())) {
            get(orgId, tag.getName()).ifPresent(existingTag -> tag.setId(existingTag.id()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization ID
     * @param tagNames The tag names to be fetched
     * @return the {@link Collection} of tags
     */
    @Override
    public Collection<Tag> getTagsByNames(final long orgId, final Collection<String> tagNames) {
        final Collection<Tag> tags = tagManager.get(orgId);
        final Collection<String> tagsByNames = tags.stream().map(tag -> tag.getName().toLowerCase()).collect(Collectors.toSet());
        final Collection<Tag> matchedTags = new HashSet<>();

        for (final String tagName : tagNames) {
            if (tagsByNames.contains(tagName.toLowerCase())) {
                final Tag matchedTag = tags.stream().filter(tag -> tag.getName().equalsIgnoreCase(tagName)).findFirst().orElse(null);

                if (Objects.nonNull(matchedTag)) {
                    matchedTags.add(matchedTag);
                }
            }
        }
        return Collections.unmodifiableCollection(matchedTags);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization ID
     * @param tagIds The tag IDs to be fetched
     * @return the {@link Collection} of tags
     */
    @Override
    public Collection<Tag> getTags(final long orgId, final Collection<Long> tagIds) {
        return tagManager.get(orgId, tagIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID
     * @param name  Represents the name of the tag to be fetched
     * @return the {@link Tag}
     */
    @Override
    public Optional<Tag> get(final long orgId, final String name) {
        final Collection<Tag> tags = tagManager.get(orgId);

        return tags.stream().filter(tag -> tag.getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID
     * @param id    Represents the id of the tag to be fetched
     * @return the {@link Tag}
     */
    @Override
    public Optional<Tag> get(final long orgId, final long id) {
        return Optional.ofNullable(tagManager.get(orgId, id));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId The organization ID
     * @param name  The name of the tag to fetch
     * @return true if the tag exists, false otherwise
     */
    @Override
    public boolean exists(final long orgId, final String name) {
        return get(orgId, name).isPresent();
    }
}