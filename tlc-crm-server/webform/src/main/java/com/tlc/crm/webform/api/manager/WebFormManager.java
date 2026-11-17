package com.tlc.crm.webform.api.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Language;

import com.tlc.crm.webform.url.UrlGenerator;
import com.tlc.file.common.queue.AttachmentRemovalPublisher;
import com.tlc.i18n.I18nKey;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.JoinClause.JoinType;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.webform.internal.status.WebFormErrorCodes;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.api.model.FormFieldProperty;
import com.tlc.crm.webform.api.model.FormField;
import com.tlc.crm.webform.api.model.FormElement;
import com.tlc.crm.webform.api.model.FormStyle;
import com.tlc.crm.webform.api.model.FieldStyle;
import com.tlc.crm.webform.api.model.SubmitButtonStyle;
import com.tlc.crm.webform.api.model.RecipientEmail;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.FormFieldProperty.Type;
import com.tlc.crm.webform.api.model.Alignment;
import com.tlc.crm.webform.api.model.Font;
import com.tlc.crm.webform.sql.resource.MWFSUBMITACTION;
import com.tlc.crm.webform.sql.resource.OMWFWEBFORM;
import com.tlc.crm.webform.sql.resource.MWFEMAIL;
import com.tlc.crm.webform.sql.resource.MWFFORMFIELDS;
import com.tlc.crm.webform.sql.resource.MWFSTYLE;
import com.tlc.crm.webform.sql.resource.MWFFORMELEMENTS;
import com.tlc.crm.webform.sql.resource.MWFFORMSTYLE;
import com.tlc.crm.webform.sql.resource.MWFFIELDSTYLE;
import com.tlc.crm.webform.sql.resource.MWFSUBMITBUTTONSTYLE;
import com.tlc.crm.webform.sql.resource.MWFTEMPLATEFIELDS;
import com.tlc.crm.webform.field.WebFormFields;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;

import java.util.Collection;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link WebForm} related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class WebFormManager extends AbstractConfigManager<WebForm> {

    private static final String IMAGE_ELEMENT_NAME = "Image";

    private final EntityHandler entityHandler;
    private final UrlGenerator urlGenerator;
    private final AttachmentRemovalPublisher attachmentRemovalPublisher;

    private WebFormManager() {
        this.entityHandler = EntityHandler.getInstance();
        urlGenerator = UrlGenerator.get();
        attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
    }

    private static class Instance {
        private static final WebFormManager INSTANCE = new WebFormManager();
    }

    /**
     * Gets the {@link WebFormManager} Instance.
     *
     * @return {@link WebFormManager}
     */
    public static WebFormManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table} of webForm.
     */
    @Override
    public Table getTable() {
        return Table.get(OMWFWEBFORM.TABLE);
    }

    /**
     * Gets the {@link FormStyle} table.
     *
     * @return The {@link Table} of form style.
     */
    public Table getFormStyleTable() {
        return Table.get(MWFFORMSTYLE.TABLE);
    }

    /**
     * Gets the {@link FieldStyle} table.
     *
     * @return The {@link Table} of field style.
     */
    public Table getFieldStyleTable() {
        return Table.get(MWFFIELDSTYLE.TABLE);
    }

    /**
     * Gets the {@link SubmitButtonStyle} table.
     *
     * @return The {@link Table} of submit button style.
     */
    public Table getSubmitButtonStyleTable() {
        return Table.get(MWFSUBMITBUTTONSTYLE.TABLE);
    }

    /**
     * Gets the {@link RecipientEmail} table.
     *
     * @return The {@link Table} of email style.
     */
    private Table getFormEmailTable() {
        return Table.get(MWFEMAIL.TABLE);
    }

    /**
     * Gets the style table.
     *
     * @return The {@link Table} of style.
     */
    public Table getStyleTable() {
        return Table.get(MWFSTYLE.TABLE);
    }

    /**
     * Gets the {@link FormField} table.
     *
     * @return The {@link Table} of form field.
     */
    private Table getFormFieldTable() {
        return Table.get(MWFFORMFIELDS.TABLE);
    }

    /**
     * Gets the {@link FormElement} table.
     *
     * @return The {@link Table} of form element.
     */
    private Table getFormElementTable() {
        return Table.get(MWFFORMELEMENTS.TABLE);
    }

    /**
     * Gets the {@link SubmitAction} table.
     *
     * @return The {@link Table} of submit action.
     */
    public Table getSubmitActionTable() {
        return Table.get(MWFSUBMITACTION.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Refers the organization id.
     * @param ids   Refers the {@link WebForm} entity ids.
     * @return Collection of {@link WebForm}.
     */
    @Override
    public Collection<WebForm> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final DataContainer dataContainer = getDataContainer(orgId, ids);
        final Stream<Row> rows = dataContainer.getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NOT_FOUND, "i18n.webFrom.not.found");
        } else {
            return rows.map(row -> convertRowToModel(row, dataContainer, false)).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of ids.
     * @return
     */
    @Override
    public Collection<WebForm> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Column pkColumn = table.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);

        loadSelectAndJoinClauseToFetchData(selectQuery);
        selectQuery.addSelectClause(List.of(pkColumn, table.getColumn(OMWFWEBFORM.NAME), table.getColumn(OMWFWEBFORM.OWNER_ID), table.getColumn(OMWFWEBFORM.ORG_ID),
                table.getColumn(OMWFWEBFORM.VISITS), table.getColumn(OMWFWEBFORM.SUBMISSIONS),
                table.getColumn(OMWFWEBFORM.CONVERSIONS), table.getColumn(OMWFWEBFORM.IS_ACTIVE),
                table.getColumn(OMWFWEBFORM.SUBMIT_ACTION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return rows.map(row -> convertRowToPartialModel(row, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Refers the organization id.
     * @param webForms Refers the collection of {@link WebForm}.
     * @return
     */
    @Override
    public ProcessOutcome create(final Long orgId, final Collection<WebForm> webForms) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        for (final WebForm webForm : webForms) {

            try {
                create(webForm);

                final Map<String, Object> succeededEntry = new HashMap<>();
                final Long id = webForm.id();

                succeededEntry.put("id", id);
                succeededEntry.put("link", webForm.getUrl());
                succeededEntry.put("iframeLink", urlGenerator.getIframeUrl(webForm.orgId(), webForm.getOwnerId(), id));
                processOutcome.addProcessedRecord(succeededEntry);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Create action: Web form create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                        "i18n.common.create.failed")));
            }
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedEntries));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param webForm An entity that holds the web form attributes which have to be saved into database.
     */
    @Override
    public WebForm create(final WebForm webForm) {
        final DataContainer dataContainer = DataContainer.create();

        loadWebFormDetailsIntoContainer(true, webForm, null, dataContainer);
        orgDataStore(webForm.orgId()).commitChanges(dataContainer);

        return webForm;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Refers the organization id.
     * @param webForms Collection of {@link WebForm}.
     * @return
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<WebForm> webForms) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = webForms.stream().map(WebForm::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = getDataContainer(orgId, ids);

        webForms.forEach(webForm -> {
            try {
                final DataContainer dataContainer = DataContainer.create();

                loadWebFormDetailsIntoContainer(false, webForm, entityContainer, dataContainer);
                orgDataStore.commitChanges(dataContainer);
                processOutcome.addProcessedRecord(webForm.id());
            } catch (ErrorCode error) {
                LOGGER.error("Update action: Web form update failed Org ID : {} ID : {} reason : {}", orgId, webForm.id(), error.getMessage(), error);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, webForm.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Refers the organization id.
     * @param webForms Collection of {@link WebForm}.
     */
    @Override
    public final void delete(final Long orgId, final Collection<WebForm> webForms) {
        if (Objects.nonNull(webForms) && !webForms.isEmpty()) {
            final Table mainTable = getTable();
            final Table formStyleTable = getFormStyleTable();
            final Table fieldStyleTable = getFieldStyleTable();
            final Table submitButtonStyleTable = getSubmitButtonStyleTable();
            final Table submitActionTable = getSubmitActionTable();
            final Set<Long> webFormIds = new HashSet<>();
            final Set<Long> submitActionIds = new HashSet<>();
            final Set<Long> formStyleIds = new HashSet<>();
            final Set<Long> fieldStyleIds = new HashSet<>();
            final Set<Long> submitButtonStyleIds = new HashSet<>();
            final Set<Long> imageAttachmentIds = new HashSet<>();

            for (final WebForm webForm : webForms) {
                webFormIds.add(webForm.id());
                submitActionIds.add(webForm.getSubmitAction().id());
                formStyleIds.add(webForm.getFormStyle().id());
                fieldStyleIds.add(webForm.getFieldStyle().id());
                submitButtonStyleIds.add(webForm.getSubmitButtonStyle().id());
                collectImageAttachmentIds(webForm, imageAttachmentIds);
            }
            final List<DeleteQuery> deleteQueries = new ArrayList<>();

            deleteQueries.add(entityHandler.buildEntityDeleteQuery(mainTable, new WhereClause(Criteria
                    .in(mainTable.getPKColumn(), webFormIds))));
            deleteQueries.add(entityHandler.buildEntityDeleteQuery(submitActionTable,
                    new WhereClause(Criteria.in(submitActionTable.getPKColumn(), submitActionIds))));
            deleteQueries.add(entityHandler.buildEntityDeleteQuery(fieldStyleTable,
                    new WhereClause(Criteria.in(fieldStyleTable.getPKColumn(), fieldStyleIds))));
            deleteQueries.add(entityHandler.buildEntityDeleteQuery(formStyleTable,
                    new WhereClause(Criteria.in(formStyleTable.getPKColumn(), formStyleIds))));
            deleteQueries.add(entityHandler.buildEntityDeleteQuery(submitButtonStyleTable,
                    new WhereClause(Criteria.in(submitButtonStyleTable.getPKColumn(), submitButtonStyleIds))));

            orgDataStore(orgId).delete(deleteQueries);

            if (!imageAttachmentIds.isEmpty()) {
                attachmentRemovalPublisher.publish(String.valueOf(orgId), imageAttachmentIds);
            }
        }
    }

    /**
     * Collects all {@code attachmentId} values from Image {@link FormElement} configs within the given {@link WebForm}.
     *
     * @param webForm       Refers the {@link WebForm}.
     * @param attachmentIds The mutable set to collect attachment IDs into.
     */
    private void collectImageAttachmentIds(final WebForm webForm, final Set<Long> attachmentIds) {
        for (final FormFieldProperty formFieldProperty : webForm.getFormFieldProperties()) {
            if (formFieldProperty instanceof final FormElement formElement
                    && FormElement.Category.IMAGE == formElement.getCategory()) {
                final Object rawId = formElement.getConfigAsJson().opt(WebFormFields.ATTACHMENT_ID.getName());

                if (Objects.nonNull(rawId)) {
                    attachmentIds.add(((Number) rawId).longValue());
                }
            }
        }
    }

    /**
     * Gets the web form by its primary ID
     *
     * @param id Represents the web form ID
     * @return the {@link WebForm}
     */
    public Optional<WebForm> get(final Long id) {
        final Table table = getTable();
        final Column pkColumn = table.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);

        loadSelectAndJoinClauseToFetchData(selectQuery);
        selectQuery.addSelectClause(List.of(pkColumn, table.getColumn(OMWFWEBFORM.OWNER_ID), table.getColumn(OMWFWEBFORM.ORG_ID),
                table.getColumn(OMWFWEBFORM.VISITS), table.getColumn(OMWFWEBFORM.SUBMISSIONS),
                table.getColumn(OMWFWEBFORM.CONVERSIONS), table.getColumn(OMWFWEBFORM.IS_ACTIVE),
                table.getColumn(OMWFWEBFORM.SUBMIT_ACTION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(pkColumn, id)));

        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Row webFormRow = dataContainer.getRow(table, id);

        return Objects.isNull(webFormRow) ? Optional.empty() : Optional.of(convertRowToModel(webFormRow, dataContainer, false));
    }

    /**
     * Fetches the {@link DataContainer} of web forms of given ids.
     *
     * @param orgId Refers the organization id.
     * @param ids   Refers the web form id.
     * @return The {@link DataContainer} of web forms.
     */
    private DataContainer getDataContainer(final Long orgId, final Collection<Long> ids) {
        final Table mainTable = getTable();
        final Table styletable = getStyleTable();
        final Table formStyleTable = getFormStyleTable();
        final Table fieldStyleTable = getFieldStyleTable();
        final Table submitButtonStyleTable = getSubmitButtonStyleTable();
        final Table formElementTable = getFormElementTable();
        final Column pkColumn = mainTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(formStyleTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(formStyleTable, styletable, fieldStyleTable, submitButtonStyleTable, mainTable));
        selectQuery.addJoinClause(new JoinClause(pkColumn, formElementTable.getColumn(MWFFORMELEMENTS.WEB_FORM_ID), JoinType.LEFT));
        selectQuery.addSelectClause(mainTable, styletable, formElementTable, formStyleTable,
                fieldStyleTable, submitButtonStyleTable);
        loadSelectAndJoinClauseToFetchData(selectQuery);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, ids)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Loads the select and join clauses into the {@link SelectQuery}.
     *
     * @param selectQuery Refers the {@link SelectQuery} in which the select and join clauses are loaded.
     */
    private void loadSelectAndJoinClauseToFetchData(final SelectQuery selectQuery) {
        final Table table = getTable();
        final Table formFieldTable = getFormFieldTable();
        final Table submitActionTable = getSubmitActionTable();
        final Table webFormEmailTable = getFormEmailTable();
        final Column pkColumn = table.getPKColumn();

        selectQuery.addJoinClause(List.of(new JoinClause(pkColumn, webFormEmailTable.getColumn(MWFEMAIL.WEB_FORM_ID), JoinType.LEFT),
                new JoinClause(pkColumn, formFieldTable.getColumn(MWFFORMFIELDS.WEB_FORM_ID)),
                new JoinClause(table.getColumn(OMWFWEBFORM.SUBMIT_ACTION_ID), submitActionTable.getPKColumn())));
        selectQuery.addSelectClause(formFieldTable, webFormEmailTable, submitActionTable);
    }

    /**
     * Loads the {@link WebForm} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webForm         An entity that holds the {@link WebForm} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadWebFormDetailsIntoContainer(final boolean isCreateEvent, final WebForm webForm,
                                                 final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table webFormTable = getTable();
        final Long id = webForm.id();
        final Long orgId = webForm.orgId();
        final Row webFormRow = isCreateEvent ? new Row(webFormTable) : entityContainer.getRow(webFormTable, id);
        final long currentTime = Instant.now().toEpochMilli();

        if (!isCreateEvent && Objects.isNull(webFormRow)) {
            throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NOT_FOUND, String.format("Org ID : %d, ID : %d", orgId, id));
        }

        if (webFormRow.isNewRow()) {
            webFormRow.setOrgId(orgId);
            webForm.setId(webFormRow.getPKValue());
            webFormRow.set(OMWFWEBFORM.CREATED_TIME, currentTime);
        }
        loadStylesIntoContainer(isCreateEvent, webForm, entityContainer, dataContainer);
        loadFieldPropertiesIntoContainer(isCreateEvent, webForm, entityContainer, dataContainer);
        loadSubmitActionIntoContainer(isCreateEvent, webForm, entityContainer, dataContainer);

        webFormRow.set(OMWFWEBFORM.NAME, webForm.getName());
        webFormRow.set(OMWFWEBFORM.DESCRIPTION, webForm.getDescription());
        webFormRow.set(OMWFWEBFORM.SUBMIT_BUTTON_NAME, webForm.getSubmitButtonName());
        webFormRow.set(OMWFWEBFORM.IS_ACTIVE, webForm.isActive());
        webFormRow.set(OMWFWEBFORM.TEMPLATE_ID, webForm.getTemplateId());
        webFormRow.set(OMWFWEBFORM.LANGUAGE_ID, webForm.getLanguage().getId());
        webFormRow.set(OMWFWEBFORM.MODIFIED_TIME, currentTime);
        webFormRow.set(OMWFWEBFORM.OWNER_ID, webForm.getOwnerId());
        webFormRow.set(OMWFWEBFORM.ATTACHMENT_ID, webForm.getAttachmentId());
        webFormRow.set(OMWFWEBFORM.SUBMIT_ACTION_ID, webForm.getSubmitAction().id());

        if (isCreateEvent) {
            webFormRow.set(OMWFWEBFORM.VISITS, webForm.getVisits());
            webFormRow.set(OMWFWEBFORM.SUBMISSIONS, webForm.getSubmissions());
            webFormRow.set(OMWFWEBFORM.CONVERSIONS, webForm.getConversions());
            webFormRow.set(OMWFWEBFORM.STYLE_ID, webForm.getStyleId());
        } else {
            webFormRow.set(OMWFWEBFORM.VISITS, webFormRow.get(OMWFWEBFORM.VISITS));
            webFormRow.set(OMWFWEBFORM.SUBMISSIONS, webFormRow.get(OMWFWEBFORM.SUBMISSIONS));
            webFormRow.set(OMWFWEBFORM.CONVERSIONS, webFormRow.get(OMWFWEBFORM.CONVERSIONS));
            webFormRow.set(OMWFWEBFORM.STYLE_ID, webFormRow.get(OMWFWEBFORM.STYLE_ID));
        }

        dataContainer.updateRow(webFormRow);

        if (!webFormRow.isNewRow()) {
            deleteUnmappedFieldProperties(webForm, entityContainer, dataContainer);
        }
    }

    /**
     * Loads the webForm styles into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webForm         An entity that holds the webForm attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadStylesIntoContainer(final boolean isCreateEvent, final WebForm webForm,
                                         final DataContainer entityContainer, final DataContainer dataContainer) {
        final FormStyle formStyle = webForm.getFormStyle();
        final FieldStyle fieldStyle = webForm.getFieldStyle();
        final SubmitButtonStyle submitButtonStyle = webForm.getSubmitButtonStyle();

        if (Objects.nonNull(formStyle)) {
            loadFormStyleIntoContainer(isCreateEvent, formStyle, entityContainer, dataContainer);
        }

        if (Objects.nonNull(fieldStyle)) {
            loadFieldStyleIntoContainer(isCreateEvent, fieldStyle, entityContainer, dataContainer);
        }

        if (Objects.nonNull(submitButtonStyle)) {
            loadSubmitButtonStyleIntoContainer(isCreateEvent, submitButtonStyle, entityContainer, dataContainer);
        }

        if (isCreateEvent) {
            loadStyleIntoContainer(webForm, dataContainer);
        }
    }

    /**
     * Loads the {@link SubmitButtonStyle} details into the data container.
     *
     * @param isCreateEvent     A flag which indicates whether the action is related to create or update event.
     * @param submitButtonStyle An entity that holds the {@link SubmitButtonStyle} attributes.
     * @param entityContainer   Holds the user defined and system field values of existing webForm.
     * @param dataContainer     Holds the updated information of user defined and system field values.
     */
    private void loadSubmitButtonStyleIntoContainer(final boolean isCreateEvent, final SubmitButtonStyle submitButtonStyle,
                                                    final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table submitButtonStyleTable = getSubmitButtonStyleTable();
        final Long id = submitButtonStyle.id();
        final Row submitButtonStyleRow = isCreateEvent ? new Row(submitButtonStyleTable) : entityContainer.getRow(submitButtonStyleTable, id);

        if (!isCreateEvent && Objects.isNull(submitButtonStyleRow)) {
            throw ErrorCode.get(WebFormErrorCodes.SUBMIT_BUTTON_STYLE_NOT_FOUND, String.format("ID : %d", id));
        }

        if (submitButtonStyleRow.isNewRow()) {
            submitButtonStyle.setId(submitButtonStyleRow.getPKValue());
        }
        submitButtonStyleRow.set(MWFSUBMITBUTTONSTYLE.BACKGROUND_COLOR, submitButtonStyle.getBackgroundColor());
        submitButtonStyleRow.set(MWFSUBMITBUTTONSTYLE.TEXT_COLOR, submitButtonStyle.getTextColor());
        submitButtonStyleRow.set(MWFSUBMITBUTTONSTYLE.ALIGNMENT_ID, submitButtonStyle.getAlignment().getId());
        submitButtonStyleRow.set(MWFSUBMITBUTTONSTYLE.WIDTH, submitButtonStyle.getWidth());

        dataContainer.updateRow(submitButtonStyleRow);
    }

    /**
     * Loads the {@link FieldStyle} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param fieldStyle      An entity that holds the {@link FieldStyle} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadFieldStyleIntoContainer(final boolean isCreateEvent, final FieldStyle fieldStyle,
                                             final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table fieldStyleTable = getFieldStyleTable();
        final Long id = fieldStyle.id();
        final Row fieldStyleRow = isCreateEvent ? new Row(fieldStyleTable) : entityContainer.getRow(fieldStyleTable, id);

        if (!isCreateEvent && Objects.isNull(fieldStyleRow)) {
            throw ErrorCode.get(WebFormErrorCodes.FIELD_STYLE_NOT_FOUND, String.format("ID : %d", id));
        }

        if (fieldStyleRow.isNewRow()) {
            fieldStyle.setId(fieldStyleRow.getPKValue());
        }
        fieldStyleRow.set(MWFFIELDSTYLE.LABEL_COLOR, fieldStyle.getLabelColor());
        fieldStyleRow.set(MWFFIELDSTYLE.LABEL_FONT_SIZE, fieldStyle.getLabelFontSize());
        fieldStyleRow.set(MWFFIELDSTYLE.INPUT_BACKGROUND_COLOR, fieldStyle.getInputBackgroundColor());
        fieldStyleRow.set(MWFFIELDSTYLE.INPUT_TEXT_COLOR, fieldStyle.getInputTextColor());
        fieldStyleRow.set(MWFFIELDSTYLE.INPUT_FONT_SIZE, fieldStyle.getInputFontSize());

        dataContainer.updateRow(fieldStyleRow);
    }

    /**
     * Loads the {@link FormStyle} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param formStyle       An entity that holds the {@link FormStyle} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadFormStyleIntoContainer(final boolean isCreateEvent, final FormStyle formStyle,
                                            final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table formStyleTable = getFormStyleTable();
        final Long id = formStyle.id();
        final Row formStyleRow = isCreateEvent ? new Row(formStyleTable) : entityContainer.getRow(formStyleTable, id);

        if (!isCreateEvent && Objects.isNull(formStyleRow)) {
            throw ErrorCode.get(WebFormErrorCodes.FORM_STYLE_NOT_FOUND, String.format("ID : %d", id));
        }

        if (formStyleRow.isNewRow()) {
            formStyle.setId(formStyleRow.getPKValue());
        }
        formStyleRow.set(MWFFORMSTYLE.BACKGROUND_COLOR, formStyle.getBackgroundColor());
        formStyleRow.set(MWFFORMSTYLE.HEADER_TEXT_COLOR, formStyle.getHeaderTextColor());
        formStyleRow.set(MWFFORMSTYLE.HEADER_FONT_SIZE, formStyle.getHeaderFontSize());
        formStyleRow.set(MWFFORMSTYLE.SUB_HEADER_TEXT_COLOR, formStyle.getSubHeaderTextColor());
        formStyleRow.set(MWFFORMSTYLE.SUB_HEADER_FONT_SIZE, formStyle.getSubHeaderFontSize());
        formStyleRow.set(MWFFORMSTYLE.DESCRIPTION_TEXT_COLOR, formStyle.getDescriptionTextColor());
        formStyleRow.set(MWFFORMSTYLE.DESCRIPTION_FONT_SIZE, formStyle.getDescriptionFontSize());
        formStyleRow.set(MWFFORMSTYLE.TEXT_COLOR, formStyle.getTextColor());
        formStyleRow.set(MWFFORMSTYLE.TEXT_FONT_SIZE, formStyle.getTextFontSize());
        formStyleRow.set(MWFFORMSTYLE.ALIGNMENT_ID, formStyle.getAlignment().getId());
        formStyleRow.set(MWFFORMSTYLE.FONT_ID, formStyle.getFont().getId());

        dataContainer.updateRow(formStyleRow);
    }

    /**
     * Loads the style details into the data container.
     *
     * @param webForm       An entity that holds the {@link WebForm} attributes.
     * @param dataContainer Holds the updated information of user defined and system field values.
     */
    private void loadStyleIntoContainer(final WebForm webForm, final DataContainer dataContainer) {
        final Long formStyleId = webForm.getFormStyle().id();
        final Long fieldStyleId = webForm.getFieldStyle().id();
        final Long submitButtonStyleId = webForm.getSubmitButtonStyle().id();

        if (Objects.nonNull(formStyleId) && Objects.nonNull(fieldStyleId) && Objects.nonNull(submitButtonStyleId)) {
            final Row styleRow = new Row(getStyleTable());

            if (styleRow.isNewRow()) {
                webForm.setStyleId(styleRow.getPKValue());
            }
            styleRow.set(MWFSTYLE.FORM_STYLE_ID, formStyleId);
            styleRow.set(MWFSTYLE.FIELD_STYLE_ID, fieldStyleId);
            styleRow.set(MWFSTYLE.SUBMIT_BUTTON_STYLE_ID, submitButtonStyleId);

            dataContainer.addNewRow(styleRow);
        }
    }

    /**
     * Loads the {@link FormFieldProperty} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webForm         An entity that holds the {@link WebForm} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadFieldPropertiesIntoContainer(final boolean isCreateEvent, final WebForm webForm,
                                                  final DataContainer entityContainer, final DataContainer dataContainer) {
        final Long webFormId = webForm.id();

        for (final FormFieldProperty formFieldProperty : webForm.getFormFieldProperties()) {

            if (formFieldProperty instanceof final FormField formField) {
                loadFormFieldIntoContainer(isCreateEvent, webFormId, formField, entityContainer, dataContainer);
            } else if (formFieldProperty instanceof final FormElement formElement) {
                loadFormElementIntoContainer(isCreateEvent, webFormId, formElement, entityContainer, dataContainer);
            }
        }
    }

    /**
     * Loads the {@link FormElement} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webFormId       Refers the webForm id.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadFormElementIntoContainer(final boolean isCreateEvent, final Long webFormId, final FormElement formElement,
                                              final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table formElementTable = getFormElementTable();
        final Long id = formElement.id();
        final Row formElementRow = Objects.isNull(id) || isCreateEvent ? new Row(formElementTable) : entityContainer.getRow(formElementTable, id);

        if (!isCreateEvent && Objects.isNull(formElementRow)) {
            throw ErrorCode.get(WebFormErrorCodes.FORM_ELEMENT_NOT_FOUND, String.format("ID : %d", id));
        }

        if (formElementRow.isNewRow()) {
            formElement.setId(formElementRow.getPKValue());
        }
        formElementRow.set(MWFFORMELEMENTS.WEB_FORM_ID, webFormId);
        formElementRow.set(MWFFORMELEMENTS.NAME, formElement.getName());
        formElementRow.set(MWFFORMELEMENTS.LABEL, formElement.getLabel());
        formElementRow.set(MWFFORMELEMENTS.CONFIG, formElement.getConfig());

        dataContainer.updateRow(formElementRow);
    }

    /**
     * Loads the {@link FormField} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webFormId       Refers the webForm id.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadFormFieldIntoContainer(final boolean isCreateEvent, final Long webFormId, final FormField formField,
                                            final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table formFieldTable = getFormFieldTable();
        final Long id = formField.id();
        final Row formFieldRow = Objects.isNull(id) || isCreateEvent ? new Row(formFieldTable) : entityContainer.getRow(formFieldTable, id);

        if (!isCreateEvent && Objects.isNull(formFieldRow)) {
            throw ErrorCode.get(WebFormErrorCodes.FORM_FIELD_NOT_FOUND, String.format("ID : %d", id));
        }

        if (formFieldRow.isNewRow()) {
            formField.setId(formFieldRow.getPKValue());
        }
        formFieldRow.set(MWFFORMFIELDS.WEB_FORM_ID, webFormId);
        formFieldRow.set(MWFFORMFIELDS.FIELD_ID, formField.getFieldId());
        formFieldRow.set(MWFFORMFIELDS.LABEL, formField.getLabel());
        formFieldRow.set(MWFFORMFIELDS.CONFIG, formField.getConfig());

        dataContainer.updateRow(formFieldRow);
    }

    /**
     * Loads the {@link SubmitAction} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webForm         An entity that holds the {@link WebForm} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadSubmitActionIntoContainer(final boolean isCreateEvent, final WebForm webForm,
                                               final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table submitActionTable = getSubmitActionTable();
        final SubmitAction submitAction = webForm.getSubmitAction();
        final Long id = submitAction.id();
        final Row submitActionRow = isCreateEvent ? new Row(submitActionTable) : entityContainer.getRow(submitActionTable, id);

        if (!isCreateEvent && Objects.isNull(submitActionRow)) {
            throw ErrorCode.get(WebFormErrorCodes.SUBMIT_ACTION_DETAIL_NOT_FOUND, String.format("ID : %d", id));
        }

        if (submitActionRow.isNewRow()) {
            final Long pkValue = submitActionRow.getPKValue();

            submitAction.setId(pkValue);
            webForm.setSubmitActionId(pkValue);
        }
        submitActionRow.set(MWFSUBMITACTION.THANK_YOU_MESSAGE, submitAction.isThankYouMessageEnabled() ? submitAction.getThankYouMessage() : null);
        submitActionRow.set(MWFSUBMITACTION.REDIRECT_URL, submitAction.isRedirectEnabled() ? submitAction.getRedirectUrl() : null);
        submitActionRow.set(MWFSUBMITACTION.IS_EMAIL_NOTIFICATION_ENABLED, submitAction.isEmailNotificationEnabled());
        submitActionRow.set(MWFSUBMITACTION.IS_CONTACT_CREATION_ENABLED, submitAction.isContactCreationEnabled());
        submitActionRow.set(MWFSUBMITACTION.IS_DEAL_CREATION_WITH_CONTACT_ENABLED, submitAction.isDealCreationWithContactEnabled());

        dataContainer.updateRow(submitActionRow);
        loadRecipientEmailsIntoContainer(isCreateEvent, webForm.id(), submitAction, entityContainer, dataContainer);
    }

    /**
     * Loads the {@link RecipientEmail} details into the data container.
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event.
     * @param webFormId       Refers the webForm id.
     * @param submitAction    An entity that holds the {@link SubmitAction} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void loadRecipientEmailsIntoContainer(final boolean isCreateEvent, final Long webFormId, final SubmitAction submitAction,
                                                  final DataContainer entityContainer, final DataContainer dataContainer) {
        final Collection<RecipientEmail> recipientEmails = submitAction.getRecipientEmails();

        if (submitAction.isEmailNotificationEnabled() && Objects.nonNull(recipientEmails)) {
            final Table emailTable = getFormEmailTable();

            for (final RecipientEmail recipientEmail : recipientEmails) {
                final WhereClause emailCriteria = new WhereClause(Criteria.eq(emailTable.getColumn(MWFEMAIL.WEB_FORM_ID), webFormId))
                        .and(Criteria.eq(emailTable.getColumn(MWFEMAIL.EMAIL), recipientEmail.getValue()));
                final Row recipientEmailRow = isCreateEvent ? new Row(emailTable) : entityContainer.getOrInitRow(emailTable, emailCriteria);

                if (!isCreateEvent && Objects.isNull(recipientEmailRow)) {
                    throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_EMAIL_NOT_FOUND, String.format("EMAIL : %s", recipientEmail.getValue()));
                }

                if (recipientEmailRow.isNewRow()) {
                    recipientEmail.setId(recipientEmailRow.getPKValue());
                }
                recipientEmailRow.set(MWFEMAIL.EMAIL, recipientEmail.getValue());
                recipientEmailRow.set(MWFEMAIL.WEB_FORM_ID, webFormId);

                dataContainer.updateRow(recipientEmailRow);
            }

            if (!isCreateEvent) {
                deleteUnmappedEmails(webFormId, submitAction, entityContainer, dataContainer);
            }
        }
    }

    /**
     * Deletes the unmapped {@link RecipientEmail} of webForm.
     *
     * @param webFormId       Refers the webForm id.
     * @param submitAction    An entity that holds the {@link SubmitAction} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void deleteUnmappedEmails(final Long webFormId, final SubmitAction submitAction,
                                      final DataContainer entityContainer, final DataContainer dataContainer) {
        final Table recepientEmailTable = getFormEmailTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(recepientEmailTable.getColumn(MWFEMAIL.WEB_FORM_ID), webFormId));
        final Set<Long> emailIds = submitAction.getRecipientEmails().stream().map(RecipientEmail::id)
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        final Stream<Row> formElementRows = entityContainer.getRows(recepientEmailTable, emailIds.isEmpty()
                ? whereClause : whereClause.and(Criteria.notIn(recepientEmailTable.getPKColumn(), emailIds)));

        formElementRows.filter(Objects::nonNull).forEach(dataContainer::deleteRow);
    }

    /**
     * Deletes the unmapped {@link FormFieldProperty} of webForm.
     *
     * @param webForm         An entity that holds the {@link WebForm} attributes.
     * @param entityContainer Holds the user defined and system field values of existing webForm.
     * @param dataContainer   Holds the updated information of user defined and system field values.
     */
    private void deleteUnmappedFieldProperties(final WebForm webForm, final DataContainer entityContainer,
                                               final DataContainer dataContainer) {
        final Table formFieldTable = getFormFieldTable();
        final Table formElementTable = getFormElementTable();
        final Long webFormId = webForm.id();
        final Set<Long> formFieldIds = new HashSet<>();
        final Set<Long> formElementIds = new HashSet<>();

        for (final FormFieldProperty formFieldProperty : webForm.getFormFieldProperties()) {
            final Type fieldPropertyType = formFieldProperty.getType();

            if (Objects.equals(Type.FIELD, fieldPropertyType)) {
                formFieldIds.add(formFieldProperty.id());
            } else if (Objects.equals(Type.ELEMENT, fieldPropertyType)) {
                formElementIds.add(formFieldProperty.id());
            }
        }

        final Stream<Row> formFieldRows = entityContainer.getRows(formFieldTable,
                new WhereClause(Criteria.eq(formFieldTable.getColumn(MWFFORMFIELDS.WEB_FORM_ID), webFormId))
                        .and(Criteria.notIn(formFieldTable.getPKColumn(), formFieldIds)));
        final WhereClause whereClause = new WhereClause(Criteria.eq(formElementTable.getColumn(MWFFORMELEMENTS.WEB_FORM_ID), webFormId));
        final Stream<Row> formElementRows = entityContainer.getRows(formElementTable, formElementIds.isEmpty()
                ? whereClause : whereClause.and(Criteria.notIn(formElementTable.getPKColumn(), formElementIds)));

        formFieldRows.filter(Objects::nonNull).forEach(dataContainer::deleteRow);
        final Set<Long> removedImageAttachmentIds = new HashSet<>();

        formElementRows.filter(Objects::nonNull).forEach(row -> {
            final String formName = row.get(MWFFORMELEMENTS.NAME);

            if (Objects.equals(IMAGE_ELEMENT_NAME, formName)) {
                final String config = row.get(MWFFORMELEMENTS.CONFIG);

                if (Objects.nonNull(config)) {
                    final Object rawId = Json.object(config).opt(WebFormFields.ATTACHMENT_ID.getName());

                    if (Objects.nonNull(rawId)) {
                        removedImageAttachmentIds.add(((Number) rawId).longValue());
                    }
                }
            }
            dataContainer.deleteRow(row);
        });

        if (!removedImageAttachmentIds.isEmpty()) {
            attachmentRemovalPublisher.publish(String.valueOf(webFormId), removedImageAttachmentIds);
        }
    }

    /**
     * Converts row into {@link WebForm}.
     *
     * @param row           WebForm row to be converted.
     * @param dataContainer A container that holds the user defined field values and system values.
     * @param isPartial     A flag that indicates the row should be converted partially or not.
     * @return {@link WebForm}.
     */
    private WebForm convertRowToModel(final Row row, final DataContainer dataContainer, final boolean isPartial) {
        final WebForm webForm = new WebForm();
        final Long id = row.getPKValue();

        webForm.setId(id);
        webForm.setOrgId(row.getOrgId());
        webForm.setName(row.get(OMWFWEBFORM.NAME));
        webForm.setActive(row.get(OMWFWEBFORM.IS_ACTIVE));
        webForm.setVisits(row.get(OMWFWEBFORM.VISITS));
        webForm.setSubmissions(row.get(OMWFWEBFORM.SUBMISSIONS));
        webForm.setConversions(row.get(OMWFWEBFORM.CONVERSIONS));
        webForm.setCreatedTime(row.get(OMWFWEBFORM.CREATED_TIME));

        if (!isPartial) {
            webForm.setDescription(row.get(OMWFWEBFORM.DESCRIPTION));
            webForm.setSubmitButtonName(row.get(OMWFWEBFORM.SUBMIT_BUTTON_NAME));
            final Integer language = row.get(OMWFWEBFORM.LANGUAGE_ID);

            if (Objects.nonNull(language)) {
                webForm.setLanguage(Language.getLanguageById(language));
            }
            webForm.setTemplateId(row.get(OMWFWEBFORM.TEMPLATE_ID));
            webForm.setOwnerId(row.get(OMWFWEBFORM.OWNER_ID));
            webForm.setAttachmentId(row.get(OMWFWEBFORM.ATTACHMENT_ID));

            final Table styleTable = getStyleTable();
            final Row styleRow = dataContainer.getRow(styleTable, new WhereClause(Criteria
                    .eq(styleTable.getPKColumn(), row.get(OMWFWEBFORM.STYLE_ID))));

            if (Objects.nonNull(styleRow)) {
                convertStyleRowToModel(styleRow, webForm, dataContainer);
            }
            final Table submitActionTable = getSubmitActionTable();
            final Table formFieldTable = getFormFieldTable();
            final Table formElementTable = getFormElementTable();
            final Row submitActionRow = dataContainer.getRow(submitActionTable,
                    new WhereClause(Criteria.eq(submitActionTable.getPKColumn(), row.get(OMWFWEBFORM.SUBMIT_ACTION_ID))));
            final Stream<Row> formFieldRows = dataContainer.getRows(formFieldTable,
                    new WhereClause(Criteria.eq(formFieldTable.getColumn(MWFFORMFIELDS.WEB_FORM_ID), id)));
            final Stream<Row> formElementRows = dataContainer.getRows(formElementTable,
                    new WhereClause(Criteria.eq(formElementTable.getColumn(MWFFORMELEMENTS.WEB_FORM_ID), id)));

            if (Objects.nonNull(submitActionRow)) {
                webForm.setSubmitAction(convertSubmitActionToModel(id, submitActionRow, dataContainer));
            }
            formFieldRows.filter(Objects::nonNull).forEach(formFieldRow -> webForm
                    .addFormFieldProperty(convertFormFieldToModel(formFieldRow, false)));
            formElementRows.filter(Objects::nonNull).forEach(formElementRow -> webForm
                    .addFormFieldProperty(convertFormElementToModel(formElementRow, false)));
        }

        return webForm;
    }

    /**
     * Converts the row into the form styles.
     *
     * @param styleRow      Refers the style row.
     * @param webForm       Refers the {@link WebForm}.
     * @param dataContainer A container that holds the user defined field values and system values.
     */
    private void convertStyleRowToModel(final Row styleRow, final WebForm webForm, final DataContainer dataContainer) {
        final Table formStyleTable = getFormStyleTable();
        final Table fieldStyleTable = getFieldStyleTable();
        final Table submitButtonStyleTable = getSubmitButtonStyleTable();
        final Row formStyleRow = dataContainer.getRow(formStyleTable,
                new WhereClause(Criteria.eq(formStyleTable.getPKColumn(), styleRow.get(MWFSTYLE.FORM_STYLE_ID))));
        final Row fieldStyleRow = dataContainer.getRow(fieldStyleTable,
                new WhereClause(Criteria.eq(fieldStyleTable.getPKColumn(), styleRow.get(MWFSTYLE.FIELD_STYLE_ID))));
        final Row submitButtonStyleRow = dataContainer.getRow(submitButtonStyleTable,
                new WhereClause(Criteria.eq(submitButtonStyleTable.getPKColumn(), styleRow.get(MWFSTYLE.SUBMIT_BUTTON_STYLE_ID))));

        if (Objects.nonNull(formStyleRow)) {
            webForm.setFormStyle(convertFormStyleRowToModel(formStyleRow));
        }

        if (Objects.nonNull(fieldStyleRow)) {
            webForm.setFieldStyle(convertFieldStyleToModel(fieldStyleRow));
        }

        if (Objects.nonNull(submitButtonStyleRow)) {
            webForm.setSubmitButtonStyle(convertSubmitButtonStyleToModel(submitButtonStyleRow));
        }
    }

    /**
     * Converts row into {@link SubmitAction}.
     *
     * @param webFormId       Refers the webForm id.
     * @param submitActionRow Refers the submit action row.
     * @param dataContainer   A container that holds the user defined field values and system values.
     * @return {@link SubmitAction} details.
     */
    private SubmitAction convertSubmitActionToModel(final Long webFormId, final Row submitActionRow,
                                                    final DataContainer dataContainer) {
        final SubmitAction submitAction = new SubmitAction();
        final Table recepientEmailTable = getFormEmailTable();
        final Stream<Row> emailRows = dataContainer.getRows(recepientEmailTable,
                new WhereClause(Criteria.eq(recepientEmailTable.getColumn(MWFEMAIL.WEB_FORM_ID), webFormId)));
        final Object value = submitActionRow.get(MWFSUBMITACTION.THANK_YOU_MESSAGE);
        final String redirectUrl = submitActionRow.get(MWFSUBMITACTION.REDIRECT_URL);
        final I18nKey i18nKey = TypeChecker.isString(value) ? new I18nKey((String) value) : (I18nKey) value;

        if (Objects.nonNull(i18nKey)) {
            submitAction.setThankYouMessage(i18nKey.getKey());
            submitAction.setThankYouMessageEnabled(true);
        }
        submitAction.setId(submitActionRow.getPKValue());
        submitAction.setI18nMessage(i18nKey);
        submitAction.setRedirectUrl(redirectUrl);
        submitAction.setRedirectEnabled(Objects.nonNull(redirectUrl));
        submitAction.setEmailNotificationEnabled(submitActionRow.get(MWFSUBMITACTION.IS_EMAIL_NOTIFICATION_ENABLED));
        submitAction.setContactCreationEnabled(submitActionRow.get(MWFSUBMITACTION.IS_CONTACT_CREATION_ENABLED));
        submitAction.setDealCreationWithContactEnabled(submitActionRow.get(MWFSUBMITACTION.IS_DEAL_CREATION_WITH_CONTACT_ENABLED));
        submitAction.setRecipientEmails(emailRows.filter(Objects::nonNull).map(row -> {
            final RecipientEmail recipientEmail = new RecipientEmail();

            recipientEmail.setId(row.get(MWFEMAIL.ID));
            recipientEmail.setValue(row.get(MWFEMAIL.EMAIL));

            return recipientEmail;
        }).toList());

        return submitAction;
    }

    /**
     * Converts row into {@link FormElement}.
     *
     * @param formElementRow Refers the form element row.
     * @param isLabelI18nKey A flag that indicates whether the label is i18n or not.
     * @return {@link FormElement} details.
     */
    public FormElement convertFormElementToModel(final Row formElementRow, final boolean isLabelI18nKey) {
        final FormElement formElement = new FormElement();
        final Object value = formElementRow.get(MWFFORMELEMENTS.LABEL);

        if (isLabelI18nKey) {
            final I18nKey i18nKey = TypeChecker.isString(value) ? new I18nKey((String) value) : (I18nKey) value;

            formElement.setI18nLabel(i18nKey);
        } else {
            formElement.setLabel((String) value);
        }
        formElement.setId(formElementRow.getPKValue());
        formElement.setName(formElementRow.get(MWFFORMELEMENTS.NAME));
        formElement.setConfig(formElementRow.get(MWFFORMELEMENTS.CONFIG));

        return formElement;
    }

    /**
     * Converts row into {@link FormField}.
     *
     * @param formFieldRow   Refers the form field row.
     * @param isLabelI18nKey A flag that indicates whether the label is i18n or not.
     * @return {@link FormField} details.
     */
    public FormField convertFormFieldToModel(final Row formFieldRow, final boolean isLabelI18nKey) {
        final FormField formField = new FormField();
        final Object label = formFieldRow.get(MWFFORMFIELDS.LABEL);

        if (isLabelI18nKey) {
            final I18nKey i18nValue = TypeChecker.isString(label) ? new I18nKey((String) label) : (I18nKey) label;

            formField.setI18nLabel(i18nValue);
        } else {
            formField.setLabel((String) label);
        }
        formField.setId(formFieldRow.getPKValue());
        formField.setFieldId(formFieldRow.get(MWFFORMFIELDS.FIELD_ID));
        formField.setFieldTypeId(formFieldRow.get(MWFTEMPLATEFIELDS.FIELD_TYPE_ID));
        formField.setConfig(formFieldRow.get(MWFFORMFIELDS.CONFIG));

        return formField;
    }

    /**
     * Converts the row into {@link SubmitButtonStyle}.
     *
     * @param submitButtonStyleRow Refers the submit button style row.
     * @return {@link SubmitButtonStyle} details.
     */
    public SubmitButtonStyle convertSubmitButtonStyleToModel(final Row submitButtonStyleRow) {
        final SubmitButtonStyle submitButtonStyle = new SubmitButtonStyle();

        submitButtonStyle.setId(submitButtonStyleRow.getPKValue());
        submitButtonStyle.setBackgroundColor(submitButtonStyleRow.get(MWFSUBMITBUTTONSTYLE.BACKGROUND_COLOR));
        submitButtonStyle.setTextColor(submitButtonStyleRow.get(MWFSUBMITBUTTONSTYLE.TEXT_COLOR));
        submitButtonStyle.setAlignment(Alignment.getAlignment(submitButtonStyleRow.get(MWFSUBMITBUTTONSTYLE.ALIGNMENT_ID)));
        submitButtonStyle.setWidth(submitButtonStyleRow.get(MWFSUBMITBUTTONSTYLE.WIDTH));

        return submitButtonStyle;
    }

    /**
     * Converts the row into {@link FieldStyle}.
     *
     * @param fieldStyleRow Refers the field style row.
     * @return {@link FieldStyle} details.
     */
    public FieldStyle convertFieldStyleToModel(final Row fieldStyleRow) {
        final FieldStyle fieldStyle = new FieldStyle();

        fieldStyle.setId(fieldStyleRow.getPKValue());
        fieldStyle.setLabelColor(fieldStyleRow.get(MWFFIELDSTYLE.LABEL_COLOR));
        fieldStyle.setLabelFontSize(fieldStyleRow.get(MWFFIELDSTYLE.LABEL_FONT_SIZE));
        fieldStyle.setInputBackgroundColor(fieldStyleRow.get(MWFFIELDSTYLE.INPUT_BACKGROUND_COLOR));
        fieldStyle.setInputTextColor(fieldStyleRow.get(MWFFIELDSTYLE.INPUT_TEXT_COLOR));
        fieldStyle.setInputFontSize(fieldStyleRow.get(MWFFIELDSTYLE.INPUT_FONT_SIZE));

        return fieldStyle;
    }

    /**
     * Converts the row into {@link FormStyle}.
     *
     * @param formStyleRow Refers the form style row.
     * @return {@link FormStyle} details.
     */
    public FormStyle convertFormStyleRowToModel(final Row formStyleRow) {
        final FormStyle formStyle = new FormStyle();

        formStyle.setId(formStyleRow.getPKValue());
        formStyle.setFont(Font.getFont(formStyleRow.get(MWFFORMSTYLE.FONT_ID)));
        formStyle.setAlignment(Alignment.getAlignment(formStyleRow.get(MWFFORMSTYLE.ALIGNMENT_ID)));
        formStyle.setBackgroundColor(formStyleRow.get(MWFFORMSTYLE.BACKGROUND_COLOR));
        formStyle.setHeaderTextColor(formStyleRow.get(MWFFORMSTYLE.HEADER_TEXT_COLOR));
        formStyle.setHeaderFontSize(formStyleRow.get(MWFFORMSTYLE.HEADER_FONT_SIZE));
        formStyle.setSubHeaderTextColor(formStyleRow.get(MWFFORMSTYLE.SUB_HEADER_TEXT_COLOR));
        formStyle.setSubHeaderFontSize(formStyleRow.get(MWFFORMSTYLE.SUB_HEADER_FONT_SIZE));
        formStyle.setDescriptionFontSize(formStyleRow.get(MWFFORMSTYLE.DESCRIPTION_FONT_SIZE));
        formStyle.setDescriptionTextColor(formStyleRow.get(MWFFORMSTYLE.DESCRIPTION_TEXT_COLOR));
        formStyle.setTextColor(formStyleRow.get(MWFFORMSTYLE.TEXT_COLOR));
        formStyle.setTextFontSize(formStyleRow.get(MWFFORMSTYLE.TEXT_FONT_SIZE));

        return formStyle;
    }

    /**
     * Retrieves the Collection of {@link WebForm} for given orgId.
     *
     * @param orgId     Refers the organization id.
     * @param start     Refers the start value of webForm to be fetched.
     * @param limit     Refers the limit value of webForm to be fetched.
     * @param sortField Refers the field by which the data is sorted.
     * @param sortType  Refers the sort type.
     * @param isActive  The flag that represents the active status of form.
     * @return Collection of {@link WebForm}.
     */
    public Collection<WebForm> getWebFormList(final Long orgId, final int start, final int limit,
                                              final String sortField, final String sortType, final boolean isActive) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column nameColumn = table.getColumn(OMWFWEBFORM.NAME);
        final Column isActiveColumn = table.getColumn(OMWFWEBFORM.IS_ACTIVE);
        final Column visitsColumn = table.getColumn(OMWFWEBFORM.VISITS);
        final Column submissionsColumn = table.getColumn(OMWFWEBFORM.SUBMISSIONS);
        final Column conversionsColumn = table.getColumn(OMWFWEBFORM.CONVERSIONS);
        final Column createdTimeColumn = table.getColumn(OMWFWEBFORM.CREATED_TIME);
        final OrderType orderType = WebFormFields.ASC.getName().equalsIgnoreCase(sortType)
                ? OrderType.ASCENDING : OrderType.DESCENDING;

        selectQuery.addSelectClause(List.of(table.getPKColumn(), table.getColumn(OMWFWEBFORM.ORG_ID),
                nameColumn, visitsColumn, submissionsColumn, conversionsColumn, createdTimeColumn, isActiveColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(isActiveColumn, isActive)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        switch (sortField) {
            case "name" -> selectQuery.addOrderByClause(new OrderByClause(nameColumn, orderType));
            case "visits" -> selectQuery.addOrderByClause(new OrderByClause(visitsColumn, orderType));
            case "submissions" -> selectQuery.addOrderByClause(new OrderByClause(submissionsColumn, orderType));
            case "conversions" -> selectQuery.addOrderByClause(new OrderByClause(conversionsColumn, orderType));
            case "createdTime" -> selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, orderType));
            default -> throw ErrorCode.get(WebFormErrorCodes.SORTING_NOT_ALLOWED);
        }

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(table);

        return rows.filter(Objects::nonNull).map(row -> convertRowToModel(row, dataContainer, true)).toList();
    }

    /**
     * Gets the count of total webForms of given orgId.
     *
     * @param orgId    Represents the organization id.
     * @param isActive The flag that represents the active status of form.
     * @return Count of webForms.
     */
    public Long getWebFormCount(final Long orgId, final boolean isActive) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMWFWEBFORM.IS_ACTIVE), isActive)));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Updates the web form active status.
     *
     * @param id       Refers the web form id.
     * @param orgId    Refers the organization id.
     * @param isActive Refers the web form status to be changed.
     */
    public boolean updateFormStatus(final Long id, final Long orgId, final boolean isActive) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMWFWEBFORM.IS_ACTIVE), isActive);
        updateQuery.addUpdateColumn(table.getColumn(OMWFWEBFORM.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        try {
            orgDataStore(orgId).update(updateQuery);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * <p>
     * Checks whether the given web form name already present for the given organization
     * </p>
     *
     * @param orgId Represents an organization id
     * @param name  Represents the web form name
     * @return true, if exists
     */
    public boolean exists(final Long orgId, final String name) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMWFWEBFORM.NAME), name)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Converts row into {@link WebForm}.
     *
     * @param row           WebForm row to be converted partially.
     * @param dataContainer A container that holds webForm information.
     * @return {@link WebForm}.
     */
    private WebForm convertRowToPartialModel(final Row row, final DataContainer dataContainer) {
        final WebForm webForm = new WebForm();
        final Long id = row.getPKValue();

        webForm.setId(id);
        webForm.setOrgId(row.getOrgId());
        webForm.setName(row.get(OMWFWEBFORM.NAME));
        webForm.setOwnerId(row.get(OMWFWEBFORM.OWNER_ID));
        webForm.setActive(row.get(OMWFWEBFORM.IS_ACTIVE));
        webForm.setVisits(row.get(OMWFWEBFORM.VISITS));
        webForm.setSubmissions(row.get(OMWFWEBFORM.SUBMISSIONS));
        webForm.setConversions(row.get(OMWFWEBFORM.CONVERSIONS));

        final Table submitActionTable = getSubmitActionTable();
        final Table formFieldTable = getFormFieldTable();
        final Row submitActionRow = dataContainer.getRow(submitActionTable,
                new WhereClause(Criteria.eq(submitActionTable.getPKColumn(), row.get(OMWFWEBFORM.SUBMIT_ACTION_ID))));
        final Stream<Row> formFieldRows = dataContainer.getRows(formFieldTable,
                new WhereClause(Criteria.eq(formFieldTable.getColumn(MWFFORMFIELDS.WEB_FORM_ID), id)));

        if (Objects.nonNull(submitActionRow)) {
            webForm.setSubmitAction(convertSubmitActionToModel(id, submitActionRow, dataContainer));
        }
        formFieldRows.filter(Objects::nonNull).forEach(formFieldRow -> webForm
                .addFormFieldProperty(convertFormFieldToModel(formFieldRow, false)));

        return webForm;
    }

    /**
     * Updates the form visit detail for given webForm id.
     *
     * @param id      Refers the form id.
     * @param orgId   Refers the organization id.
     * @param webForm Refers the {@link WebForm}.
     * @return true, if the form visit detail is updated, otherwise returns false.
     */
    public final boolean updateVisitDetails(final long id, final long orgId, final WebForm webForm) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMWFWEBFORM.CONVERSIONS), webForm.getConversionValue(true));
        updateQuery.addUpdateColumn(table.getColumn(OMWFWEBFORM.VISITS), webForm.getVisits());
        updateQuery.addUpdateColumn(table.getColumn(OMWFWEBFORM.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        try {
            orgDataStore(orgId).update(updateQuery);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Fetches the submission, visits and conversion details of a form.
     *
     * @param orgId Refers the organization id.
     * @param id    Refers the webForm id.
     * @return The webForm which contains the submission details.
     */
    public final WebForm getFormVisitsAndSubmissionDetails(final long orgId, final long id) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(table.getColumn(OMWFWEBFORM.SUBMISSIONS),
                table.getColumn(OMWFWEBFORM.CONVERSIONS), table.getColumn(OMWFWEBFORM.VISITS)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(table);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(WebFormErrorCodes.WEB_FORM_NOT_FOUND);
        } else {
            return convertFormVisitsAndSubmissionDetails(row);
        }
    }

    /**
     * <p>
     * Gets the form field property id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param formId   Represents the form id
     * @param fieldId  Represents the field id
     * @return The form field property id
     */
    public final Optional<Long> getFormFieldPropertyId(final Long orgId, final Long formId, final Long fieldId) {
        final Table formFieldTable = getFormFieldTable();
        final Row row = orgDataStore(orgId).get(formFieldTable, new WhereClause(Criteria.eq(formFieldTable.getColumn(MWFFORMFIELDS.WEB_FORM_ID), formId))
                .and(Criteria.eq(formFieldTable.getColumn(MWFFORMFIELDS.FIELD_ID), fieldId)), List.of(formFieldTable.getPKColumn())).getRow(formFieldTable);

        return Objects.isNull(row) ? Optional.empty() : Optional.of(row.getPKValue());
    }

    /**
     * Converts the row into webForm.
     *
     * @param row Refers the row which has the webForm details.
     * @return The {@link WebForm}.
     */
    private WebForm convertFormVisitsAndSubmissionDetails(final Row row) {
        final WebForm webForm = new WebForm();

        webForm.setVisits(row.get(OMWFWEBFORM.VISITS));
        webForm.setSubmissions(row.get(OMWFWEBFORM.SUBMISSIONS));
        webForm.setConversions(row.get(OMWFWEBFORM.CONVERSIONS));

        return webForm;
    }
}
