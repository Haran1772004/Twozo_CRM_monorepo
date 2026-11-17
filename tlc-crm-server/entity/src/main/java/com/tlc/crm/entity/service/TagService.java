package com.tlc.crm.entity.service;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.service.internal.TagServiceImpl;

import java.util.Collection;
import java.util.Optional;

/**
 * Service responsible for retrieving tags associated with an organization
 *
 * @author sundar
 * @version 1.0
 */
public interface TagService {

    /**
     * Gets the instance of {@link TagService}
     *
     * @return the {@link TagService} instance
     */
    static TagService getInstance() {
        return TagServiceImpl.getInstance();
    }

    /**
     * <p>
     * Checks whether the given tags are already present or not
     * </p>
     *
     * @param orgId    Represents an organization
     * @param tagNames Represents the tag names
     * @return true, if already present
     */
    boolean exists(final long orgId, final Collection<String> tagNames);

    /**
     * <p>
     * Checks whether the given tag name already exists for another tag
     * </p>
     *
     * @param orgId  Represents an organization
     * @param tagNames  Represents the tag name
     * @param tagIds  Represents the tag id to be excluded from the check
     * @return true, if the tag name exists for another tag
     */
    boolean exists(final long orgId, final Collection<String> tagNames, final Collection<Long> tagIds);

    /**
     * <p>
     * Resolves the existing tag for the given organization and updates the provided
     * tag with the existing tag's ID when a tag with the same name is found.
     * </p>
     *
     * @param orgId Represents an organization
     * @param tag   Represents the tag to resolve
     */
    void resolveExistingTag(final long orgId, final Tag tag);

    /**
     * Get the tags by given names and organization ID
     *
     * @param orgId    Represents the organization ID
     * @param tagNames The tag names to be fetched
     * @return the {@link Collection} of tags
     */
    Collection<Tag> getTagsByNames(final long orgId, final Collection<String> tagNames);

    /**
     * Gets the tags by given tag IDs and organization ID
     *
     * @param orgId  Represents the organization ID
     * @param tagIds The tag IDs to be fetched
     * @return the {@link Collection} of tags
     */
    Collection<Tag> getTags(final long orgId, final Collection<Long> tagIds);

    /**
     * Gets the {@link Tag} by the provided name and organisation ID
     *
     * @param orgId Represents the organisation ID
     * @param name  Represents the name of the tag to be fetched
     * @return the {@link Tag}
     */
    Optional<Tag> get(final long orgId, final String name);

    /**
     * Gets the {@link Tag} by the provided ID and organisation ID
     *
     * @param orgId Represents the organisation ID
     * @param id    Represents the id of the tag to be fetched
     * @return the {@link Tag}
     */
    Optional<Tag> get(final long orgId, final long id);

    /**
     * Verifies the existence of the specified named tag
     *
     * @param orgId The organization ID
     * @param name  The name of the tag to fetch
     * @return true if the tag exists, false otherwise
     */
    boolean exists(final long orgId, final String name);
}
