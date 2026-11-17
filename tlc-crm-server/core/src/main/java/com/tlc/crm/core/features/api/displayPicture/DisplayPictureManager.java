package com.tlc.crm.core.features.api.displayPicture;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.crm.core.sql.resource.MCOMDISPLAYPICTURE;
import com.tlc.crm.core.sql.resource.MCONDISPLAYPICTURE;
import com.tlc.crm.core.sql.resource.OMDPDISPLAYPICTURE;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Implements the display picture related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class DisplayPictureManager implements Serializable {

    private static class Instance {
        private static final DisplayPictureManager INSTANCE = new DisplayPictureManager();
    }

    private DisplayPictureManager() {
    }

    public static DisplayPictureManager getInstance() {
        return DisplayPictureManager.Instance.INSTANCE;
    }

    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    private Table getDisplayPictureTable() {
        return Table.get(OMDPDISPLAYPICTURE.TABLE);
    }

    /**
     * <p>
     * Gets the contact display picture table
     * </p>
     *
     * @return the contact profile picture {@link Table}
     */
    private Table getContactDisplayPictureTable() {
        return Table.get(MCONDISPLAYPICTURE.TABLE);
    }

    /**
     * <p>
     * Gets the company display picture table
     * </p>
     *
     * @return the company profile picture {@link Table}
     */
    private Table getCompanyDisplayPictureTable() {
        return Table.get(MCOMDISPLAYPICTURE.TABLE);
    }

    /**
     * <p>
     * Saves the display picture details into the container
     * </p>
     *
     * @param sourceDisplayPicture Represents the {@link SourceDisplayPicture}
     */
    public void saveDisplayPictureInfo(final SourceDisplayPicture sourceDisplayPicture) {
        final DataContainer dataContainer = DataContainer.create();

        saveDisplayPictureInfo(sourceDisplayPicture, dataContainer);

        orgDataStore(sourceDisplayPicture.orgId()).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Saves the display picture details into the container for the synced contact
     * </p>
     *
     * @param sourceDisplayPicture Represents the {@link SourceDisplayPicture}
     * @param targetContainer      Represents the {@link DataContainer}
     */
    public void saveDisplayPictureInfo(final SourceDisplayPicture sourceDisplayPicture, final DataContainer targetContainer) {
        final long orgId = sourceDisplayPicture.orgId();
        final Source source = Source.get(sourceDisplayPicture.getSource());
        final Long entityId = sourceDisplayPicture.getEntityId();
        final Row row = getDisplayPictureRow(orgId, source, entityId);
        final Row displayPictureRow = Objects.isNull(row) ? new Row(getDisplayPictureTable()) : row;
        final DisplayPicture originalDisplayPicture = (DisplayPicture) sourceDisplayPicture.getOriginalPicture();
        final DisplayPicture thumbnail = (DisplayPicture) sourceDisplayPicture.getThumbnail();
        final Long pkValue = displayPictureRow.getPKValue();

        if (displayPictureRow.isNewRow()) {
            displayPictureRow.setOrgId(orgId);
            displayPictureRow.set(OMDPDISPLAYPICTURE.SOURCE_ID, source.getId());
            sourceDisplayPicture.setId(pkValue);
        }
        displayPictureRow.set(OMDPDISPLAYPICTURE.ORIGINAL_SIZE, originalDisplayPicture.size());
        displayPictureRow.set(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION, originalDisplayPicture.extension().getId());
        displayPictureRow.set(OMDPDISPLAYPICTURE.THUMBNAIL_SIZE, thumbnail.size());
        displayPictureRow.set(OMDPDISPLAYPICTURE.ORIGINAL_FILE_ID, sourceDisplayPicture.getOriginalPictureId());
        displayPictureRow.set(OMDPDISPLAYPICTURE.THUMBNAIL_FILE_ID, sourceDisplayPicture.getThumbnailId());

        targetContainer.updateRow(displayPictureRow);
        loadMappingRowIntoContainer(orgId, targetContainer, pkValue, source, entityId);
    }

    /**
     * <p>
     * Loads the mapping row into the container based on the source
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param targetContainer Represents the {@link DataContainer}
     * @param pkValue         Represents the pk column value
     * @param source          Represents the {@link Source}
     */
    private void loadMappingRowIntoContainer(final long orgId, final DataContainer targetContainer, final Long pkValue,
                                             final Source source, final Long entityId) {
        switch (source) {
            case CONTACT -> {
                final Table contactDisplayPictureTable = getContactDisplayPictureTable();
                final DataContainer sourceContainer = orgDataStore(orgId).get(contactDisplayPictureTable,
                        new WhereClause(Criteria.eq(contactDisplayPictureTable.getColumn(MCONDISPLAYPICTURE.CONTACT_ID), entityId)));
                final Row row = sourceContainer.isEmpty() ? new Row(contactDisplayPictureTable) : sourceContainer.getRow(contactDisplayPictureTable);

                row.set(MCONDISPLAYPICTURE.CONTACT_ID, entityId);
                row.set(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID, pkValue);
                targetContainer.updateRow(row);
            }
            case COMPANY -> {
                final Table companyDisplayPictureTable = getCompanyDisplayPictureTable();
                final DataContainer sourceContainer = orgDataStore(orgId).get(companyDisplayPictureTable,
                        new WhereClause(Criteria.eq(companyDisplayPictureTable.getColumn(MCOMDISPLAYPICTURE.COMPANY_ID), entityId)));
                final Row row = sourceContainer.isEmpty() ? new Row(companyDisplayPictureTable) : sourceContainer.getRow(companyDisplayPictureTable);

                row.set(MCOMDISPLAYPICTURE.COMPANY_ID, entityId);
                row.set(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID, pkValue);
                targetContainer.updateRow(row);
            }
        }
    }

    /**
     * <p>
     * Gets the display picture based on the given details
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param entityId     Represents the entity id
     * @param source       Represents the {@link Source}
     * @param imageQuality Represents the {@link ProfilePicture.Quality}
     * @return the {@link DisplayPicture}
     */
    public Optional<DisplayPicture> getDisplayPicture(final long orgId, final Long entityId,
                                                      final Source source, final ProfilePicture.Quality imageQuality) {

        final DisplayPicture displayPicture = switch (imageQuality) {
            case THUMBNAIL -> getThumbnailInfo(orgId, source, entityId);
            case ORIGINAL -> getOriginalPicInfo(orgId, source, entityId);
        };

        return Optional.ofNullable(displayPicture);
    }

    /**
     * Gets the display picture id based on the given details.
     *
     * @param orgId        Represents an organization id.
     * @param entityId     Represents the entity id.
     * @param source       Represents the {@link Source}.
     * @param imageQuality Represents the {@link ProfilePicture.Quality}.
     * @return The {@link DisplayPicture} id.
     */
    public final Long getDisplayPictureFileId(final long orgId, final Long entityId, final Source source,
                                              final ProfilePicture.Quality imageQuality) {
        final Row row = getDisplayPictureRow(orgId, source, entityId);

        if (Objects.nonNull(row)) {
            return switch (imageQuality) {
                case ORIGINAL -> row.get(OMDPDISPLAYPICTURE.ORIGINAL_FILE_ID);
                case THUMBNAIL -> row.get(OMDPDISPLAYPICTURE.THUMBNAIL_FILE_ID);
            };
        }

        return null;
    }

    /**
     * Gets the display picture ids based on the given details.
     *
     * @param orgId    Represents an organization id.
     * @param entityId Represents the entity id.
     * @param source   Represents the {@link Source}.
     * @return The {@link DisplayPicture} ids.
     */
    public final Collection<Long> getDisplayPictureIds(final long orgId, final Long entityId, final Source source) {
        final Row row = getDisplayPictureRow(orgId, source, entityId);

        return Objects.isNull(row) ? Collections.emptySet()
                : Set.of(row.get(OMDPDISPLAYPICTURE.ORIGINAL_FILE_ID), row.get(OMDPDISPLAYPICTURE.THUMBNAIL_FILE_ID));
    }

    /**
     * <p>
     * Deletes the display picture based on the given info
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     */
    public void deleteDisplayPictureInfo(final long orgId, final Long entityId, final Source source) {
        final Table displayPictureTable = getDisplayPictureTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(displayPictureTable.getColumn(OMDPDISPLAYPICTURE.SOURCE_ID), source.getId()));
        final DataContainer dataContainer = fetchDisplayPictureContainer(orgId, whereClause, entityId, source);

        if (Objects.nonNull(dataContainer) && !dataContainer.isEmpty()) {
            final DataContainer targetContainer = DataContainer.create();

            targetContainer.deleteRow(dataContainer.getRow(displayPictureTable));
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Updates the thumbnail size for the given display picture
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param displayPictureId Represents the display picture id
     * @param thumbnailSize    Represents the thumbnail size
     */
    public void updateThumbnailSize(final long orgId, final Long displayPictureId, final int thumbnailSize, final int sourceId) {
        final Table table = getDisplayPictureTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMDPDISPLAYPICTURE.THUMBNAIL_SIZE), thumbnailSize);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMDPDISPLAYPICTURE.SOURCE_ID), sourceId))
                .and(Criteria.eq(table.getPKColumn(), displayPictureId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Fetches the display picture container for display picture
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the {@link WhereClause}
     * @param entityId    Represents the entity id
     * @param source      Represents the {@link Source}
     * @return the {@link DataContainer}
     */
    private DataContainer fetchDisplayPictureContainer(final long orgId, final WhereClause whereClause,
                                                       final Long entityId, final Source source) {
        final SelectQuery selectQuery = SelectQuery.get(getDisplayPictureTable());

        return switch (source) {
            case CONTACT -> fetchContactPictureContainer(orgId, selectQuery, whereClause, entityId);
            case COMPANY -> fetchCompanyPictureContainer(orgId, selectQuery, whereClause, entityId);
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the thumbnail info for the given details
     * </p>
     *
     * @param orgId    Represents na organization id
     * @param source   Represents the {@link Source}
     * @param entityId Represents the entity id
     * @return the {@link DisplayPicture}
     */
    private DisplayPicture getThumbnailInfo(final long orgId, final Source source, final Long entityId) {
        final Row row = getDisplayPictureRow(orgId, source, entityId);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.THUMBNAIL);

            displayPicture.setSize(row.get(OMDPDISPLAYPICTURE.THUMBNAIL_SIZE));
            displayPicture.setExtension(ProfilePicture.Extension.get((short) row.get(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION)));

            return displayPicture;
        }
    }

    /**
     * <p>
     * Gets the original picture based on the given details
     * </p>
     *
     * @param orgId    Represents na organization id
     * @param source   Represents the {@link Source}
     * @param entityId Represents the entity id
     * @return the {@link DisplayPicture}
     */
    private DisplayPicture getOriginalPicInfo(final Long orgId, final Source source, final Long entityId) {
        final Row row = getDisplayPictureRow(orgId, source, entityId);

        if (Objects.isNull(row)) {
            return null;
        } else {
            final DisplayPicture displayPicture = new DisplayPicture(ProfilePicture.Quality.ORIGINAL);

            displayPicture.setSize(row.get(OMDPDISPLAYPICTURE.ORIGINAL_SIZE));
            displayPicture.setExtension(ProfilePicture.Extension.get((short) row.get(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION)));

            return displayPicture;
        }
    }

    /**
     * <p>
     * Gets the display pictures based on the given details.
     * </p>
     *
     * @param orgId          Represents an organization id.
     * @param entityIds      Represents the entity ids.
     * @param source         Represents the {@link Source}.
     * @param pictureQuality Represents the {@link ProfilePicture.Quality}.
     * @return The map of entity id and {@link DisplayPicture}.
     */
    public Map<Long, DisplayPicture> getDisplayPicture(final Long orgId, final Collection<Long> entityIds,
                                                       final Source source, final ProfilePicture.Quality pictureQuality) {
        return switch (pictureQuality) {
            case THUMBNAIL -> getThumbnails(orgId, entityIds, source);
            case ORIGINAL -> getOriginalPictures(orgId, entityIds, source);
        };
    }

    /**
     * <p>
     * Gets the thumbnail info for the given details.
     * </p>
     *
     * @param orgId     Represents na organization id.
     * @param entityIds Represents the entity ids.
     * @param source    Represents the {@link Source}.
     * @return The map of entity id and {@link DisplayPicture}.
     */
    private Map<Long, DisplayPicture> getThumbnails(final long orgId, final Collection<Long> entityIds, final Source source) {
        return switch (source) {
            case CONTACT -> getContactDisplayPictures(orgId, entityIds, ProfilePicture.Quality.THUMBNAIL);
            case COMPANY -> getCompanyDisplayPictures(orgId, entityIds, ProfilePicture.Quality.THUMBNAIL);
            default -> Map.of();
        };
    }

    /**
     * <p>
     * Gets the original picture based on the given details.
     * </p>
     *
     * @param orgId     Represents na organization id.
     * @param entityIds Represents the entity ids.
     * @param source    Represents the {@link Source}.
     * @return The map of entity id and {@link DisplayPicture}.
     */
    private Map<Long, DisplayPicture> getOriginalPictures(final Long orgId, final Collection<Long> entityIds, final Source source) {
        return switch (source) {
            case CONTACT -> getContactDisplayPictures(orgId, entityIds, ProfilePicture.Quality.ORIGINAL);
            case COMPANY -> getCompanyDisplayPictures(orgId, entityIds, ProfilePicture.Quality.ORIGINAL);
            default -> Map.of();
        };
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link DisplayPicture}.
     * </p>
     *
     * @param displayPictureRow Holds the details of the {@link DisplayPicture}.
     * @param pictureQuality    Represents the {@link ProfilePicture.Quality}.
     * @return {@link DisplayPicture} that holds the details of the display picture.
     */
    private DisplayPicture convertRowToModel(final Row displayPictureRow, final ProfilePicture.Quality pictureQuality) {
        final DisplayPicture displayPicture = new DisplayPicture(pictureQuality);

        displayPicture.setSize(displayPictureRow.get(OMDPDISPLAYPICTURE.THUMBNAIL_SIZE));
        displayPicture.setExtension(ProfilePicture.Extension.get((short) displayPictureRow.get(OMDPDISPLAYPICTURE.ORIGINAL_EXTENSION)));

        return displayPicture;
    }

    /**
     * <p>
     * Fetches the {@link DisplayPicture} of the given contacts.
     * </p>
     *
     * @param orgId          Represents na organization id.
     * @param contactIds     Represents the contact ids.
     * @param pictureQuality Represents the {@link ProfilePicture.Quality}.
     * @return The map of contact id and {@link DisplayPicture}.
     */
    private Map<Long, DisplayPicture> getContactDisplayPictures(final long orgId, final Collection<Long> contactIds,
                                                                final ProfilePicture.Quality pictureQuality) {
        final Table displayPictureTable = getDisplayPictureTable();
        final Table contactDisplayPictureTable = getContactDisplayPictureTable();
        final SelectQuery selectQuery = SelectQuery.get(displayPictureTable);

        selectQuery.addJoinClause(new JoinClause(displayPictureTable.getPKColumn(),
                contactDisplayPictureTable.getColumn(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactDisplayPictureTable.getColumn(MCONDISPLAYPICTURE.CONTACT_ID), contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, DisplayPicture> displayPictures = new HashMap<>();

        dataContainer.getRows(contactDisplayPictureTable).forEach(contactDisplayPictureRow -> {
            final Row displayPictureRow = dataContainer.getRow(displayPictureTable,
                    (Long) contactDisplayPictureRow.get(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID));

            displayPictures.put(contactDisplayPictureRow.get(MCONDISPLAYPICTURE.CONTACT_ID),
                    convertRowToModel(displayPictureRow, pictureQuality));
        });

        return displayPictures;
    }

    /**
     * <p>
     * Fetches the {@link DisplayPicture} of the given companies.
     * </p>
     *
     * @param orgId          Represents na organization id.
     * @param companyIds     Represents the company ids.
     * @param pictureQuality Represents the {@link ProfilePicture.Quality}.
     * @return The map of company id and {@link DisplayPicture}.
     */
    private Map<Long, DisplayPicture> getCompanyDisplayPictures(final long orgId, final Collection<Long> companyIds,
                                                                final ProfilePicture.Quality pictureQuality) {
        final Table displayPictureTable = getDisplayPictureTable();
        final Table companyDisplayPictureTable = getCompanyDisplayPictureTable();
        final SelectQuery selectQuery = SelectQuery.get(displayPictureTable);

        selectQuery.addJoinClause(new JoinClause(displayPictureTable.getPKColumn(), companyDisplayPictureTable.getColumn(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(companyDisplayPictureTable.getColumn(MCOMDISPLAYPICTURE.COMPANY_ID), companyIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, DisplayPicture> displayPictures = new HashMap<>();

        dataContainer.getRows(companyDisplayPictureTable).forEach(companyDisplayPictureRow -> {
            final Row displayPictureRow = dataContainer.getRow(displayPictureTable,
                    (Long) companyDisplayPictureRow.get(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID));

            displayPictures.put(companyDisplayPictureRow.get(MCOMDISPLAYPICTURE.COMPANY_ID), convertRowToModel(displayPictureRow, pictureQuality));
        });

        return displayPictures;
    }

    /**
     * <p>
     * Fetches the display picture {@link Row} using the given details
     * </p>
     *
     * @param orgId    Represents na organization id
     * @param source   Represents the {@link Source}
     * @param entityId Represents the entity id
     * @return the {@link Row}
     */
    private Row getDisplayPictureRow(final long orgId, final Source source, final Long entityId) {
        final Table displayPictureTable = getDisplayPictureTable();
        final DataContainer dataContainer = fetchDisplayPictureContainer(orgId, null, entityId, source);

        return Objects.nonNull(dataContainer) && !dataContainer.isEmpty() ? dataContainer.getRow(displayPictureTable) : null;
    }

    /**
     * <p>
     * Fetches the contact profile container for the given contact id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param selectQuery Represents the {@link SelectQuery}
     * @param whereClause Represents the {@link WhereClause}
     * @param entityId    Represents the contact id
     * @return the {@link DataContainer}
     */
    private DataContainer fetchContactPictureContainer(final long orgId, final SelectQuery selectQuery, final WhereClause whereClause, final Long entityId) {
        final Table contactDisplayPictureTable = getContactDisplayPictureTable();
        final Criteria criteria = Criteria.eq(contactDisplayPictureTable.getColumn(MCONDISPLAYPICTURE.CONTACT_ID), entityId);

        selectQuery.addJoinClause(new JoinClause(getDisplayPictureTable().getPKColumn(), contactDisplayPictureTable.getColumn(MCONDISPLAYPICTURE.DISPLAY_PICTURE_ID)));
        selectQuery.setWhereClause(Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Fetches the contact profile container for the given company id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param selectQuery Represents the {@link SelectQuery}
     * @param whereClause Represents the {@link WhereClause}
     * @param entityId    Represents the company id
     * @return the {@link DataContainer}
     */
    private DataContainer fetchCompanyPictureContainer(final long orgId, final SelectQuery selectQuery, final WhereClause whereClause, final Long entityId) {
        final Table companyDisplayPictureTable = getCompanyDisplayPictureTable();
        final Criteria criteria = Criteria.eq(companyDisplayPictureTable.getColumn(MCOMDISPLAYPICTURE.COMPANY_ID), entityId);

        selectQuery.addJoinClause(new JoinClause(getDisplayPictureTable().getPKColumn(), companyDisplayPictureTable.getColumn(MCOMDISPLAYPICTURE.DISPLAY_PICTURE_ID)));
        selectQuery.setWhereClause(Objects.isNull(whereClause) ? new WhereClause(criteria) : whereClause.and(criteria));

        return orgDataStore(orgId).get(selectQuery);
    }
}
