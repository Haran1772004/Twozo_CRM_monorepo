package com.tlc.crm.entity.followers.records;

import java.util.Set;

/**
 * A record class that contains followers details
 *
 * @author Ryan N
 * @version 1.0
 */
public record UserFollowerUpdateRecords(Set<Long> followersToAdd, Set<Long> followersToRemove) {
}
