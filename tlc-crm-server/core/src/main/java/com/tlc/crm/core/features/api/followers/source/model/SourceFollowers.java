package com.tlc.crm.core.features.api.followers.source.model;

import com.tlc.crm.core.features.api.followers.model.Follower;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * <p>
 * A model that represents the {@link SourceFollowers} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class SourceFollowers {

    private Long id;
    private Long orgId;
    private Long entityId;
    private Source source;
    private Long ownerId;
    private Collection<Follower> followers;

    public Long setId() {
        return id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Collection<Follower> getFollowers() {
        return followers;
    }

    public void setFollowers(Collection<Follower> followers) {
        this.followers = followers;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }
}
