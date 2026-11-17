package com.tlc.crm.entity.list.dynamic.model;

import com.tlc.crm.entity.list.model.Provider;

import java.util.Collection;

/**
 * <p>
 * Holder to hold the dynamic fields requested data
 * </p>
 *
 * @param orgId         Represents an org id
 * @param entityIds     Which contains the collection of ids
 * @param provider      Represents the {@link Provider}
 * @param dynamicFields Which contains the dynamic fields
 * @author Kishore J
 * @version 1.0
 */
public record DynamicFieldRequest(long orgId, Collection<Long> entityIds, Provider provider, Collection<String> dynamicFields) {
}
