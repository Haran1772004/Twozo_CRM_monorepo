package com.tlc.crm.entity.models;

/**
 * <p>
 * Represents the types of the relationship between the user and entity.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum UserEntityRelationshipType {

    /**
     * Sales owner of the entity.
     */
    OWNER_OF_ENTITY,

    /**
     * An entity which is mapped with user's territory.
     */
    USER_TERRITORY_ENTITY,

    /**
     * An entity which is following by user.
     */
    FOLLOWER_OF_ENTITY,
}
