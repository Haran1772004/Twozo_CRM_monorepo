package com.tlc.crm.security.iam;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * Holds the basic information about the User
 *
 * @author sundar
 * @version 1.0
 */
public interface BaseCrmUser extends TlcModel {

    /**
     * Gets the role ID associated with the user
     *
     * @return the role ID
     */
    Long getRoleId();

    /**
     * Gets the {@link Collection} of team IDs associated with the user
     *
     * @return the {@link Collection} of team IDs
     */
    Collection<Long> getTeamIds();

    /**
     * Gets the {@link Collection} of territory IDs associated with the user
     *
     * @return the {@link Collection} of territory IDs
     */
    Collection<Long> getTerritoryIds();
}
