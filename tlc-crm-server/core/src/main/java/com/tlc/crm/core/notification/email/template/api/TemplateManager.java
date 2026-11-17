package com.tlc.crm.core.notification.email.template.api;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.Visibility;
import com.tlc.crm.core.notification.email.template.api.model.TemplateMetrics.MetricsType;
import com.tlc.crm.core.notification.email.template.api.model.TemplateMetrics;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;
import com.tlc.crm.core.notification.email.template.api.builder.EmailTemplateQueryBuilder;
import com.tlc.crm.core.notification.email.template.status.TemplateErrorCodes;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.OMEMOUTBOUNDTEMPLATE;
import com.tlc.crm.core.sql.resource.OMEMTAG;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATE;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEATTACHMENT;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDER;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATEFOLDERUSER;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATESTATISTICS;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATETAG;

import com.tlc.crm.email.notification.AbstractNotificationManager;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link EntityMailTemplate} related DB actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 * @see AbstractNotificationManager
 */
public class TemplateManager extends AbstractNotificationManager<EntityMailTemplate> {

    private static final Long DEFAULT_COUNT = 1L;
    private static final String I18_CONTACT_NOT_FOUND = "i18n.contact.not.found";

    private final TemplateTagManager templateTagManager;
    private final DealManager dealManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final MailPlaceHolderExtractor placeHolderExtractor;
    private final EmailTemplateQueryBuilder emailTemplateQueryBuilder;

    private static class Instance {
        private static final TemplateManager INSTANCE = new TemplateManager();
    }

    private TemplateManager() {
        templateTagManager = TemplateTagManager.getInstance();
        dealManager = DealManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
        emailTemplateQueryBuilder = EmailTemplateQueryBuilder.getInstance();
    }

    /**
     * <p>
     * Gets the {@link TemplateManager} Instance
     * </p>
     *
     * @return {@link TemplateManager}
     */
    public static TemplateManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of template
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMTEMPLATE.TABLE);
    }

    /**
     * <p>
     * Fetches the folder user table
     * </p>
     *
     * @return he {@link Table} of template user
     */
    private Table getFolderUserTable() {
        return Table.get(OMEMTEMPLATEFOLDERUSER.TABLE);
    }

    /**
     * <p>
     * Gets the template attachment table
     * </p>
     *
     * @return the {@link Table} of template attachment
     */
    private Table getTemplateAttachmentTable() {
        return Table.get(OMEMTEMPLATEATTACHMENT.TABLE);
    }

    /**
     * <p>
     * Gets the template folder table
     * </p>
     *
     * @return the {@link Table} of template folder
     */
    private Table getTemplateFolderTable() {
        return Table.get(OMEMTEMPLATEFOLDER.TABLE);
    }

    /**
     * <p>
     * Gets the outbound template table
     * </p>
     *
     * @return the {@link Table} of outbound mail with template
     */
    public Table getOutboundTemplateTable() {
        return Table.get(OMEMOUTBOUNDTEMPLATE.TABLE);
    }

    /**
     * <p>
     * Gets the template statistics table
     * </p>
     *
     * @return the {@link Table} of template statistics
     */
    public Table getTemplateStatisticsTable() {
        return Table.get(OMEMTEMPLATESTATISTICS.TABLE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of template ids
     * @return the {@link Collection} of {@link EntityMailTemplate}
     */
    @Override
    public Collection<EntityMailTemplate> get(final Long orgId, final Collection<Long> ids) {
        final Table templateTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(templateTable,
                new WhereClause(Criteria.in(templateTable.getPKColumn(), ids)));
        final Stream<Row> rows = dataContainer.getRows(templateTable);

        return rows.map(row -> convertRowToModel(row, orgId, dataContainer)).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of {@link EntityMailTemplate} ids
     * @return the {@link Collection} of {@link EntityMailTemplate}
     */
    public Collection<EntityMailTemplate> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table templateTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addSelectClause(List.of(templateTable.getColumn(OMEMTEMPLATE.ORG_ID),
                templateTable.getColumn(OMEMTEMPLATE.NAME), templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID)));
        final DataContainer dataContainer = orgDataStore(orgId).get(templateTable,
                new WhereClause(Criteria.in(templateTable.getPKColumn(), ids)));
        final Stream<Row> rows = dataContainer.getRows(templateTable);

        return rows.map(row -> convertRowToModel(row, orgId, dataContainer)).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param template Represents the {@link EntityMailTemplate} that holds the template attributes
     * @return the {@link EntityMailTemplate}
     */
    @Override
    public EntityMailTemplate create(final EntityMailTemplate template) {
        final DataContainer targetContainer = DataContainer.create();

        loadTemplateIntoContainer(true, template, targetContainer);
        orgDataStore(template.orgId()).commitChanges(targetContainer);

        return template;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param templates Represents the {@link Collection} of {@link EntityMailTemplate} that holds the attributes have to be updated
     * @return A {@link Map} which keys are status message and values are update operation details
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<EntityMailTemplate> templates) {
        final Table templateTable = getTable();
        final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
        final Table templateAttachmentTable = getTemplateAttachmentTable();
        final Column templatePkColumn = templateTable.getPKColumn();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Collection<Long> ids = templates.stream().map(EntityMailTemplate::id).filter(Objects::nonNull).toList();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(templatePkColumn, templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID), leftJoin),
                new JoinClause(templatePkColumn, templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(templatePkColumn, ids)));
        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);

        templates.forEach(template -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTemplateIntoContainer(false, template, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(template.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Template update failed Org ID : {} ID : {} reason : {}", orgId, template.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, template.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.common.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Fetches the count of all folders templates available for a specific user within an organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link Long}
     */
    public Long fetchAllFolderTemplatesCount(final Long orgId, final Long userId) {
        final Table templateUserTable = getFolderUserTable();
        final Table folderTable = getTemplateFolderTable();
        final Table templateTable = getTable();
        final Column folderPkColumn = folderTable.getPKColumn();
        final CountQuery countQuery = CountQuery.get(folderTable);

        countQuery.addJoinClause(List.of(new JoinClause(folderPkColumn, templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID)),
                new JoinClause(folderPkColumn, templateUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), JoinClause.JoinType.LEFT)));
        countQuery.setWhereClause((new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId))
                .or(Criteria.eq(templateUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.USER_ID), userId)))
                .or(new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), (int) Visibility.EVERYONE.getId()))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the available templates
     * </p>
     *
     * @param orgId Represents an organization id
     * @param start Represents the lower bound of template to be fetched
     * @param limit Represents the upper bound of template to be fetched
     * @return {@link Collection} of templates
     */
    public Collection<EntityMailTemplate> getTemplateList(final Long orgId, final Long userId, final int start, final int limit) {
        final Table templateTable = getTable();
        final Table folderUserTable = getFolderUserTable();
        final Table templateAttachmentTable = getTemplateAttachmentTable();
        final Table folderTable = getTemplateFolderTable();
        final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
        final Table tagTable = Table.get(OMEMTAG.TABLE);
        final Column templateFolderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);
        final Column templateIdColumn = templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID);
        final Column templateAttachmentIdColumn = templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.ATTACHMENT_ID);
        final Column templatePkColumn = templateTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addJoinClause(List.of(new JoinClause(templateFolderIdColumn,
                        folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), leftJoin),
                new JoinClause(templateFolderIdColumn, folderTable.getPKColumn()),
                new JoinClause(templatePkColumn, templateIdColumn, leftJoin),
                new JoinClause(templatePkColumn, templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID), leftJoin),
                new JoinClause(templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID), tagTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(templateTable.getColumn(OMEMTEMPLATE.NAME), tagTable.getColumn(OMEMTAG.NAME),
                templateTable.getColumn(OMEMTEMPLATE.CREATED_TIME), templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), templateAttachmentTable.getPKColumn(),
                templateAttachmentIdColumn, templateIdColumn, templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID),
                templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.setWhereClause((new WhereClause(Criteria.eq(folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.USER_ID),
                userId)).or(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId)))
                .or(new WhereClause(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.VISIBILITY_TYPE), (int) Visibility.EVERYONE.getId()))));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> templateRows = dataContainer.getRows(templateTable);

        return templateRows.filter(Objects::nonNull).map(row -> convertRowToModel(row, orgId, dataContainer)).toList();
    }

    /**
     * Retrieves a collection of entity mail templates to populate a dropdown list based on the
     * given organization ID, user ID, dropdown limit, and dropdown criteria.
     *
     * @param orgId            The ID of the organization for which the mail templates are to be retrieved.
     * @param userId           The ID of the user for which the mail templates are to be retrieved.
     * @param dropdownLimit    The limit on the number of templates to be retrieved.
     * @param dropdownCriteria The criteria to filter and sort the templates.
     * @return The collection of {@link EntityMailTemplate} representing the mail templates for the dropdown list.
     */
    public Collection<EntityMailTemplate> getTemplateDropdownList(final Long orgId, final Long userId, final DropdownLimit dropdownLimit,
                                                                  final DropdownCriteria dropdownCriteria) {
        final Table templateTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.addSelectClause(templateTable.getColumn(OMEMTEMPLATE.NAME));
        loadFolderJoinsAndCriteria(selectQuery, userId, templateTable, dropdownCriteria);
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        selectQuery.addOrderByClause(new OrderByClause(templateTable.getPKColumn()));

        return orgDataStore(orgId).get(selectQuery).getRows(templateTable).map(row -> {
            final EntityMailTemplate template = new EntityMailTemplate();

            template.setId(row.getPKValue());
            template.setOrgId(orgId);
            template.setName(row.get(OMEMTEMPLATE.NAME));

            return template;
        }).toList();
    }

    /**
     * <p>
     * Fetches the template statistics for the specified folder and user
     * </p>
     *
     * @param folderId Represents the folder id
     * @param userId   Represents the user id
     * @param orgId    Represents an organization id
     * @param start    Represents the lower bound of template to be fetched
     * @param limit    Represents the upper bound of template to be fetched
     * @return {@link Collection} of templates
     */
    public Collection<EntityMailTemplate> getMetrics(final Long folderId, final Long userId, final Long orgId, final int start,
                                                     final int limit) {
        final Table templateTable = getTable();
        final Table folderTable = Table.get(OMEMTEMPLATEFOLDER.TABLE);
        final Table metricsTable = getTemplateStatisticsTable();
        final Table folderUserTable = getFolderUserTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final Column templateFolderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(templateFolderIdColumn,
                        folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), leftJoin),
                new JoinClause(templateFolderIdColumn, folderTable.getPKColumn()),
                new JoinClause(templateTable.getPKColumn(), metricsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID), leftJoin)));
        selectQuery.addSelectClause(List.of(
                templateTable.getColumn(OMEMTEMPLATE.NAME), templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), metricsTable.getColumn(OMEMTEMPLATESTATISTICS.SENT),
                metricsTable.getColumn(OMEMTEMPLATESTATISTICS.REPLIED), metricsTable.getColumn(OMEMTEMPLATESTATISTICS.OPENED),
                metricsTable.getColumn(OMEMTEMPLATESTATISTICS.CLICKED), metricsTable.getColumn(OMEMTEMPLATESTATISTICS.UNSUBSCRIBED),
                metricsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templateFolderIdColumn, folderId))
                .and(Criteria.eq(folderUserTable.getColumn(OMEMTEMPLATEFOLDERUSER.USER_ID), userId))
                .or(Criteria.eq(folderTable.getColumn(OMEMTEMPLATEFOLDER.OWNER_ID), userId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(templateTable).filter(Objects::nonNull)
                .map(row -> convertRowToModel(row, orgId, dataContainer)).toList();
    }

    /**
     * <p>
     * Populates the fields of a given template object from a source template based on their organization id and template id
     * </p>
     *
     * @param template Represents the {@link EntityMailTemplate} that holds template attributes
     */
    public void populateTemplateFromSource(final EntityMailTemplate template) {
        final EntityMailTemplate sourceTemplate = populateEmailDataFromTemplate(template.orgId(), template.id());

        template.setSubject(sourceTemplate.getSubject());
        template.setContent(sourceTemplate.getContent());
        template.setAttachmentIds(sourceTemplate.getAttachmentIds());
        template.setName(sourceTemplate.getName());
        template.setFolderId(sourceTemplate.getFolderId());
        template.setTemplateTags(sourceTemplate.getTemplateTags());
    }

    /**
     * <p>
     * Deletes the templates
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param templates Represents the collection of templates
     */
    public void delete(final Long orgId, final Collection<EntityMailTemplate> templates) {
        final Table templateTable = getTable();
        final Collection<Long> templateIds = templates.stream().map(EntityMailTemplate::id).collect(Collectors.toSet());
        final DeleteQuery templateDeleteQuery = DeleteQuery.get(templateTable);

        templateDeleteQuery.setWhereClause(new WhereClause(Criteria.in(templateTable.getPKColumn(), templateIds)));

        orgDataStore(orgId).delete(templateDeleteQuery);
    }

    /**
     * <p>
     * Transfer the templates from one folder to another
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param id          Represents the current folder id
     * @param newFolderId Represents the moving folder id
     * @param templateIds Represents the collection of template ids
     */
    public void switchFolder(final Long orgId, final Long id, final Long newFolderId, final Collection<Long> templateIds) {
        final Table templateTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), id))
                .and(Criteria.in(templateTable.getPKColumn(), templateIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Column templatePkColumn = templateTable.getPKColumn();
        final Column folderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);
        final Collection<Long> templatePkValues = dataContainer.getRows(templateTable).map(Row::getPKValue).toList();
        final UpdateQuery updateQuery = UpdateQuery.get(templateTable);

        updateQuery.addUpdateColumn(folderIdColumn, newFolderId);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(templatePkColumn, templatePkValues)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Checks the existence of templates ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the collection of template ids
     * @return True means template ids are exists
     */
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        final Table templateTable = getTable();
        final CountQuery countQuery = CountQuery.get(templateTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(templateTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * <p>
     * Checks whether the folder id and template ids are related
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param id          Represents the folder id
     * @param templateIds Represents the collection of template ids
     * @return True means template ids and folder id are exists
     */
    public boolean isFolderAlreadyMapped(final Long orgId, final Long id, final Collection<Long> templateIds) {
        final Table templateTable = getTable();
        final CountQuery countQuery = CountQuery.get(templateTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(templateTable.getPKColumn(), templateIds))
                .and(Criteria.eq(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), id)));

        return orgDataStore(orgId).get(countQuery) == templateIds.size();
    }

    /**
     * <p>
     * Loads template statistics into a data container for a given organization
     * </p>
     * <p>
     *
     * @param orgId           Represents an organization id
     * @param templateId      Represent the template id
     * @param totalRecipients Represent the total recipients count
     * @param targetContainer Holds the updated information of template metrics values
     */
    public void loadTemplateStatisticsIntoContainer(final long orgId, final long templateId, final long totalRecipients,
                                                    final DataContainer targetContainer) {
        final Table templateStatisticsTable = getTemplateStatisticsTable();
        final Row templateStatisticsRow = targetContainer.getRow(templateStatisticsTable, new WhereClause(Criteria
                .eq(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID), templateId)));
        final Row existingStatisticsRow = Objects.isNull(templateStatisticsRow) ? getStatisticsContainer(orgId, templateId).getRow(templateStatisticsTable) : templateStatisticsRow;
        final Row row = Objects.isNull(existingStatisticsRow) ? new Row(templateStatisticsTable) : existingStatisticsRow;

        loadMetricsRowIntoContainer(orgId, templateId, MetricsType.SENT, totalRecipients, row, targetContainer);
    }

    /**
     * <p>
     * Loads template metrics into a data container for a given organization and template
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param templateId      Represent the template id
     * @param metricsType     Represent the {@link TemplateMetrics}
     * @param targetContainer Holds the updated information of template metrics values
     */
    public void loadTemplateMetricsIntoContainer(final long orgId, final long templateId, final MetricsType metricsType,
                                                 final DataContainer targetContainer) {
        final Table templateStatisticsTable = getTemplateStatisticsTable();
        final Row templateStatisticsRow = targetContainer.getRow(templateStatisticsTable, new WhereClause(Criteria
                .eq(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID), templateId)));
        final Row existingStatisticsRow = Objects.isNull(templateStatisticsRow) ? getStatisticsContainer(orgId, templateId).getRow(templateStatisticsTable) : templateStatisticsRow;
        final Row row = Objects.isNull(existingStatisticsRow) ? new Row(templateStatisticsTable) : existingStatisticsRow;

        loadMetricsRowIntoContainer(orgId, templateId, metricsType, DEFAULT_COUNT, row, targetContainer);
    }

    /**
     * <p>
     * Loads template metrics into a data container for a given organization and template
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param templateId      Represent the template id
     * @param metricsType     Represent the {@link TemplateMetrics}
     * @param totalRecipients Represent the total recipients count
     * @param metricsRow      Represent the {@link Row} holds the metrics information of the template
     * @param targetContainer Holds the updated information of template metrics values
     */
    private void loadMetricsRowIntoContainer(final long orgId, final long templateId, final MetricsType metricsType,
                                             final Long totalRecipients, final Row metricsRow, final DataContainer targetContainer) {
        final Table templateStatisticsTable = getTemplateStatisticsTable();
        final Consumer<Column> metricsColumnHandler = (column) -> {
            final String columnName = column.getColumnName();
            final Long existingMetricsCount = metricsRow.get(columnName);

            metricsRow.set(columnName, existingMetricsCount + totalRecipients);
        };

        if (metricsRow.isNewRow()) {
            metricsRow.setOrgId(orgId);
            metricsRow.set(OMEMTEMPLATESTATISTICS.TEMPLATE_ID, templateId);
        }

        switch (metricsType) {
            case SENT -> metricsColumnHandler.accept(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.SENT));
            case OPENED ->
                    metricsColumnHandler.accept(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.OPENED));
            case REPLIED ->
                    metricsColumnHandler.accept(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.REPLIED));
            case CLICKED ->
                    metricsColumnHandler.accept(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.CLICKED));
            case UNSUBSCRIBED ->
                    metricsColumnHandler.accept(templateStatisticsTable.getColumn(OMEMTEMPLATESTATISTICS.UNSUBSCRIBED));
        }

        targetContainer.updateRow(metricsRow);
    }

    /**
     * <p>
     * Gets the template count by folder id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param folderId Represents an folder id
     * @return The {@link Long}
     */
    public Long getTemplatesCountInFolder(final Long orgId, final Long folderId) {
        final Table templateTable = getTable();
        final CountQuery countQuery = CountQuery.get(templateTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID), folderId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Processes the {@link EntityOutboundEmail} of both subject and content to get the placeholders adequate data
     * to replace them in further actions of sending and drafting of mail
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param outboundEmail  Represents the {@link EntityOutboundEmail}
     * @param contactEmailId Represents the email id of a respective entity
     * @param contactId      Represents the contact ID
     */
    public void fillPlaceHolderValues(final long orgId, final EntityOutboundEmail outboundEmail, final Long contactEmailId,
                                      final Long contactId) {
        final EntityMailTemplate template = new EntityMailTemplate();

        template.setOrgId(orgId);
        template.setSubject(outboundEmail.getSubject());
        template.setContent(outboundEmail.getContent());

        if (Objects.nonNull(contactId)) {
            template.setEntityId(contactId);
        } else if (Objects.nonNull(contactEmailId)) {
            final Table contactEmailTable = contactManager.getContactEmailTable();
            final WhereClause contactEmailWhereClause = new WhereClause(Criteria.eq(contactEmailTable
                    .getColumn(MCONEMAILFIELDS.EMAIL_ID), contactEmailId));
            final Row contactEmailRow = orgDataStore(orgId).get(contactEmailTable, contactEmailWhereClause)
                    .getRow(contactEmailTable);

            if (Objects.nonNull(contactEmailRow)) {
                template.setEntityId(contactEmailRow.get(MCONEMAILFIELDS.ENTITY_ID));
            }
        }
        template.setSource(Objects.isNull(template.getEntityId()) ? null : Source.CONTACT);
        final Collection<MailPlaceHolder> mailPlaceHolders = placeHolderExtractor.getPlaceHolders(template);

        outboundEmail.setMailPlaceHolders(mailPlaceHolders);
        template.setPlaceHolders(List.copyOf(mailPlaceHolders));
        outboundEmail.setMailTemplate(template);
    }

    /**
     * <p>
     * Checks whether the template name already exists for the provided organization
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param templateName Represents the template name
     * @return {@link Boolean} true, if template already exists
     */
    public boolean exists(final Long orgId, final String templateName) {
        return exists(orgId, null, templateName);
    }

    /**
     * <p>
     * Checks whether the template name already exists for the provided organization
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param templateId   Represents the ID of the template
     * @param templateName Represents the template name
     * @return {@link Boolean} true, if template already exists
     */
    public boolean exists(final Long orgId, final Long templateId, final String templateName) {
        final Table templateTable = getTable();
        final CountQuery countQuery = CountQuery.get(templateTable);
        final WhereClause nameClause = new WhereClause(Criteria.eq(templateTable.getColumn(OMEMTEMPLATE.NAME), templateName));
        final WhereClause whereClause = Objects.isNull(templateId) ? nameClause :
                 nameClause.and(Criteria.notEq(templateTable.getPKColumn(), templateId));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retrieves a collection of template ids containing a certain search text criteria within a specific organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param searchText Represents the text to search for within folder names
     * @return The {@link Collection} of template ids
     */
    public Collection<Long> searchTemplateIds(final long orgId, final String searchText) {
        final Table templateTable = getTable();
        final Table tagTable = templateTagManager.getTable();
        final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
        final Column templatePkColumn = templateTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final SelectQuery innerQuery = SelectQuery.get(tagTable);

        innerQuery.addSelectClause(templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID));
        innerQuery.addJoinClause(new JoinClause(tagTable.getPKColumn(), templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID)));
        innerQuery.setWhereClause(new WhereClause(Criteria.contains(tagTable.getColumn(OMEMTAG.NAME), searchText)));

        selectQuery.addSelectClause(templatePkColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.contains(templateTable.getColumn(OMEMTEMPLATE.NAME), searchText))
                .or(Criteria.in(templatePkColumn, innerQuery)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptySet() : dataContainer.getRows(templateTable)
                .map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Populates email data from a template based on the specified organization ID and template ID.
     * </p>
     *
     * @return the {@link SelectQuery}
     */
    private EntityMailTemplate populateEmailDataFromTemplate(final Long orgId, final Long templateId) {
        final Table templateTable = getTable();
        final Table templateAttachmentTable = getTemplateAttachmentTable();
        final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
        final Table tagTable = Table.get(OMEMTAG.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(templateTable);
        final Column templatePkColumn = templateTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                new JoinClause(templatePkColumn, templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID), leftJoin),
                new JoinClause(templatePkColumn, templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID), leftJoin),
                new JoinClause(templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID), tagTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(templateTable, templateAttachmentTable, templateTagTable, tagTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(templatePkColumn, templateId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_NOT_FOUND, String.format("Org Id : %d, Template Id : %s", orgId,
                    templateId));
        }

        return convertRowToModel(dataContainer.getRow(templateTable), orgId, dataContainer);
    }

    /**
     * <p>
     * Loads the template data into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is create or an update event
     * @param template        An entity that holds the template attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTemplateIntoContainer(final boolean isCreateEvent, final EntityMailTemplate template, final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null : orgDataStore(template.orgId()).get(getTable(), List.of(template.id()));

        loadTemplateIntoContainer(isCreateEvent, template, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the template data into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is create or an update event
     * @param template        An entity that holds the template attributes
     * @param sourceContainer Holds the field values of existing template
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTemplateIntoContainer(final boolean isCreateEvent, final EntityMailTemplate template, final DataContainer sourceContainer,
                                           final DataContainer targetContainer) {
        final Table templateTable = getTable();
        final Long id = template.id();
        final Long orgId = template.orgId();
        final Collection<Long> attachmentIds = template.getAttachmentIds();
        final Row templateRow = isCreateEvent ? new Row(templateTable) : sourceContainer.getRow(templateTable, id);

        if (!isCreateEvent && Objects.isNull(templateRow)) {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_NOT_FOUND, String.format("Org Id %d Id %d", orgId, id));
        }

        if (templateRow.isNewRow()) {
            template.setId(templateRow.getPKValue());
            templateRow.set(OMEMTEMPLATE.CREATED_TIME, Instant.now().toEpochMilli());
        }
        templateRow.set(OMEMTEMPLATE.NAME, template.getName());
        templateRow.set(OMEMTEMPLATE.CONTENT, template.getContent());
        templateRow.set(OMEMTEMPLATE.SUBJECT, template.getSubject());
        templateRow.set(OMEMTEMPLATE.FOLDER_ID, template.getFolderId());
        final Collection<TemplateTag> templateTags = template.getTemplateTags();

        if (Objects.nonNull(templateTags) && !templateTags.isEmpty()) {
            templateTagManager.loadTemplateTagIntoContainer(isCreateEvent, orgId, template.id(), templateTags,
                    sourceContainer, targetContainer);
        }

        if (Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty()) {
            loadTemplateAttachmentIntoContainer(isCreateEvent, template.id(), attachmentIds, sourceContainer, targetContainer);
        }

        targetContainer.updateRow(templateRow);

        if (!isCreateEvent) {
            templateTagManager.deleteUnmappedTags(template, sourceContainer, targetContainer);
            deleteUnmappedTemplateAttachments(template, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the template attachment data into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which represents whether the event create or not
     * @param templateId      Represents the template id
     * @param attachmentIds   The {@link Collection} of attachment ids
     * @param sourceContainer A source container which contains the rows of existing records.
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadTemplateAttachmentIntoContainer(final boolean isCreateEvent, final Long templateId, final Collection<Long> attachmentIds,
                                                     final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table templateAttachmentTable = getTemplateAttachmentTable();

        for (final Long attachmentId : attachmentIds) {
            final Row templateAttachmentRow;

            if (isCreateEvent) {
                templateAttachmentRow = new Row(templateAttachmentTable);
            } else {
                final Row existingRow = sourceContainer.getRow(templateAttachmentTable,
                        new WhereClause(Criteria.eq(templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID), templateId))
                                .and(Criteria.eq(templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.ATTACHMENT_ID), attachmentId)));
                templateAttachmentRow = Objects.isNull(existingRow) ? new Row(templateAttachmentTable) : existingRow;
            }
            templateAttachmentRow.set(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID, templateId);
            templateAttachmentRow.set(OMEMTEMPLATEATTACHMENT.ATTACHMENT_ID, attachmentId);
            targetContainer.updateRow(templateAttachmentRow);
        }
    }

    /**
     * <p>
     * Deletes the unmapped template attachments for a given template.
     * </p>
     *
     * @param template        Represents the {@link EntityMailTemplate}
     * @param sourceContainer A source container which contains the rows of existing records.
     * @param targetContainer A target container to which all the new/updated rows will be added.
     */
    private void deleteUnmappedTemplateAttachments(final EntityMailTemplate template, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<Attachment> attachments = template.getAttachments();
        final Collection<Long> attachmentIds = Objects.nonNull(attachments) && !attachments.isEmpty()
                ? attachments.stream().map(Attachment::id).collect(Collectors.toUnmodifiableSet()) : null;
        final Table templateAttachmentTable = getTemplateAttachmentTable();
        final WhereClause templateIdWhereclause = new WhereClause(Criteria.eq(templateAttachmentTable
                .getColumn(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID), template.id()));
        final WhereClause whereClause = Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty() ? templateIdWhereclause.and(Criteria
                .notIn(templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.ATTACHMENT_ID), attachmentIds)) : templateIdWhereclause;
        final Stream<Row> rows = sourceContainer.getRows(templateAttachmentTable, whereClause);

        rows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Fetches the contact for the provided contact id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Contact} entity comprising details for the id provided
     */
    public Contact getContact(final long orgId, final long contactId, final Collection<FieldInfo> fieldInfos) {
        if (contactManager.exists(orgId, contactId)) {
            return contactManager.getContactWithTemplateFields(orgId, contactId, fieldInfos);
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, I18_CONTACT_NOT_FOUND);
        }
    }

    /**
     * <p>
     * Fetches the related company for the contact id provided
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Company} mapped with the contact id provided
     */
    public Company getRelatedCompany(final long orgId, final long contactId) {
        if (contactManager.exists(orgId, contactId)) {
            final Long companyId = contactManager.getContactCompanyId(contactId, orgId);

            return Objects.nonNull(companyId) && companyManager.exists(orgId, companyId) ? companyManager.get(orgId, companyId) : null;
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, I18_CONTACT_NOT_FOUND);
        }
    }

    /**
     * <p>
     * Fetches the related deal for the contact id provided
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link Deal} mapped with the contact id provided
     */
    public Deal getRelatedDeal(final long orgId, final long contactId, final Collection<FieldInfo> fieldInfos) {
        if (contactManager.exists(orgId, contactId)) {
            final Long dealId = contactManager.getRecentlyLinkedDealId(orgId, contactId);

            Deal deal = null;

            if (Objects.nonNull(dealId) && dealManager.exists(orgId, dealId)) {
                deal = dealManager.getDealWithTemplateFields(orgId, dealId, fieldInfos);
            }

            return deal;
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, I18_CONTACT_NOT_FOUND);
        }
    }

    /**
     * <p>
     * Fetches the container for the provided template id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param templateId Represents the template id
     * @return {@link DataContainer}
     */
    private DataContainer getStatisticsContainer(final Long orgId, final Long templateId) {
        final Table statisticsTable = getTemplateStatisticsTable();

        return orgDataStore(orgId).get(statisticsTable,
                new WhereClause(Criteria.eq(statisticsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID), templateId)));
    }

    /**
     * <p>
     * Converts the obtained row into {@link EntityMailTemplate}
     * </p>
     *
     * @param row           Represents the {@link EntityMailTemplate} row
     * @param orgId         Represents an organization id
     * @param dataContainer Holds the updated information of template values
     * @return the {@link EntityMailTemplate}
     */
    public EntityMailTemplate convertRowToModel(final Row row, final Long orgId, final DataContainer dataContainer) {
        final EntityMailTemplate template = new EntityMailTemplate();
        final Long templateId = row.getPKValue();

        template.setId(templateId);
        template.setOrgId(orgId);
        template.setName(row.get(OMEMTEMPLATE.NAME));
        template.setSubject(row.get(OMEMTEMPLATE.SUBJECT));
        template.setCreatedTime(row.get(OMEMTEMPLATE.CREATED_TIME));
        template.setContent(row.get(OMEMTEMPLATE.CONTENT));
        template.setFolderId(row.get(OMEMTEMPLATE.FOLDER_ID));

        if (!dataContainer.isEmpty()) {
            final Table templateAttachmentTable = getTemplateAttachmentTable();
            final Stream<Row> templateAttachmentRows = dataContainer.getRows(templateAttachmentTable, new WhereClause(Criteria.eq(
                    templateAttachmentTable.getColumn(OMEMTEMPLATEATTACHMENT.TEMPLATE_ID), templateId)));

            template.setAttachmentIds(templateAttachmentRows.map(attachmentRow ->
                    (Long) attachmentRow.get(OMEMTEMPLATEATTACHMENT.ATTACHMENT_ID)).collect(Collectors.toUnmodifiableSet()));

            final Table metricsTable = getTemplateStatisticsTable();
            final Row metricsRow = dataContainer.getRow(metricsTable,
                    new WhereClause(Criteria.eq(metricsTable.getColumn(OMEMTEMPLATESTATISTICS.TEMPLATE_ID), templateId)));
            final TemplateMetrics metrics = new TemplateMetrics();

            if (Objects.nonNull(metricsRow)) {
                metrics.setId(metricsRow.getPKValue());
                metrics.setSentCount(metricsRow.get(OMEMTEMPLATESTATISTICS.SENT));
                metrics.setOpenedCount(metricsRow.get(OMEMTEMPLATESTATISTICS.OPENED));
                metrics.setClickedCount(metricsRow.get(OMEMTEMPLATESTATISTICS.CLICKED));
                metrics.setRepliedCount(metricsRow.get(OMEMTEMPLATESTATISTICS.REPLIED));
                metrics.setUnsubscribedCount(metricsRow.get(OMEMTEMPLATESTATISTICS.UNSUBSCRIBED));
            }

            template.setTemplateMetrics(metrics);

            final Table templateTagTable = Table.get(OMEMTEMPLATETAG.TABLE);
            final Table tagTable = Table.get(OMEMTAG.TABLE);
            final Stream<Row> templateTagRows = dataContainer.getRows(templateTagTable, new WhereClause(Criteria.eq(
                    templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID), templateId)));
            final Collection<Long> tagIds = templateTagRows.map(templateTagRow -> (Long)
                    templateTagRow.get(OMEMTEMPLATETAG.TAG_ID)).toList();

            if (!tagIds.isEmpty()) {
                final Stream<Row> tagRows = dataContainer.getRows(tagTable, new WhereClause(Criteria.in(
                        tagTable.getPKColumn(), tagIds)));

                template.setTemplateTags(tagRows.map(templateTagManager::convertRowToModel).toList());
            }
        }

        return template;
    }

    /**
     * <p>
     * Retrieves the total template count of the user accessible folders
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param userId           Represents a user ID
     * @param dropdownCriteria Represents the dropdown criteria
     * @return The count of entities
     */
    public long totalCount(final Long orgId, final Long userId, final DropdownCriteria dropdownCriteria) {
        final Table templateTable = getTable();
        final CountQuery countQuery = CountQuery.get(templateTable);

        loadFolderJoinsAndCriteria(countQuery, userId, templateTable, dropdownCriteria);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Adds the join clause and where clause to the query
     * </p>
     *
     * @param multiQuery        Represents the {@link MultiQuery}
     * @param userId            Represents a user ID
     * @param templateTable     Represents the {@link Table} of template
     * @param dropdownCriteria  Represents the {@link DropdownCriteria}
     */
    private void loadFolderJoinsAndCriteria(final MultiQuery multiQuery, final Long userId, final Table templateTable, final DropdownCriteria dropdownCriteria) {
        final Table folderTable = getTemplateFolderTable();
        final Column templateFolderIdColumn = templateTable.getColumn(OMEMTEMPLATE.FOLDER_ID);

        multiQuery.addJoinClause(List.of(
                new JoinClause(templateFolderIdColumn, getFolderUserTable().getColumn(OMEMTEMPLATEFOLDERUSER.FOLDER_ID), JoinClause.JoinType.LEFT),
                new JoinClause(templateFolderIdColumn, folderTable.getPKColumn()))
        );
        multiQuery.setWhereClause(buildDropDownClause(emailTemplateQueryBuilder.getAllFolderCriteria(userId, folderTable), dropdownCriteria, templateTable));
    }

    /**
     * <p>
     * Gets the {@link WhereClause} on the dropdown criteria
     * </p>
     *
     * @param whereClause        Represents the {@link WhereClause}
     * @param dropDownCriteria   Represents the {@link DropdownCriteria}
     * @param templateTable      Represents the {@link Table}
     * @return the {@link WhereClause} based on the dropdown criteria
     */
    private WhereClause buildDropDownClause(final WhereClause whereClause, final DropdownCriteria dropDownCriteria, final Table templateTable) {
        return Objects.isNull(dropDownCriteria) ? whereClause :
                whereClause.and(new WhereClause(dropDownCriteria.build(templateTable.getColumn(OMEMTEMPLATE.NAME))));

    }
}