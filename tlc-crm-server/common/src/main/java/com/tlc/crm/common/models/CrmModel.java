package com.tlc.crm.common.models;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Model which represents the {@link CrmModel}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface CrmModel extends TlcModel {

    /**
     * <p>
     * Gets the collection of user ids
     * </p>
     *
     * @return The collection of user ids
     */
    Collection<Long> userIds();
}
