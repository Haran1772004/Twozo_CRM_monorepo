package com.tlc.crm.core.webform.api.manager;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.auto.assignment.event.AutoAssignmentEvent;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.sql.resource.MWFCONTACTWEBFORM;
import com.tlc.crm.core.sql.resource.MWFDEALWEBFORM;
import com.tlc.crm.core.sql.resource.OMWFFORMSUBMISSION;
import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.publish.TriggerDataPublisher;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.api.model.SubmitAction;
import com.tlc.crm.webform.api.model.WebForm;
import com.tlc.crm.webform.sql.resource.OMWFWEBFORM;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the webForm {@link SubmitAction} related DB-actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FormSubmitActionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormSubmitActionManager.class);
    private final WebFormManager webFormManager;
    private final TriggerDataPublisher triggerDataPublisher;
    private final IndexDataPublisher indexDataPublisher;
    private final AuditLogger auditLogger;
    private final EventDataHandler eventDataHandler;

    private FormSubmitActionManager() {
        this.webFormManager = WebFormManager.getInstance();
        this.triggerDataPublisher = TriggerDataPublisher.getInstance();
        this.indexDataPublisher = IndexDataPublisher.getInstance();
        this.auditLogger = AuditLogger.getInstance();
        this.eventDataHandler = EventDataHandler.getInstance();
    }

    private static class Instance {
        private static final FormSubmitActionManager INSTANCE = new FormSubmitActionManager();
    }

    /**
     * Gets the {@link FormSubmitActionManager} Instance.
     *
     * @return {@link FormSubmitActionManager}
     */
    public static FormSubmitActionManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore}.
     *
     * @param orgId Represents the organization id.
     * @return The {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * Gets the table of contact webForm.
     *
     * @return The {@link Table} of contact webForm.
     */
    private Table getContactWebFormTable() {
        return Table.get(MWFCONTACTWEBFORM.TABLE);
    }

    /**
     * Gets the table of deal webForm.
     *
     * @return The {@link Table} of deal webForm.
     */
    private Table getDealWebFormTable() {
        return Table.get(MWFDEALWEBFORM.TABLE);
    }

    /**
     * Gets the table of form submission.
     *
     * @return The {@link Table} of form submission.
     */
    private Table getFormSubmissionTable() {
        return Table.get(OMWFFORMSUBMISSION.TABLE);
    }

    /**
     * Creates an entity when a form is submitted.
     *
     * @param webForm Refers the {@link WebForm}.
     * @param entity  Refers the {@link Entity}.
     * @return true if entity is created.
     */
    public final boolean createEntity(final WebForm webForm, final Entity entity, final String refId) {
        final DataContainer dataContainer = DataContainer.create();
        final Table table = webFormManager.getTable();
        final long orgId = webForm.orgId();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Long webFormId = webForm.id();
        final Row row = orgDataStore.get(table, new WhereClause(Criteria.eq(table.getPKColumn(), webFormId))).getRow(table);

        if (Objects.nonNull(entity)) {
            try {
                if (entity instanceof final Contact contact) {
                    ContactManager.getInstance().loadContactIntoContainer(true, contact, dataContainer);
                    loadContactDetails(webFormId, contact, dataContainer);
                } else if (entity instanceof final Deal deal) {
                    DealManager.getInstance().loadDealIntoContainer(true, deal, dataContainer);
                    loadDealDetails(webFormId, deal, dataContainer);
                }
            } catch (Exception exception) {
                LOGGER.error("WebForm: failed while loading entity into container. orgId={}, webFormId={}, entityType={}", orgId, webFormId, entity.getEntityType());
                exception.printStackTrace();
                return false;
            }
        }

        if (Objects.nonNull(row)) {
            final long currentTime = Instant.now().toEpochMilli();
            row.set(OMWFWEBFORM.CONVERSIONS, webForm.getConversionValue(false));
            row.set(OMWFWEBFORM.SUBMISSIONS, webForm.getSubmissions());
            row.set(OMWFWEBFORM.MODIFIED_TIME, currentTime);

            webForm.setUpdatedTime(currentTime);
            dataContainer.updateRow(row);
        }

        final Table formSubmissionTable = getFormSubmissionTable();

        Optional.ofNullable(orgDataStore.get(formSubmissionTable, new WhereClause(Criteria.eq(formSubmissionTable.getColumn(OMWFFORMSUBMISSION.REF_ID), refId))
                        .and(Criteria.eq(formSubmissionTable.getColumn(OMWFFORMSUBMISSION.WEB_FORM_ID), webFormId)))
                .getRow(formSubmissionTable)).ifPresent(dataContainer::deleteRow);

        try {
            LOGGER.info("WebForm: committing changes. orgId={}, webFormId={}, refId={}, entityType={}, entityId={}", orgId, webFormId, refId, entity.getEntityType(), entity.id());

            orgDataStore.commitChanges(dataContainer);

            LOGGER.info("WebForm: commitChanges SUCCESS. orgId={}, webFormId={}, refId={}, entityType={}, entityId={}", orgId, webFormId, refId, entity.getEntityType(), entity.id());
            LOGGER.info("WebForm: publishing trigger data. orgId={}, entityType={}, entityId={}", orgId, entity.getEntityType(), entity.id());

            triggerDataPublisher.publish(orgId, webForm.getOwnerId(), List.of(entity), new TriggerInfo(Source.get(entity.getEntityType()),
                    Action.CREATE, ModelSource.WEB_FORM, null, true));

            LOGGER.info("WebForm: triggerDataPublisher SUCCESS. orgId={}, entityType={}, entityId={}", orgId, entity.getEntityType(), entity.id());
            LOGGER.info("WebForm: publishing index data. orgId={}, entityType={}, entityId={}", orgId, entity.getEntityType(), entity.id());
            indexDataPublisher.publishIndexData(orgId, List.of(entity), Action.CREATE);
            LOGGER.info("WebForm: indexDataPublisher SUCCESS. orgId={}, entityType={}, entityId={}", orgId, entity.getEntityType(), entity.id());

            final EventData eventData = new SecureEventData(orgId, webForm.getOwnerId());

            eventData.setAction(Action.CREATE);
            eventData.setValues(List.of(entity));
            eventDataHandler.handleEvent(AutoAssignmentEvent.ASSIGN_OWNER, eventData, Event.Type.POST);

            if (Objects.nonNull(entity)) {
                auditLogger.log(orgId, List.of(new AuditLogData(webForm.getOwnerId(), entity.id(), entity, AuditSource.get(entity.getEntityType()), AuditAction.ADDED)));
            }
            LOGGER.info("WebForm: createEntity SUCCESS. orgId={}, webFormId={}, refId={}, entityId={}", orgId, webFormId, refId, entity.id());
            return true;
        } catch (Exception exception) {
            LOGGER.error("WebForm: createEntity FAILED. orgId={}, webFormId={}, refId={}, entityType={}, entityId={}", orgId, webFormId, refId, Objects.nonNull(entity) ? entity.getEntityType() : "null", Objects.nonNull(entity) ? entity.id() : null);
            exception.printStackTrace();
            return false;
        }
    }


    /**
     * Loads the deal details provided through a web form into the data container.
     *
     * @param webFormId     Refers the web form id.
     * @param deal          Refers the {@link Deal} details.
     * @param dataContainer A container that holds webForm information.
     */
    private void loadDealDetails(final Long webFormId, final Deal deal, final DataContainer dataContainer) {
        final Contact contact = deal.getPrimaryContact();

        if (Objects.nonNull(contact)) {
            loadContactDetails(webFormId, contact, dataContainer);
        }
        final Row dealWebFormRow = new Row(getDealWebFormTable());

        dealWebFormRow.set(MWFDEALWEBFORM.DEAL_ID, deal.id());
        dealWebFormRow.set(MWFDEALWEBFORM.WEB_FORM_ID, webFormId);

        dataContainer.addNewRow(dealWebFormRow);
    }

    /**
     * Loads the contact details provided through a web form into the data container.
     *
     * @param webFormId     Refers the web form id.
     * @param contact       Refers the {@link Contact} details.
     * @param dataContainer A container that holds webForm information.
     */
    private void loadContactDetails(final Long webFormId, final Contact contact, final DataContainer dataContainer) {
        final Row contactWebFormRow = new Row(getContactWebFormTable());

        contactWebFormRow.set(MWFCONTACTWEBFORM.CONTACT_ID, contact.id());
        contactWebFormRow.set(MWFCONTACTWEBFORM.WEB_FORM_ID, webFormId);

        dataContainer.addNewRow(contactWebFormRow);
    }

    /**
     * Saves the form submission details into the database.
     *
     * @param orgId   Refers the organization id.
     * @param webForm Refers the {@link WebForm}.
     * @param data    Refers the form submission data in string format.
     */
    public void saveFormSubmission(final long orgId, final WebForm webForm, final String refId, final String data) {
        final Row row = new Row(getFormSubmissionTable());

        row.setOrgId(orgId);
        row.set(OMWFFORMSUBMISSION.WEB_FORM_ID, webForm.id());
        row.set(OMWFFORMSUBMISSION.OWNER_ID, webForm.getOwnerId());
        row.set(OMWFFORMSUBMISSION.PAYLOAD, data);
        row.set(OMWFFORMSUBMISSION.REF_ID, refId);
        row.set(OMWFFORMSUBMISSION.SUBMITTED_AT, Instant.now().toEpochMilli());

        orgDataStore(orgId).addRow(row);
    }
}