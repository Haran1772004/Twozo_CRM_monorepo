package com.tlc.crm.core.features.api.tag;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.model.SourceTags;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.sql.resource.MTCOMPANYTAG;
import com.tlc.crm.core.sql.resource.MTCONTACTTAG;
import com.tlc.crm.core.sql.resource.MTDEALTAG;
import com.tlc.crm.core.sql.resource.MTPRODUCTTAG;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.entity.features.internal.status.TagErrorCodes;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.service.TagService;
import com.tlc.crm.entity.sql.resource.OMTTAG;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.LinkedHashSet;

import static com.tlc.crm.fields.models.StoreType.TAGS;

/**
 * <p>
 * Implements the {@link Tag} related DB actions
 * </p>
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class SourceTagManager {

    private static final I18nKey TAG_KEY = new I18nKey("i18n.tags.display.name");

    private final TagManager tagManager;
    private final AuditLogger auditLogger;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final ProductManager productManager;
    private final TagService tagService;

    private SourceTagManager() {
        tagManager = TagManager.getInstance();
        auditLogger = AuditLogger.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        productManager = ProductManager.getInstance();
        tagService = TagService.getInstance();
    }

    private static class Instance {
        private static final SourceTagManager INSTANCE = new SourceTagManager();
    }

    /**
     * <p>
     * Gets the {@link SourceTagManager} Instance
     * </p>
     *
     * @return {@link SourceTagManager}
     */
    public static SourceTagManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the org data store
     * </p>
     *
     * @param orgId Represents the organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the tag table
     * </p>
     *
     * @return the {@link Table} of tag
     */
    public Table getTable() {
        return Table.get(OMTTAG.TABLE);
    }

    /**
     * <p>
     * Gets the deal tag table
     * </p>
     *
     * @return the {@link Table} of deal tag
     */
    public Table getDealTagTable() {
        return Table.get(MTDEALTAG.TABLE);
    }

    /**
     * <p>
     * Gets the contact tag table
     * </p>
     *
     * @return the {@link Table} of contact tag
     */
    public Table getContactTagTable() {
        return Table.get(MTCONTACTTAG.TABLE);
    }

    /**
     * <p>
     * Gets the company tag table
     * </p>
     *
     * @return the {@link Table} of company tag
     */
    public Table getCompanyTagTable() {
        return Table.get(MTCOMPANYTAG.TABLE);
    }

    /**
     * <p>
     * Gets the product tag table
     * </p>
     *
     * @return the {@link Table} of product tag
     */
    private Table getProductTagTable() {
        return Table.get(MTPRODUCTTAG.TABLE);
    }

    /**
     * <p>
     * Loads the source id and tag id into the data container
     * </p>
     *
     * @param sourceId         Represents the source id
     * @param tagId            Represents the tagId id
     * @param sourceColumnName Represents the source column name
     * @param tagColumnName    Represents the tag column name
     * @param tagOrder         Represents the order of the tag
     * @param table            Represents the table
     * @param dataContainer    Holds the updated information of user defined and system field values
     */
    private void loadSourceTagIntoContainer(final Long tagId, final Long sourceId, final String sourceColumnName,
                                            final String tagColumnName, final short tagOrder, final Table table, final DataContainer dataContainer) {
        final Row row = new Row(table);

        row.set(sourceColumnName, sourceId);
        row.set(tagColumnName, tagId);
        row.set(TagFields.ORDER.name(), tagOrder);

        dataContainer.addNewRow(row);
    }

    /**
     * <p>
     * Maps the {@link Set} of tag with source
     * </p>
     *
     * @param orgId      Represents the Organisation ID
     * @param sourceTags Holds the data for mapping tags with entities
     */
    public void mapTagsWithSource(final Long orgId, final SourceTags sourceTags) {
        try {
            final Collection<Tag> newTags = sourceTags.getNewTags();
            final DataContainer targetContainer = DataContainer.create();

            if (Objects.nonNull(newTags) && !newTags.isEmpty()) {
                final Table tagTable = getTable();
                final Set<String> tagNames = newTags.stream().map(Tag::getName).collect(Collectors.toCollection(LinkedHashSet::new));
                final DataContainer entityContainer = orgDataStore(orgId).get(tagTable, getTagNameCondition(tagNames));
                final Column tagNameColumn = tagTable.getColumn(OMTTAG.NAME);

                entityContainer.indexRows(tagNameColumn);

                for (final Tag newTag : newTags) {
                    final Row existingTagRow = entityContainer.getIndexedRow(tagNameColumn, newTag.getName());

                    if (Objects.nonNull(existingTagRow)) {
                        sourceTags.addTagId(existingTagRow.getPKValue());

                        if (Objects.nonNull(newTag.getColor())) {
                            existingTagRow.set(OMTTAG.COLOR, newTag.getColor().id());
                            existingTagRow.set(OMTTAG.UPDATED_TIME, Instant.now().toEpochMilli());
                            targetContainer.updateRow(existingTagRow);
                        }
                        continue;
                    }
                    tagManager.loadTagIntoContainer(true, newTag, null, targetContainer);

                    final Long tagId = newTag.id();
                    final Row tagRow = targetContainer.getRow(tagTable, tagId);

                    if (Objects.nonNull(tagRow)) {
                        tagRow.set(OMTTAG.LAST_USED, Instant.now().toEpochMilli());

                        if (Objects.nonNull(newTag.getColor())) {
                            tagRow.set(OMTTAG.COLOR, newTag.getColor().id());
                            tagRow.set(OMTTAG.UPDATED_TIME, Instant.now().toEpochMilli());
                        }
                    }
                    sourceTags.addTagId(tagId);
                }
            }
            final String sourceName = sourceTags.getSource().getName();
            final Set<Long> entityIds = sourceTags.getEntityIds();
            final Set<Long> tagIds = new LinkedHashSet<>(sourceTags.getTagIds());
            final Map<Long, String> entities = fetchEntityNames(orgId, sourceName, entityIds);

            switch (sourceName) {
                case Contact.ENTITY_TYPE -> {
                    for (final Long entityId : entityIds) {
                        mapTagsWithContacts(orgId, tagIds, entityId, targetContainer);
                    }
                }
                case Deal.ENTITY_TYPE -> {
                    for (final Long entityId : entityIds) {
                        mapTagsWithDeals(orgId, tagIds, entityId, targetContainer);
                    }
                }
                case Company.ENTITY_TYPE -> {
                    for (final Long entityId : entityIds) {
                        mapTagsWithCompanies(orgId, tagIds, entityId, targetContainer);
                    }
                }
                case Product.ENTITY_TYPE -> {
                    for (final Long entityId : entityIds) {
                        mapTagsWithProducts(orgId, tagIds, entityId, targetContainer);
                    }
                }
            }

            orgDataStore(orgId).commitChanges(targetContainer);

            if (Objects.nonNull(entities)) {
                final Collection<String> tags = tagManager.partialGet(orgId, tagIds).stream().map(tag ->
                        tag.getName().toString()).collect(Collectors.toSet());
                final AuditAttribute auditAttribute = new AuditAttribute(TAG_KEY, null, tags, AuditAction.ADDED, TAGS);
                final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
                final AuditAction auditAction = AuditAction.UPDATED;

                for (final Map.Entry<Long, String> entity : entities.entrySet()) {
                    auditLogDataList.add(new AuditLogData(sourceTags.getUserId(), entity.getKey(), entity.getValue(), AuditSource.get(sourceName), auditAction, auditAttribute));
                }
                auditLogger.log(orgId, auditLogDataList);
            }
        } catch (ErrorCode errorCode) {
            throw ErrorCode.get(TagErrorCodes.TAG_MAPPING_FAILED);
        }
    }

    /**
     * <p>
     * Deletes the mapped tag
     * </p>
     *
     * @param orgId      Represents the Organization id
     * @param userId     Represents the user id
     * @param tagIds     Represents the {@link Set} of tag id
     * @param entityIds  Represents the {@link Set} of entity id
     * @param sourceName Represents the name of the source
     */
    public void deleteMappedTag(final Long orgId, final long userId, final Set<Long> tagIds, final Set<Long> entityIds, final String sourceName) {
        final Map<Long, String> entities = fetchEntityNames(orgId, sourceName, entityIds);

        final BiConsumer<Table, WhereClause> tagsRemover = (table, whereClause) -> {
            if (tagIds.isEmpty()) {
                tagIds.addAll(getMappedTagIds(orgId, sourceName, table, entityIds));
            }
            deleteMappedTag(orgId, table, whereClause);
        };

        switch (sourceName) {
            case Contact.ENTITY_TYPE -> tagsRemover.accept(getContactTagTable(), getContactTagCondition(entityIds, tagIds));
            case Deal.ENTITY_TYPE -> tagsRemover.accept(getDealTagTable(), getDealTagCondition(entityIds, tagIds));
            case Company.ENTITY_TYPE -> tagsRemover.accept(getCompanyTagTable(), getCompanyTagCondition(entityIds, tagIds));
            case Product.ENTITY_TYPE -> tagsRemover.accept(getProductTagTable(), getProductTagCondition(entityIds, tagIds));
        }

        if (Objects.nonNull(entities)) {
            final Collection<String> tags = tagManager.partialGet(orgId, tagIds).stream().map(tag ->
                    tag.getName().toString()).collect(Collectors.toSet());
            final AuditAttribute auditAttribute = new AuditAttribute(TAG_KEY, tags, null, AuditAction.REMOVED, TAGS);
            final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
            final AuditAction auditAction = AuditAction.UPDATED;

            for (final Map.Entry<Long, String> entity : entities.entrySet()) {
                auditLogDataList.add(new AuditLogData(userId, entity.getKey(), entity.getValue(), AuditSource.get(sourceName), auditAction, auditAttribute));
            }
            auditLogger.log(orgId, auditLogDataList);
        }
    }

    /**
     * <p>
     * Deletes the tag
     * </p>
     *
     * @param orgId       Represents Organization id
     * @param table       Represents the table name
     * @param whereClause which contains the condition to delete tag
     */
    private void deleteMappedTag(final Long orgId, final Table table, final WhereClause whereClause) {
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(whereClause);
        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * Retrieves the where clause for the given tag name
     * </p>
     *
     * @param tagNames represents the {@link Collection} of tag name
     * @return {@link WhereClause} for tag name
     */
    private WhereClause getTagNameCondition(final Collection<String> tagNames) {
        return new WhereClause(Criteria.in(getTable().getColumn(OMTTAG.NAME), tagNames));
    }

    /**
     * <p>
     * Maps the given {@link Set} of tag id with  contact id
     * </p>
     *
     * @param orgId           Represents the Organization id
     * @param tagIds          Represents the {@link Set} tag id
     * @param contactId       Represents the contact id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void mapTagsWithContacts(final Long orgId, final Set<Long> tagIds, final Long contactId, final DataContainer targetContainer) {
        final Table contactTagTable = getContactTagTable();
        final Set<Long> mappedTagIds = getMappedTagIds(orgId, contactId, contactTagTable, MTCONTACTTAG.CONTACT_ID, MTCONTACTTAG.TAG_ID);
        final Counter orderIncrement = new Counter((short) (mappedTagIds.size() + NumericalConstant.ONE));

        for (final Long tagId : tagIds) {

            if (mappedTagIds.isEmpty() || !mappedTagIds.contains(tagId)) {
                loadSourceTagIntoContainer(tagId, contactId, MTCONTACTTAG.CONTACT_ID, MTCONTACTTAG.TAG_ID, (short) orderIncrement.getAndIncrement(), contactTagTable, targetContainer);
            }
        }
        loadTagRowIntoContainer(orgId, targetContainer, tagIds);
    }

    /**
     * <p>
     * Loads the tag values into data container
     * </p>
     *
     * @param orgId           Represents the organisation id
     * @param targetContainer Holds the updated information of user defined and system field values
     * @param tagIds          Represents the tag ids
     */
    private void loadTagRowIntoContainer(final Long orgId, final DataContainer targetContainer, final Set<Long> tagIds) {
        final Table tagTable = tagManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(tagTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(tagTable.getPKColumn(), tagIds)));
        final Stream<Row> tagRows = orgDataStore(orgId).get(selectQuery).getRows(tagTable);

        tagRows.forEach(tagRow -> {
            tagRow.set(OMTTAG.LAST_USED, Instant.now().toEpochMilli());
            targetContainer.updateRow(tagRow);
        });
    }

    /**
     * <p>
     * Maps the given {@link Set} of tag id with given deal id
     * </p>
     *
     * @param orgId           Represents the Organization id
     * @param tagIds          Represents the {@link Set} of tag id
     * @param dealId          Represents the deal id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void mapTagsWithDeals(final Long orgId, final Set<Long> tagIds, final Long dealId, final DataContainer targetContainer) {
        final Table dealTagTable = getDealTagTable();
        final Set<Long> mappedTagIds = getMappedTagIds(orgId, dealId, dealTagTable, MTDEALTAG.DEAL_ID, MTDEALTAG.TAG_ID);
        final Counter orderIncrement = new Counter((short) (mappedTagIds.size() + NumericalConstant.ONE));

        for (final Long tagId : tagIds) {

            if (mappedTagIds.isEmpty() || !mappedTagIds.contains(tagId)) {
                loadSourceTagIntoContainer(tagId, dealId, MTDEALTAG.DEAL_ID, MTDEALTAG.TAG_ID, (short) orderIncrement.getAndIncrement(), dealTagTable, targetContainer);
            }
        }
        loadTagRowIntoContainer(orgId, targetContainer, tagIds);
    }

    /**
     * <p>
     * Gets the {@link Set} of mapped tag id
     * </p>
     *
     * @param orgId            Represents the Organization id
     * @param sourceId         Represents the source id
     * @param table            Represents the mapping table
     * @param sourceColumnName Represents source column name
     * @param tagColumnName    Represents the tag column name
     * @return {@link Set} of tag mapped id
     */
    private Set<Long> getMappedTagIds(final Long orgId, final Long sourceId, final Table table,
                                      final String sourceColumnName, final String tagColumnName) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(tagColumnName));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(sourceColumnName), sourceId)));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(table);

        return rows.filter(Objects::nonNull).map(row -> (Long) row.get(tagColumnName)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Maps the given {@link Set} of tag id with product id
     * </p>
     *
     * @param orgId           Represents the Organization id
     * @param tagIds          Represents the tag ids
     * @param productId       Represents the product id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void mapTagsWithProducts(final Long orgId, final Set<Long> tagIds, final Long productId, final DataContainer targetContainer) {
        final Table productTagTable = getProductTagTable();
        final Set<Long> mappedTagIds = getMappedTagIds(orgId, productId, productTagTable, MTPRODUCTTAG.PRODUCT_ID, MTPRODUCTTAG.TAG_ID);
        final Counter orderIncrement = new Counter((short) (mappedTagIds.size() + NumericalConstant.ONE));

        for (final Long tagId : tagIds) {

            if (mappedTagIds.isEmpty() || !mappedTagIds.contains(tagId)) {
                loadSourceTagIntoContainer(tagId, productId, MTPRODUCTTAG.PRODUCT_ID, MTPRODUCTTAG.TAG_ID, (short) orderIncrement.getAndIncrement(), productTagTable, targetContainer);
            }
        }
        loadTagRowIntoContainer(orgId, targetContainer, tagIds);
    }

    /**
     * <p>
     * Maps the given {@link Set} of tag id with company id
     * </p>
     *
     * @param orgId           Represents the Organization id
     * @param tagIds          Represents the tag ids
     * @param companyId       Represents the company id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void mapTagsWithCompanies(final Long orgId, final Set<Long> tagIds, final Long companyId, final DataContainer targetContainer) {
        final Table companyTagTable = getCompanyTagTable();
        final Set<Long> mappedTagIds = getMappedTagIds(orgId, companyId, companyTagTable, MTCOMPANYTAG.COMPANY_ID, MTCOMPANYTAG.TAG_ID);
        final Counter orderIncrement = new Counter((short) (mappedTagIds.size() + NumericalConstant.ONE));

        for (final Long tagId : tagIds) {

            if (mappedTagIds.isEmpty() || !mappedTagIds.contains(tagId)) {
                loadSourceTagIntoContainer(tagId, companyId, MTCOMPANYTAG.COMPANY_ID, MTCOMPANYTAG.TAG_ID, (short) orderIncrement.getAndIncrement(), companyTagTable, targetContainer);
            }
        }
        loadTagRowIntoContainer(orgId, targetContainer, tagIds);
    }

    /**
     * <p>
     * Retrieves the Where clause for the given {@link Set} of tag id and {@link Set} of contact id
     * </p>
     *
     * @param contactIds Represents the {@link Set} of contact id
     * @param tagIds     Represents the {@link Set} of tag id
     * @return {@link WhereClause} for contact tag
     */
    private WhereClause getContactTagCondition(final Set<Long> contactIds, final Set<Long> tagIds) {
        final Table contactTagTable = getContactTagTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID), contactIds));

        return tagIds.isEmpty() ? whereClause : whereClause.and(Criteria.in(contactTagTable.getColumn(MTCONTACTTAG.TAG_ID), tagIds));
    }

    /**
     * <p>
     * Retrieves the where clause for the given {@link Set} of tag id and {@link Set} of deal id
     * </p>
     *
     * @param dealIds Represents the {@link Set} of deal id
     * @param tagIds  Represents the {@link Set} of tag id
     * @return {@link WhereClause} for deal tag
     */
    private WhereClause getDealTagCondition(final Set<Long> dealIds, final Set<Long> tagIds) {
        final Table dealTagTable = getDealTagTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(dealTagTable.getColumn(MTDEALTAG.DEAL_ID), dealIds));

        return tagIds.isEmpty() ? whereClause : whereClause.and(Criteria.in(dealTagTable.getColumn(MTDEALTAG.TAG_ID), tagIds));
    }

    /**
     * <p>
     * Retrieves the where clause for the given {@link Set} of tag id and {@link Set} of product id
     * </p>
     *
     * @param productIds Represents the {@link Set} of product id
     * @param tagIds     Represents the {@link Set} of tag id
     * @return {@link WhereClause} for product tag
     */
    private WhereClause getProductTagCondition(final Set<Long> productIds, final Set<Long> tagIds) {
        final Table productTagTable = getProductTagTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(productTagTable.getColumn(MTPRODUCTTAG.PRODUCT_ID), productIds));

        return tagIds.isEmpty() ? whereClause : whereClause.and(Criteria.in(productTagTable.getColumn(MTPRODUCTTAG.TAG_ID), tagIds));
    }

    /**
     * <p>
     * Retrieves the where clause for the given {@link Set} of tag id and {@link Set} of company id
     * </p>
     *
     * @param companyIds Represents the {@link Set} of company id
     * @param tagIds     Represents the {@link Set}of tag id
     * @return {@link WhereClause} for company tag
     */
    private WhereClause getCompanyTagCondition(final Set<Long> companyIds, final Set<Long> tagIds) {
        final Table companyTagTable = getCompanyTagTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID), companyIds));

        return tagIds.isEmpty() ? whereClause : whereClause.and(Criteria.in(companyTagTable.getColumn(MTCOMPANYTAG.TAG_ID), tagIds));
    }

    /**
     * <p>
     * Checks whether the {@link Set} of tag id exits or not
     * </p>
     *
     * @param orgId  Represents the Organization id
     * @param tagIds Represents the tag ids
     * @return true if the given tag ids are exits
     */
    public boolean checkAllTagIdsExists(final Long orgId, final Set<Long> tagIds) {
        final Table tagTable = getTable();
        final CountQuery countQuery = CountQuery.get(tagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(tagTable.getPKColumn(), tagIds)));

        return orgDataStore(orgId).get(countQuery) == tagIds.size();
    }

    /**
     * <p>
     * Checks whether any of the given {@link Collection} of tag names already exists
     * </p>
     *
     * @param orgId    Represents the Organisation ID
     * @param tagNames Represents the {@link Collection} of tag name
     * @return true if any of the given {@link Collection} of tag name is already exists, false otherwise
     */
    public boolean doesAnyTagAlreadyExist(final Long orgId, final Collection<String> tagNames) {
        final CountQuery countQuery = CountQuery.get(getTable());

        countQuery.setWhereClause(getTagNameCondition(tagNames));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Gets the {@link Collection} of {@link Tag} associated with a specific source within an organization.
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param source   Represents the source
     * @return {@link Collection} of {@link Tag}
     */
    public Collection<Tag> getMappedTags(final long orgId, final Long entityId, final String source) {
        final Table tagTable = getTable();
        final Column tagPkColumn = tagTable.getPKColumn();
        final String tagIdKey = TagFields.TAG_ID.name();
        final SelectQuery selectQuery = SelectQuery.get(tagTable);
        final BiConsumer<Table, String> sourceTagQuery = (sourceTagTable, entityIdColumn) -> {
            selectQuery.addJoinClause(new JoinClause(tagPkColumn, sourceTagTable.getColumn(tagIdKey)));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(sourceTagTable.getColumn(entityIdColumn), entityId)));
            selectQuery.addOrderByClause(new OrderByClause(sourceTagTable.getColumn(TagFields.ORDER.name())));
        };

        switch (source) {
            case Contact.ENTITY_TYPE -> sourceTagQuery.accept(getContactTagTable(), MTCONTACTTAG.CONTACT_ID);
            case Company.ENTITY_TYPE -> sourceTagQuery.accept(getCompanyTagTable(), MTCOMPANYTAG.COMPANY_ID);
            case Deal.ENTITY_TYPE -> sourceTagQuery.accept(getDealTagTable(), MTDEALTAG.DEAL_ID);
            case Product.ENTITY_TYPE -> sourceTagQuery.accept(getProductTagTable(), MTPRODUCTTAG.PRODUCT_ID);
        }

        return orgDataStore(orgId).get(selectQuery).getRows(tagTable).map(row -> tagManager.convertRowToModel(row, null)).toList();
    }

    /**
     * <p>
     * Fetches the collection of mapped tag ids for the entity ids
     * </p>
     *
     * @param source    Represents the source
     * @param table     Represents the tag mapping table
     * @param entityIds Which contains the collections of entity ids
     * @return The collections of tag ids
     */
    public Collection<Long> getMappedTagIds(final Long orgId, final String source, final Table table, final Collection<Long> entityIds) {
        final BiFunction<WhereClause, String, Collection<Long>> tagIdCollector = (whereClause, tagIdColumn) -> {
            final DataContainer dataContainer = orgDataStore(orgId).get(table, whereClause);

            return dataContainer.getRows(table).map(row -> (Long) row.get(tagIdColumn)).collect(Collectors.toUnmodifiableSet());
        };

        return switch (source) {
            case Contact.ENTITY_TYPE -> tagIdCollector.apply(new WhereClause(Criteria.in(table
                    .getColumn(MTCONTACTTAG.CONTACT_ID), entityIds)), MTCONTACTTAG.TAG_ID);
            case Company.ENTITY_TYPE -> tagIdCollector.apply(new WhereClause(Criteria.in(table
                    .getColumn(MTCOMPANYTAG.COMPANY_ID), entityIds)), MTCOMPANYTAG.TAG_ID);
            case Deal.ENTITY_TYPE -> tagIdCollector.apply(new WhereClause(Criteria.in(table
                    .getColumn(MTDEALTAG.DEAL_ID), entityIds)), MTDEALTAG.TAG_ID);
            case Product.ENTITY_TYPE -> tagIdCollector.apply(new WhereClause(Criteria.in(table
                    .getColumn(MTPRODUCTTAG.PRODUCT_ID), entityIds)), MTPRODUCTTAG.TAG_ID);
            default -> throw ErrorCode.get(TagErrorCodes.INVALID_SOURCE, "i18n.tag.invalid.source");
        };
    }

    /**
     * <p>
     * Fetches the names of the entity by using id.
     * </p>
     *
     * @param orgId      Represents the organisation id.
     * @param sourceName Represents the name of the source.
     * @param entityIds  Represents the {@link Set} of entity id.
     * @return Entity names.
     */
    private Map<Long, String> fetchEntityNames(final Long orgId, final String sourceName, final Collection<Long> entityIds) {
        return switch (sourceName) {
            case Contact.ENTITY_TYPE -> contactManager.partialGet(orgId, entityIds).stream()
                    .collect(Collectors.toMap(Contact::id, Contact::getFullName));
            case Company.ENTITY_TYPE -> companyManager.partialGet(orgId, entityIds).stream()
                    .collect(Collectors.toMap(Company::id, Company::getName));
            case Deal.ENTITY_TYPE -> DealManager.getInstance().partialGet(orgId, entityIds).stream()
                    .collect(Collectors.toMap(Deal::id, Deal::getTitle));
            case Product.ENTITY_TYPE -> productManager.partialGet(orgId, entityIds).stream()
                    .collect(Collectors.toMap(Product::id, Product::getName));
            default -> null;
        };
    }

    /**
     * <p>
     * Loads the tag-related join clauses and select clause for a given source name
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param sourceTable Represents the source table
     * @param selectQuery Represents the {@link SelectQuery}
     */
    public void loadJoinsAndSelectClause(final Source source, final Table sourceTable, final SelectQuery selectQuery) {
        final Table tagTable = getTable();
        final JoinClause.JoinType leftJoinType = JoinClause.JoinType.LEFT;

        switch (source) {
            case CONTACT -> {
                final Table contactTagTable = getContactTagTable();

                selectQuery.addJoinClause(List.of(new JoinClause(sourceTable.getPKColumn(),
                                contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID), leftJoinType),
                        new JoinClause(contactTagTable.getColumn(MTCONTACTTAG.TAG_ID), tagTable.getPKColumn(), leftJoinType)));
                selectQuery.addSelectClause(tagTable, contactTagTable);
            }
            case COMPANY -> {
                final Table companyTagTable = getCompanyTagTable();

                selectQuery.addJoinClause(List.of(new JoinClause(sourceTable.getPKColumn(),
                                companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID), leftJoinType),
                        new JoinClause(companyTagTable.getColumn(MTCOMPANYTAG.TAG_ID), tagTable.getPKColumn(), leftJoinType)));
                selectQuery.addSelectClause(tagTable, companyTagTable);
            }
            case DEAL -> {
                final Table dealTagTable = getDealTagTable();

                selectQuery.addJoinClause(List.of(new JoinClause(sourceTable.getPKColumn(),
                                dealTagTable.getColumn(MTDEALTAG.DEAL_ID), leftJoinType),
                        new JoinClause(dealTagTable.getColumn(MTDEALTAG.TAG_ID), tagTable.getPKColumn(), leftJoinType)));
                selectQuery.addSelectClause(tagTable, dealTagTable);
            }
            default -> throw ErrorCode.get(TagErrorCodes.INVALID_SOURCE, "i18n.tag.invalid.source");
        }
    }

    /**
     * Converts the row into {@link Tag}
     *
     * @param entityId      Represents the entity id
     * @param source        Represents the {@link Source}
     * @param dataContainer A container that holds the user defined source values
     */
    public Collection<Tag> buildTags(final Long entityId, final Source source, final DataContainer dataContainer) {
        final Table tagTable = tagManager.getTable();
        final List<Tag> tags = new ArrayList<>();
        switch (source) {
            case CONTACT -> {
                final Table contactTagTable = getContactTagTable();
                dataContainer.getRows(contactTagTable, new WhereClause(Criteria.eq(contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID), entityId)))
                        .forEach(row -> {
                            final Row tagRow = dataContainer.getRow(tagTable, (Long) row.get(MTCONTACTTAG.TAG_ID));

                            tags.add(tagManager.convertRowToModel(tagRow, row.get(MTCONTACTTAG.ORDER)));
                        });
            }
            case COMPANY -> {
                final Table companyTagTable = getCompanyTagTable();dataContainer.getRows(companyTagTable, new WhereClause(Criteria.eq(companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID), entityId)))
                        .forEach(row -> {
                            final Row tagRow = dataContainer.getRow(tagTable, (Long) row.get(MTCOMPANYTAG.TAG_ID));

                            tags.add(tagManager.convertRowToModel(tagRow, row.get(MTCOMPANYTAG.ORDER)));
                        });
            }
            case DEAL -> {
                final Table dealTagTable = getDealTagTable();dataContainer.getRows(dealTagTable, new WhereClause(Criteria.eq(dealTagTable.getColumn(MTDEALTAG.DEAL_ID), entityId)))
                        .forEach(row -> {
                            final Row tagRow = dataContainer.getRow(tagTable, (Long) row.get(MTDEALTAG.TAG_ID));

                            tags.add(tagManager.convertRowToModel(tagRow, row.get(MTDEALTAG.ORDER)));
                        });
            }
            default -> throw ErrorCode.get(TagErrorCodes.INVALID_SOURCE, "i18n.tag.invalid.source");
        }
        tags.sort(Comparator.comparing(Tag::getOrder));

        return tags;
    }

    /**
     * <p>
     * Maps the given tag id with entity source
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgId           Represents an organization id
     * @param entityId        Represents the entity id
     * @param tags            which holds the {@link Collection} of {@link Tag}
     * @param source          Represents the {@link Source}
     * @param sourceContainer The source data container containing existing tag information
     * @param targetContainer The target data container where the tag information will be loaded
     */
    public void mapTagsWithSource(final boolean isCreateEvent, final long orgId, final Long entityId,
                                  final Collection<Tag> tags, final Source source,
                                  final DataContainer sourceContainer, final DataContainer targetContainer) {
        if (Objects.nonNull(entityId)) {
            final List<Tag> orderedTags = new ArrayList<>(tags);
            final Counter orderCounter = new Counter(NumericalConstant.ONE);

            for (final Tag tag : orderedTags) {
                tagService.resolveExistingTag(orgId, tag);

                if (Objects.isNull(tag.id())) {
                    tagManager.create(tag);
                } else if (Objects.nonNull(tag.getColor())) {
                    final Tag existingTag = tagManager.get(orgId, tag.id());

                    if (!Objects.equals(existingTag.getColor(), tag.getColor())) {
                        tagManager.update(tag);
                    }
                }
                final short order = (short) orderCounter.getAndIncrement();

                switch (source) {
                    case CONTACT ->
                            mapTagsWithContact(isCreateEvent, entityId, tag, order, sourceContainer, targetContainer);
                    case DEAL -> mapTagsWithDeal(isCreateEvent, entityId, tag, order, sourceContainer, targetContainer);
                    case COMPANY ->
                            mapTagsWithCompany(isCreateEvent, entityId, tag, order, sourceContainer, targetContainer);
                    default -> throw ErrorCode.get(TagErrorCodes.INVALID_SOURCE, "i18n.tag.invalid.source");
                }
            }
            final Set<Long> tagIds = orderedTags.stream().map(Tag::id).collect(Collectors.toUnmodifiableSet());

            loadTagRowIntoContainer(orgId, targetContainer, tagIds);
        }
    }

    /**
     * <p>
     * Maps the given tag id with contact id
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param contactId       Represents the {@link Contact} id
     * @param tag           Represents the {@link Tag} id
     * @param order           Represents the order of the tag
     * @param sourceContainer The source data container containing existing tag information
     * @param targetContainer The target data container where the tag information will be loaded
     */
    private void mapTagsWithContact(final boolean isCreateEvent, final Long contactId, final Tag tag, final short order,
                                    final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table contactTagTable = getContactTagTable();
        final Column contactIdColumn = contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID);
        final Column tagIdColumn = contactTagTable.getColumn(MTCONTACTTAG.TAG_ID);
        final Long tagId = tag.id();

        if (!isCreateEvent && isTagAlreadyMapped(sourceContainer, contactTagTable, contactIdColumn, tagIdColumn, contactId, tagId, order)) {
            return;
        }
        final Row row = isCreateEvent || Objects.isNull(sourceContainer) ? new Row(contactTagTable) : sourceContainer.getOrInitRow(contactTagTable,
                new WhereClause(Criteria.eq(contactIdColumn, contactId)).and(Criteria.eq(tagIdColumn, tagId)));

        if (row.isNewRow()) {
            row.set(MTCONTACTTAG.CONTACT_ID, contactId);
            row.set(MTCONTACTTAG.TAG_ID, tagId);
        }
        row.set(TagFields.ORDER.name(), order);

        targetContainer.updateRow(row);
    }

    /**
     * Checks whether the tagId is already associated with the provided entity ID in the same order
     *
     * @param sourceContainer Container containing existing entity and tag mapping rows
     * @param sourceTagTable  Specified source's tag mapping table
     * @param entityIdColumn  Entity ID column of the sourceTagTable
     * @param tagIdColumn     Tag ID column of the sourceTagTable
     * @param entityId        Entity ID to check
     * @param tagId           Tag ID to check
     * @param order           Represents the order of the tag
     * @return true if the entityId is already mapped with the given tag ID in the same order, otherwise false
     */
    private boolean isTagAlreadyMapped(final DataContainer sourceContainer, final Table sourceTagTable,
                                       final Column entityIdColumn, final Column tagIdColumn, final long entityId, final long tagId, final short order) {
        final Row row = sourceContainer.getRow(sourceTagTable, new WhereClause(Criteria.eq(entityIdColumn, entityId))
                .and(Criteria.eq(tagIdColumn, tagId))
                .and(Criteria.eq(sourceTagTable.getColumn(TagFields.ORDER.name()), order)));

        return Objects.nonNull(row);
    }

    /**
     * <p>
     * Maps the given tag id with deal id
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param dealId          Represents the {@link Deal} id
     * @param tag           Represents the {@link Tag} id
     * @param order           Represents the order of the tag
     * @param sourceContainer The source data container containing existing tag information
     * @param targetContainer The target data container where the tag information will be loaded
     */
    private void mapTagsWithDeal(final boolean isCreateEvent, final Long dealId, final Tag tag, final short order,
                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table dealTagTable = getDealTagTable();
        final Column dealIdColumn = dealTagTable.getColumn(MTDEALTAG.DEAL_ID);
        final Column tagIdColumn = dealTagTable.getColumn(MTDEALTAG.TAG_ID);
        final Long tagId = tag.id();

        if (!isCreateEvent && isTagAlreadyMapped(sourceContainer, dealTagTable, dealIdColumn, tagIdColumn, dealId, tagId, order)) {
            return;
        }
        final Row row = Objects.isNull(sourceContainer) ? new Row(dealTagTable) : sourceContainer.getOrInitRow(dealTagTable,
                new WhereClause(Criteria.eq(dealIdColumn, dealId)).and(Criteria.eq(tagIdColumn, tagId)));

        if (row.isNewRow()) {
            row.set(MTDEALTAG.DEAL_ID, dealId);
            row.set(MTDEALTAG.TAG_ID, tagId);
        }
        row.set(TagFields.ORDER.name(), order);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Maps the given tag id with company id
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param companyId       Represents the {@link Company} id
     * @param tag           Represents the {@link Tag} id
     * @param order           Represents the order of the tag
     * @param sourceContainer The source data container containing existing tag information
     * @param targetContainer The target data container where the tag information will be loaded
     */
    private void mapTagsWithCompany(final boolean isCreateEvent, final Long companyId, final Tag tag, final short order,
                                    final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table companyTagTable = getCompanyTagTable();
        final Column companyIdColumn = companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID);
        final Column tagIdColumn = companyTagTable.getColumn(MTCOMPANYTAG.TAG_ID);
        final Long tagId = tag.id();

        if (!isCreateEvent && isTagAlreadyMapped(sourceContainer, companyTagTable, companyIdColumn, tagIdColumn, companyId, tagId, order)) {
            return;
        }
        final Row row = Objects.isNull(sourceContainer) ? new Row(companyTagTable) : sourceContainer.getOrInitRow(companyTagTable,
                new WhereClause(Criteria.eq(companyIdColumn, companyId)).and(Criteria.eq(tagIdColumn, tagId)));

        if (row.isNewRow()) {
            row.set(MTCOMPANYTAG.COMPANY_ID, companyId);
            row.set(MTCOMPANYTAG.TAG_ID, tagId);
        }
        row.set(TagFields.ORDER.name(), order);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Deletes the mapped tag for the given source name
     * </p>
     *
     * @param entityId        Represents the entity id
     * @param tags            Represents the {@link Collection} of {@link Tag}
     * @param source          Represents the {@link Source}
     * @param sourceContainer The source data container containing existing tag information
     * @param targetContainer The target data container where the tag information will be loaded
     */
    public void deleteMappedTags(final long entityId, final Collection<Tag> tags, final Source source,
                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Set<Long> tagIds = tags.stream().map(Tag::id).collect(Collectors.toUnmodifiableSet());

        switch (source) {
            case CONTACT -> {
                final Table contactTagTable = getContactTagTable();
                final Criteria criteria = Criteria.eq(contactTagTable.getColumn(MTCONTACTTAG.CONTACT_ID), entityId);
                final WhereClause whereClause = tagIds.isEmpty() ? new WhereClause(criteria) :
                        new WhereClause(criteria).and(Criteria.notIn(contactTagTable.getColumn(MTCONTACTTAG.TAG_ID), tagIds));

                sourceContainer.getRows(contactTagTable, whereClause).forEach(targetContainer::deleteRow);
            }
            case DEAL -> {
                final Table dealTagTable = getDealTagTable();
                final Criteria criteria = Criteria.eq(dealTagTable.getColumn(MTDEALTAG.DEAL_ID), entityId);
                final WhereClause whereClause = tagIds.isEmpty() ? new WhereClause(criteria) :
                        new WhereClause(criteria).and(Criteria.notIn(dealTagTable.getColumn(MTDEALTAG.TAG_ID), tagIds));

                sourceContainer.getRows(dealTagTable, whereClause).forEach(targetContainer::deleteRow);
            }
            case COMPANY -> {
                final Table companyTagTable = getCompanyTagTable();
                final Criteria criteria = Criteria.eq(companyTagTable.getColumn(MTCOMPANYTAG.COMPANY_ID), entityId);
                final WhereClause whereClause = tagIds.isEmpty() ? new WhereClause(criteria) :
                        new WhereClause(criteria).and(Criteria.notIn(companyTagTable.getColumn(MTCOMPANYTAG.TAG_ID), tagIds));

                sourceContainer.getRows(companyTagTable, whereClause).forEach(targetContainer::deleteRow);
            }
            default -> throw ErrorCode.get(TagErrorCodes.INVALID_SOURCE, "i18n.tag.invalid.source");
        }
    }

    /**

     * Gets the entity IDs mapped to the given tag from the specified mapping table.
     *
     * @param orgId          Organization ID.
     * @param mappingTable   Table containing the tag-to-entity mapping.
     * @param entityIdColumn Column containing the entity ID.
     * @param tagId          Tag ID used to find the mapped entities.
     * @return Set of entity IDs mapped to the given tag.
     */
    private Set<Long> getEntityIdsForTag(final Long orgId, final Table mappingTable, final String entityIdColumn,
                                         final Long tagId) {
        final SelectQuery selectQuery = SelectQuery.get(mappingTable);

        selectQuery.addSelectClause(mappingTable.getColumn(entityIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(mappingTable.getColumn(MTCOMPANYTAG.TAG_ID), tagId)));

        return orgDataStore(orgId).get(selectQuery).getRows(mappingTable).map(row -> (Long) row.get(entityIdColumn)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Fetches the Deals, Contacts and Companies that have the given tag applied, grouped by source.
     * </p>
     *
     * @param orgId Represents the organisation id
     * @param tagId Represents the tag id
     * @return Map of source name to a collection of {id, name} json objects
     */
    public Map<String, Collection<JsonObject>> getEntitiesByTag(final Long orgId, final Long tagId) {
        final Map<String, Collection<JsonObject>> result = new LinkedHashMap<>();
        final Set<Long> dealIds = getEntityIdsForTag(orgId, getDealTagTable(), MTDEALTAG.DEAL_ID, tagId);
        final Set<Long> contactIds = getEntityIdsForTag(orgId, getContactTagTable(), MTCONTACTTAG.CONTACT_ID, tagId);
        final Set<Long> companyIds = getEntityIdsForTag(orgId, getCompanyTagTable(), MTCOMPANYTAG.COMPANY_ID, tagId);

        if (!dealIds.isEmpty()) {
            result.put(Deal.ENTITY_TYPE, DealManager.getInstance().partialGet(orgId, dealIds).stream()
                    .map(deal -> Json.object().put(DealFields.ID, deal.id()).put(DealFields.TITLE, deal.getTitle())).toList());
        }

        if (!contactIds.isEmpty()) {
            result.put(Contact.ENTITY_TYPE, contactManager.partialGet(orgId, contactIds).stream()
                    .map(contact -> Json.object().put(ContactFields.ID, contact.id()).put(ContactFields.NAME, contact.getFullName())).toList());
        }

        if (!companyIds.isEmpty()) {
            result.put(Company.ENTITY_TYPE, companyManager.partialGet(orgId, companyIds).stream()
                    .map(company -> Json.object().put(CompanyFields.ID, company.id()).put(CompanyFields.NAME, company.getName())).toList());
        }

        return result;
    }
}
