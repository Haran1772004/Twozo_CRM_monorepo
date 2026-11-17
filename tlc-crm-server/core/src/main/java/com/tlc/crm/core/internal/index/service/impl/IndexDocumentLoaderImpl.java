package com.tlc.crm.core.internal.index.service.impl;

import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.core.internal.index.service.IndexDocumentLoader;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Loads data into the document for indexing.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class IndexDocumentLoaderImpl implements IndexDocumentLoader {

    private final SourceFollowersManager followersManager;
    private final IndexDataFetcher indexDataFetcher;

    private IndexDocumentLoaderImpl() {
        followersManager = SourceFollowersManager.getInstance();
        indexDataFetcher = IndexDataFetcher.get();
    }

    private static class Instance {
        private static final IndexDocumentLoaderImpl INSTANCE = new IndexDocumentLoaderImpl();
    }

    /**
     * <p>
     * Gets the {@link IndexDocumentLoaderImpl} Instance
     * </p>
     *
     * @return {@link IndexDocumentLoaderImpl}
     */
    public static IndexDocumentLoaderImpl getInstance() {
        return IndexDocumentLoaderImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads follower ids for contact index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    public void loadFollowerIdsForContactIndex(final Long orgId, final Map<String, Object> doc) {
        final Collection<Long> followerIds = followersManager.getFollowerIdsByEntityId(orgId, (Long) doc.get(CommonSearchFields.ID), Source.CONTACT);

        if (!followerIds.isEmpty()) {
            doc.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
        }
        loadCompanyFollowerIds(orgId, doc);
    }

    /**
     * <p>
     * Loads follower ids for contact index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    public void loadFollowerIdsForCompanyIndex(final Long orgId, final Map<String, Object> doc) {
        final Collection<Long> followerIds = followersManager.getFollowerIdsByEntityId(orgId, (Long) doc.get(CommonSearchFields.ID), Source.COMPANY);

        if (!followerIds.isEmpty()) {
            doc.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
        }
    }

    /**
     * <p>
     * Loads follower ids for deal index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    public void loadFollowerIdsForDealIndex(final Long orgId, final Map<String, Object> doc) {
        final Collection<Long> followerIds = followersManager.getFollowerIdsByEntityId(orgId, (Long) doc.get(CommonSearchFields.ID), Source.DEAL);

        if (!followerIds.isEmpty()) {
            doc.put(CommonSearchFields.FOLLOWER_IDS, followerIds);
        }
        loadContactFollowerIds(orgId, doc);
        loadCompanyFollowerIds(orgId, doc);
    }

    /**
     * <p>
     * Loads follower ids for activity index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    public void loadFollowerIdsForActivityIndex(final Long orgId, final Map<String, Object> doc) {
        final Collection<Long> contactIds = (Collection<Long>) doc.get(CommonSearchFields.CONTACT_IDS);
        final Collection<Long> ownerIds = ((Collection<Long>) doc.get(CommonSearchFields.OWNER_IDS));
        final Collection<Long> territoryIds = new HashSet<>();

        if (Objects.nonNull(contactIds) && !contactIds.isEmpty()) {
            final Map<String, Collection<Long>> contactPermissionRelatedIdsMap =
                    indexDataFetcher.getContactPermissionRelatedIdsMap(orgId, contactIds);

            ownerIds.addAll(contactPermissionRelatedIdsMap.get(CommonSearchFields.OWNER_IDS));
            territoryIds.addAll(contactPermissionRelatedIdsMap.get(CommonSearchFields.TERRITORY_IDS));
        }
        loadDealFollowerIds(orgId, doc, ownerIds, territoryIds);
        loadContactFollowerIds(orgId, doc);
        loadCompanyFollowerIds(orgId, doc, ownerIds, territoryIds);
        territoryIds.removeIf(Objects::isNull);
        ownerIds.removeIf(Objects::isNull);
        doc.put(CommonSearchFields.OWNER_IDS, ownerIds);

        if (!territoryIds.isEmpty()) {
            doc.put(CommonSearchFields.TERRITORY_IDS, territoryIds);
        }
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    @Override
    public void loadPermissionRelatedFieldsForNoteIndex(final Long orgId, final Map<String, Object> doc) {
        final NoteHolder.Source source = (NoteHolder.Source) doc.get(CommonSearchFields.ENTITY_SOURCE);
        final String sourceName = source.name().toLowerCase();

        loadPermissionRelatedFieldsBasedOnSource(orgId, doc, sourceName);
        loadSourceDetails(sourceName, doc);
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    @Override
    public void loadPermissionRelatedFieldsForFileIndex(final Long orgId, final Map<String, Object> doc) {
        final String source = ((String) doc.get(CommonSearchFields.ENTITY_SOURCE)).toLowerCase();

        loadPermissionRelatedFieldsBasedOnSource(orgId, doc, source);
        loadSourceDetails(source, doc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFollowerIdsForSourceOwnedDoc(final Long orgId, final String source, final Map<String, Object> doc) {
        switch (source.toLowerCase()) {
            case CommonSearchFields.DEAL -> {
                loadFollowerIdsBasedOnSource(orgId, doc, Source.DEAL);
                loadContactFollowerIds(orgId, doc);
                loadCompanyFollowerIds(orgId, doc);
            }
            case CommonSearchFields.CONTACT -> {
                loadFollowerIdsBasedOnSource(orgId, doc, Source.CONTACT);
                loadCompanyFollowerIds(orgId, doc);
            }
            case CommonSearchFields.COMPANY -> loadFollowerIdsBasedOnSource(orgId, doc, Source.COMPANY);
            default -> {
            }
        }
    }

    /**
     * <p>
     * Load follower ids onto an entity card map (root + nested paths per mapping).
     * </p>
     *
     * @param orgId     An organization's id.
     * @param className {@link TlcModel} name.
     * @param doc       Holds the entity data to be indexed.
     */
    @Override
    public void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc) {
        switch (className.toLowerCase()) {
            case CommonSearchFields.DEAL -> loadFollowerIdsForDealIndex(orgId, doc);
            case CommonSearchFields.CONTACT -> loadFollowerIdsForContactIndex(orgId, doc);
            case CommonSearchFields.COMPANY -> loadFollowerIdsForCompanyIndex(orgId, doc);
            default -> {
            }
        }
    }

    /**
     * <p>
     * Loads source details
     * </p>
     *
     * @param source Source's name.
     * @param doc    Holds the entity data to be indexed.
     */
    private void loadSourceDetails(final String source, final Map<String, Object> doc) {
        final Map<String, Object> sourceDoc = (Map<String, Object>) doc.get(source);

        doc.put(CommonSearchFields.ENTITY_SOURCE_ID, sourceDoc.get(CommonSearchFields.ID));
        doc.put(CommonSearchFields.ENTITY_SOURCE_NAME, sourceDoc.get(CommonSearchFields.NAME));
    }

    /**
     * <p>
     * Loads follower ids related to contact.
     * </p>
     *
     * @param orgId Organization's id.
     * @param doc   Holds the entity data to be indexed.
     */
    private void loadContactFollowerIds(final Long orgId, final Map<String, Object> doc) {
        final Map<String, Object> contactDoc = (Map<String, Object>) doc.get(CommonSearchFields.CONTACT);

        if (Objects.nonNull(contactDoc)) {
            final Collection<Long> contactFollowers = followersManager.getFollowerIdsByEntityId(
                    orgId, (Long) contactDoc.get(CommonSearchFields.ID), Source.CONTACT);

            if (!contactFollowers.isEmpty()) {
                contactDoc.put(CommonSearchFields.FOLLOWER_IDS, contactFollowers);
            }
            loadCompanyFollowerIds(orgId, contactDoc);
        }
    }

    /**
     * <p>
     * Loads follower ids related to company.
     * </p>
     *
     * @param orgId Organization's id.
     * @param doc   Holds the entity data to be indexed.
     */
    private void loadCompanyFollowerIds(final Long orgId, final Map<String, Object> doc) {
        final Map<String, Object> companyDoc = (Map<String, Object>) doc.get(CommonSearchFields.COMPANY);

        if (Objects.nonNull(companyDoc)) {
            final Collection<Long> companyFollowers = followersManager.getFollowerIdsByEntityId(
                    orgId, (Long) companyDoc.get(CommonSearchFields.ID), Source.COMPANY);

            if (!companyFollowers.isEmpty()) {
                companyDoc.put(CommonSearchFields.FOLLOWER_IDS, companyFollowers);
            }
        }
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    private void loadPermissionRelatedFieldsBasedOnSource(final Long orgId, final Map<String, Object> doc,
                                                          final String source) {
        final Map<String, Object> entityDoc = (Map<String, Object>) doc.get(source);

        if (entityDoc.containsKey(CommonSearchFields.TERRITORY_ID)) {
            doc.put(CommonSearchFields.TERRITORY_ID, entityDoc.get(CommonSearchFields.TERRITORY_ID));
        }
        doc.put(CommonSearchFields.OWNER_ID, entityDoc.get(CommonSearchFields.OWNER_ID));
        doc.put(CommonSearchFields.ENTITY_SOURCE, source);

        switch (source) {
            case CommonSearchFields.DEAL -> {
                loadFollowerIdsBasedOnSource(orgId, doc, Source.DEAL);
                loadContactFollowerIds(orgId, doc);
                loadCompanyFollowerIds(orgId, doc);
            }
            case CommonSearchFields.CONTACT -> {
                loadFollowerIdsBasedOnSource(orgId, doc, Source.CONTACT);
                loadCompanyFollowerIds(orgId, doc);
            }
            case CommonSearchFields.COMPANY -> loadFollowerIdsBasedOnSource(orgId, doc, Source.COMPANY);
        }
    }

    /**
     * <p>
     * Loads follower ids based on source.
     * </p>
     *
     * @param orgId Organization's id.
     * @param doc   Holds the entity data to be indexed.
     */
    private void loadFollowerIdsBasedOnSource(final Long orgId, final Map<String, Object> doc, final Source source) {
        final Map<String, Object> entityDoc = (Map<String, Object>) doc.get(source.getName().toLowerCase());

        if (Objects.nonNull(entityDoc)) {
            final Collection<Long> followers = followersManager.getFollowerIdsByEntityId(
                    orgId, (Long) entityDoc.get(CommonSearchFields.ID), source);

            if (!followers.isEmpty()) {
                entityDoc.put(CommonSearchFields.FOLLOWER_IDS, followers);
                doc.put(CommonSearchFields.FOLLOWER_IDS, followers);
            }
        }
    }

    /**
     * <p>
     * Loads follower ids related to deal.
     * </p>
     *
     * @param orgId        Organization's id.
     * @param doc          Holds the entity data to be indexed.
     * @param ownerIds     Owner ids.
     * @param territoryIds Territory ids.
     */
    private void loadDealFollowerIds(final Long orgId, final Map<String, Object> doc, final Collection<Long> ownerIds,
                                     final Collection<Long> territoryIds) {
        final Map<String, Object> dealDoc = (Map<String, Object>) doc.get(CommonSearchFields.DEAL);

        if (Objects.nonNull(dealDoc)) {
            final Collection<Long> dealFollowers = followersManager.getFollowerIdsByEntityId(
                    orgId, (Long) dealDoc.get(CommonSearchFields.ID), Source.DEAL);

            if (!dealFollowers.isEmpty()) {
                dealDoc.put(CommonSearchFields.FOLLOWER_IDS, dealFollowers);
                ownerIds.addAll(dealFollowers);
            }
            ownerIds.add((Long) dealDoc.get(CommonSearchFields.OWNER_ID));
            territoryIds.add((Long) dealDoc.get(CommonSearchFields.TERRITORY_ID));
            loadCompanyFollowerIds(orgId, dealDoc);
            loadContactFollowerIds(orgId, dealDoc);
        }
    }

    /**
     * <p>
     * Loads follower ids related to company.
     * </p>
     *
     * @param orgId        Organization's id.
     * @param doc          Holds the entity data to be indexed.
     * @param ownerIds     Owner ids.
     * @param territoryIds Territory ids.
     */
    private void loadCompanyFollowerIds(final Long orgId, final Map<String, Object> doc,
                                        final Collection<Long> ownerIds, final Collection<Long> territoryIds) {
        final Map<String, Object> companyDoc = (Map<String, Object>) doc.get(CommonSearchFields.COMPANY);

        if (Objects.nonNull(companyDoc)) {
            final Collection<Long> companyFollowers = followersManager.getFollowerIdsByEntityId(
                    orgId, (Long) companyDoc.get(CommonSearchFields.ID), Source.COMPANY);

            if (!companyFollowers.isEmpty()) {
                companyDoc.put(CommonSearchFields.FOLLOWER_IDS, companyFollowers);
                ownerIds.addAll(companyFollowers);
            }
            ownerIds.add((Long) companyDoc.get(CommonSearchFields.OWNER_ID));
            territoryIds.add((Long) companyDoc.get(CommonSearchFields.TERRITORY_ID));
        }
    }
}