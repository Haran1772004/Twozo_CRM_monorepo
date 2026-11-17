package com.tlc.iam.server.internal.api.manager;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;

/**
 * Provides an abstract way to deal with IAM entities
 *
 * @param <T>
 * @author Selvakumar
 * @version 1.0
 */
public interface IamManager<T extends TlcModel> {

    /**
     * Checks whether an entity is exists or not
     *
     * @param id Entity id
     * @return true if an entity exists, otherwise false will be returned
     */
    boolean exists(Long id);

    /**
     * Checks whether an entity is exists under the organization or not
     *
     * @param id Entity id
     * @return true if an entity exists under the organization, otherwise false will be returned
     */
    boolean exists(Long id, Long orgId);

    /**
     * Fetches an entity by its id
     *
     * @param id Entity id
     * @return Entity
     */
    T get(Long id);

    /**
     * Saves the given entity to the datastore
     *
     * @param t An IAM entity
     */
    void save(T t);

    /**
     * Saves the given entities into datastore
     *
     * @param models Collection of IAM entities
     * @return Processed results
     */
    Map<String, Object> save(Collection<T> models);

    /**
     * Updates the given entity to the datastore
     *
     * @param t An IAM entity
     */
    void update(T t);

    /**
     * Saves the given entities into datastore
     *
     * @param models Collection of IAM entities
     * @return Processed results
     */
    Map<String, Object> update(Collection<T> models);

    /**
     * Deletes the given entity from the datastore
     *
     * @param id ID of an IAM entity
     */
    void delete(Long id);

    /**
     * Deletes the IAM entities from the datastore
     *
     * @param ids Collection of IAM entity ids
     */
    void deleteById(Collection<Long> ids);

    /**
     * Deletes the IAM entities from the datastore
     *
     * @param models Collection of IAM entities
     */
    void delete(Collection<T> models);

}
