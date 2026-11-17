package com.tlc.crm.core.workflow.handler;

import com.tlc.commons.pair.Pair;
import com.tlc.validator.TlcModel;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.workflow.internal.trigger.EntityTrigger;
import com.tlc.crm.core.workflow.internal.converter.ActivityWorkflowConverter;
import com.tlc.crm.core.workflow.internal.converter.CompanyWorkflowConverter;
import com.tlc.crm.core.workflow.internal.converter.ContactWorkflowConverter;
import com.tlc.crm.core.workflow.internal.converter.DealWorkflowConverter;
import com.tlc.crm.core.workflow.EntityTriggerManager;

import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The handler manages the models for triggering and provides various services such as storing the
 * previous state of models and retrieving the models when needed.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerModelHandler {

    private final ContactWorkflowConverter contactWorkflowConverter;
    private final CompanyWorkflowConverter companyWorkflowConverter;
    private final DealWorkflowConverter dealWorkflowConverter;
    private final ActivityWorkflowConverter activityWorkflowConverter;
    private final EntityTriggerManager entityTriggerManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final SourceActivityManager sourceActivityManager;

    private TriggerModelHandler() {
        this.contactWorkflowConverter = ContactWorkflowConverter.getInstance();
        this.companyWorkflowConverter = CompanyWorkflowConverter.getInstance();
        this.dealWorkflowConverter = DealWorkflowConverter.getInstance();
        this.activityWorkflowConverter = ActivityWorkflowConverter.getInstance();
        this.entityTriggerManager = EntityTriggerManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
    }

    private static class Instance {
        private static final TriggerModelHandler INSTANCE = new TriggerModelHandler();
    }

    /**
     * Gets the instance of {@link TriggerModelHandler}.
     *
     * @return {@link TriggerModelHandler} instance.
     */
    public static TriggerModelHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates the entity data of previous values into the database.
     *
     * @param orgId  Refers the organization id.
     * @param source Refers the source of the model.
     * @param model  Refers the model to be loaded.
     */
    public Long create(final Long orgId, final Source source, final TlcModel model) {
        return entityTriggerManager.create(new EntityTrigger(model.id(), orgId, source, getPriorStateJson(source, model)));
    }

    /**
     * Gets the {@link JsonObject} of model.
     *
     * @param source Refers the source of the model.
     * @param model  Refers the model which is to be converted into the {@link JsonObject}.
     * @return The converted json.
     */
    public JsonObject getPriorStateJson(final Source source, final TlcModel model) {
        return switch (source) {
            case COMPANY -> companyWorkflowConverter.convert((Company) model);
            case CONTACT -> contactWorkflowConverter.convert((Contact) model);
            case DEAL -> dealWorkflowConverter.convert((Deal) model);
            case ACTIVITY -> activityWorkflowConverter.convert((ActivityHolder) model);
            default -> Json.object();
        };
    }

    /**
     * Fetches the old models for the given entity ids and source.
     *
     * @param orgId            Refers the organization id.
     * @param entityTriggerIds Refers the entity trigger ids of the model.
     * @param source           Refers the source of the model.
     * @return The {@link Map} comprises model IDs as keys and their corresponding models as values.
     */
    public Map<Long, TlcModel> fetchOldModels(final Long orgId, final Collection<Long> entityTriggerIds, final Source source, final Map<Long, Long> entityStateIdMap) {
        final Map<Long, TlcModel> previousModels = new HashMap<>();
        final Collection<EntityTrigger> entityTriggers = entityTriggerManager.get(orgId, entityTriggerIds, source);

        for (final EntityTrigger entityTrigger : entityTriggers) {
            final Long entityStateId = entityTrigger.id();
            final JsonObject priorStateJson = entityTrigger.priorState();

            switch (source) {
                case COMPANY -> {
                    final Company company = companyWorkflowConverter.convert(priorStateJson);
                    final Long companyId = company.id();

                    previousModels.put(companyId, company);
                    entityStateIdMap.put(companyId, entityStateId);
                }
                case CONTACT -> {
                    final Contact contact = contactWorkflowConverter.convert(priorStateJson);
                    final Long contactId = contact.id();

                    previousModels.put(contactId, contact);
                    entityStateIdMap.put(contactId, entityStateId);
                }
                case DEAL -> {
                    final Deal deal = dealWorkflowConverter.convert(priorStateJson);
                    final Long dealId = deal.id();

                    previousModels.put(dealId, deal);
                    entityStateIdMap.put(dealId, entityStateId);
                }
                case ACTIVITY -> {
                    final ActivityHolder activityHolder = activityWorkflowConverter.convert(priorStateJson);
                    final Long activityHolderId = activityHolder.id();

                    previousModels.put(activityHolderId, activityHolder);
                    entityStateIdMap.put(activityHolderId, entityStateId);
                }
            }
        }
        entityTriggerManager.updateStatus(orgId, entityTriggerIds, EntityTrigger.Status.COMPLETED);

        return previousModels;
    }

    /**
     * Fetches the models for the given entity ids and source.
     *
     * @param orgId     Refers the organization id.
     * @param entityIds Refers the entity ids.
     * @param source    Refers the source of the model.
     * @return The {@link Map} comprises model IDs as keys and their corresponding models as values.
     */
    public Collection<? extends TlcModel> fetchModels(final Long orgId, final Collection<Long> entityIds, final Source source) {
        return switch (source) {
            case COMPANY -> {
                final Collection<Company> companies = companyManager.get(orgId, entityIds);

                for (final Company company : companies) {
                    companyManager.loadSummaryDetails(orgId, company);
                }

                yield companies;
            }
            case CONTACT -> {
                final Collection<Contact> contacts = contactManager.get(orgId, entityIds);

                for (final Contact contact : contacts) {
                    contactManager.loadSummaryDetails(orgId, contact);
                }

                yield contacts;
            }
            case DEAL -> {
                final Collection<Deal> deals = dealManager.get(orgId, entityIds);

                for (final Deal deal : deals) {
                    dealManager.loadSummaryDetails(orgId, deal);
                }
                yield deals;
            }
            case ACTIVITY -> sourceActivityManager.get(orgId, entityIds);
            default -> null;
        };
    }

    /**
     * Fetches the model pair for the given entity id and source.
     *
     * @param orgId      Represents the organization id.
     * @param entityId   Represents the entity id.
     * @param source     Represents the source.
     * @param entityJson Represents the entity json.
     * @return The pair of models.
     */
    public Pair<TlcModel, TlcModel> fetchModelPair(final Long orgId, final Long entityId, final Source source, final JsonObject entityJson) {
        return switch (source) {
            case COMPANY -> {
                final Company company = companyManager.get(orgId, entityId);

                companyManager.loadSummaryDetails(orgId, company);
                yield Pair.of(company, companyWorkflowConverter.convert(entityJson));
            }
            case CONTACT -> {
                final Contact contact = contactManager.get(orgId, entityId);

                contactManager.loadSummaryDetails(orgId, contact);
                yield Pair.of(contact, contactWorkflowConverter.convert(entityJson));
            }
            case DEAL -> {
                final Deal deal = dealManager.get(orgId, entityId);

                dealManager.loadSummaryDetails(orgId, deal);
                yield Pair.of(deal, dealWorkflowConverter.convert(entityJson));
            }
            case ACTIVITY ->
                    Pair.of(sourceActivityManager.get(orgId, entityId), activityWorkflowConverter.convert(entityJson));
            default -> null;
        };
    }

    /**
     * Fetches the model for the given entity id and source.
     *
     * @param orgId    Represents the organization id.
     * @param entityId Represents the entity id.
     * @param source   Represents the source.
     * @return The model.
     */
    public TlcModel fetchModel(final Long orgId, final Long entityId, final Source source) {
        return switch (source) {
            case COMPANY -> {
                final Company company = companyManager.get(orgId, entityId);

                companyManager.loadSummaryDetails(orgId, company);
                yield company;
            }
            case CONTACT -> {
                final Contact contact = contactManager.get(orgId, entityId);

                contactManager.loadSummaryDetails(orgId, contact);
                yield contact;
            }
            case DEAL -> {
                final Deal deal = dealManager.get(orgId, entityId);

                dealManager.loadSummaryDetails(orgId, deal);
                yield deal;
            }
            case ACTIVITY -> sourceActivityManager.get(orgId, entityId);
            default -> null;
        };
    }

}