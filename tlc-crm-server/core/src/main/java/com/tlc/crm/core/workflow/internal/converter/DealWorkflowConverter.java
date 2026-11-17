package com.tlc.crm.core.workflow.internal.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.converter.PipelineConverter;
import com.tlc.crm.core.api.deal.model.converter.PipelineStageConverter;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.converter.ProductDetailsHolderConverter;
import com.tlc.crm.core.api.deal.model.converter.DealValueConverter;
import com.tlc.crm.core.field.deal.ProductDetailsHolderFields;
import com.tlc.crm.core.field.summary.SummaryFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The converter that facilitates different ways to convert data from/to {@link Deal}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class DealWorkflowConverter extends EntityWorkflowConverter<Deal> {

    private final PipelineConverter pipelineConverter;
    private final PipelineStageConverter pipelineStageConverter;
    private final ProductDetailsHolderConverter productDetailsHolderConverter;
    private final DealValueConverter dealValueConverter;
    private final DealManager dealManager;
    private final DateTimeUtil dateTimeUtil;
    private final DateUtil dateUtil;

    private DealWorkflowConverter() {
        this.pipelineConverter = PipelineConverter.getInstance();
        this.pipelineStageConverter = PipelineStageConverter.getInstance();
        this.productDetailsHolderConverter = ProductDetailsHolderConverter.getInstance();
        this.dealValueConverter = DealValueConverter.getInstance();
        this.dealManager = DealManager.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.dateUtil = DateUtil.getInstance();
    }

    private static class Instance {
        private static final DealWorkflowConverter INSTANCE = new DealWorkflowConverter();
    }

    public static DealWorkflowConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal Refers the model.
     * @return The converted json.
     */
    @Override
    public JsonObject convert(final Deal deal) {
        final JsonObject dealJson = Json.object();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final ValueHolder valueHolder = deal.getValueHolder();
        final Company company = deal.getCompany();
        final DealValue dealValue = deal.getDealValue();
        final Collection<Participant> dealParticipants = deal.getParticipants();
        final Territory territory = deal.getTerritory();
        final Option lostReason = deal.getLostReason();
        final Option wonReason = deal.getWonReason();
        final Option paymentStatus = deal.getPaymentStatus();
        final Option dealSource = deal.getSource();
        final Option type = deal.getType();
        final CrmUser createdBy = deal.getCreatedBy();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final String idKey = DealFields.ID;
        final JsonObject monetaryJson;
        final ProductDetailsHolder productDetailsHolder;
        final Contact primaryContact = deal.getPrimaryContact();

        if (Objects.isNull(dealValue)) {
            monetaryJson = null;
            productDetailsHolder = null;
        } else {
            monetaryJson = dealValueConverter.convert(dealValue);
            productDetailsHolder = deal.getProductDetailsHolder();
        }
        dealJson.put(idKey, deal.id());
        dealJson.put(DealFields.ORG_ID, deal.orgId());
        dealJson.put(DealFields.CREATED_TIME, dateTimeUtil.getTimeStamp(deal.getCreatedTime()));
        dealJson.put(DealFields.UPDATED_TIME, dateTimeUtil.getTimeStamp(deal.getModifiedTime()));
        dealJson.put(fields.get(DealFields.TITLE).idAsString(), deal.getTitle());
        dealJson.put(fields.get(DealFields.EXPECTED_CLOSE_DATE).idAsString(), dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        dealJson.put(fields.get(DealFields.PIPELINE).idAsString(), pipelineConverter.convert(deal.getPipeline()));
        dealJson.put(fields.get(DealFields.PIPELINE_STAGE).idAsString(), pipelineStageConverter.convert(deal.getPipelineStage()));
        dealJson.put(fields.get(DealFields.DESCRIPTION).idAsString(), deal.getDescription());
        dealJson.put(fields.get(DealFields.STATUS).idAsString(), deal.getState().getId());
        dealJson.put(fields.get(DealFields.SALES_OWNER).idAsString(), deal.getSalesOwner().id());
        dealJson.put(fields.get(DealFields.DEAL_CLOSED_ON).idAsString(), dateTimeUtil.getTimeStamp(deal.getClosedTime()));
        dealJson.put(CompanyFields.RECENT_NOTE, deal.getRecentNote());

        if (Objects.nonNull(primaryContact)) {
            dealJson.put(fields.get(DealFields.PRIMARY_CONTACT).idAsString(), primaryContact.id());
        }

        if (Objects.nonNull(company)) {
            dealJson.put(fields.get(DealFields.COMPANY).idAsString(), company.id());
        }

        if (Objects.nonNull(dealParticipants) && !dealParticipants.isEmpty()) {
            final JsonArray participants = Json.array();

            for (final Participant participant : dealParticipants) {
                participants.put(Json.object().put(idKey, participant.getContact().id()));
            }
            dealJson.put(fields.get(DealFields.RELATED_CONTACTS).idAsString(), participants);
        }

        if (Objects.nonNull(monetaryJson)) {
            if (Objects.nonNull(productDetailsHolder)) {
                final Map<StoreSubType, FieldSubType> dealValueSubTypes = FieldTypeService.getInstance().groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);

                productDetailsHolder.setManuallyAdded(false);
                monetaryJson.put(dealValueSubTypes.get(StoreSubType.PRODUCT).idAsString(), productDetailsHolderConverter.convert(productDetailsHolder));
            } else {
                monetaryJson.put(DealFields.IS_MANUALLY_ADDED, true);
            }
            dealJson.put(fields.get(DealFields.DEAL_VALUE).idAsString(), monetaryJson);
        }

        if (Objects.nonNull(dealSource)) {
            dealJson.put(fields.get(DealFields.DEAL_SOURCE).idAsString(), dealSource.id());
        }

        if (Objects.nonNull(type)) {
            dealJson.put(fields.get(DealFields.TYPE).idAsString(), type.id());
        }

        if (Objects.nonNull(paymentStatus)) {
            dealJson.put(fields.get(DealFields.PAYMENT_STATUS).idAsString(), paymentStatus.id());
        }

        if (Objects.nonNull(lostReason)) {
            dealJson.put(fields.get(DealFields.LOST_REASON).idAsString(), lostReason.id());
        }

        if (Objects.nonNull(wonReason)) {
            dealJson.put(fields.get(DealFields.WON_REASON).idAsString(), wonReason.id());
        }

        if (Objects.nonNull(createdBy)) {
            dealJson.put(fields.get(DealFields.CREATED_BY).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy)) {
            dealJson.put(fields.get(DealFields.UPDATED_BY).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(territory)) {
            dealJson.put(fields.get(DealFields.TERRITORY).idAsString(), territory.id());
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(dealJson);
        }
        final JsonObject summaryJson = summaryInfoConverter.convert(deal.getSummaryInfo());

        if (summaryJson.size() > 0) {
            dealJson.put(SummaryFields.SUMMARY, summaryJson);
        }

        return dealJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the json object.
     * @return The {@link Deal}.
     */
    @Override
    public Deal convert(final JsonObject jsonObject) {
        final Long orgId = jsonObject.getLong(DealFields.ORG_ID);
        final String createdByKey = DealFields.CREATED_BY;
        final String idKey = DealFields.ID;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        final Long ownerId = jsonObject.getLong(fields.get(createdByKey).idAsString());
        final ConverterConfig converterConfig = new ConverterConfig(orgId, ownerId, null, false, jsonObject);
        final Deal deal = new Deal();
        final String titleId = fields.get(DealFields.TITLE).idAsString();
        final String contactKey = fields.get(DealFields.PRIMARY_CONTACT).idAsString();
        final String companyKey = fields.get(DealFields.COMPANY).idAsString();
        final String participantsKey = fields.get(DealFields.RELATED_CONTACTS).idAsString();
        final String pipelineKey = fields.get(DealFields.PIPELINE).idAsString();
        final String stageKey = fields.get(DealFields.PIPELINE_STAGE).idAsString();
        final FieldInfo stateField = fields.get(DealFields.STATUS);
        final FieldInfo typeField = fields.get(DealFields.TYPE);
        final FieldInfo sourceField = fields.get(DealFields.DEAL_SOURCE);
        final FieldInfo lostReasonField = fields.get(DealFields.LOST_REASON);
        final FieldInfo wonReasonField = fields.get(DealFields.WON_REASON);
        final FieldInfo paymentField = fields.get(DealFields.PAYMENT_STATUS);
        final String monetaryKey = fields.get(DealFields.DEAL_VALUE).idAsString();
        final String stateIdKey = stateField.idAsString();
        final String typeKey = typeField.idAsString();
        final String sourceKey = sourceField.idAsString();
        final String lostReasonKey = lostReasonField.idAsString();
        final String wonReasonKey = wonReasonField.idAsString();
        final String paymentKey = paymentField.idAsString();
        final String closeDateIdKey = fields.get(DealFields.EXPECTED_CLOSE_DATE).idAsString();
        final String closedTimeKey = fields.get(DealFields.DEAL_CLOSED_ON).idAsString();
        final String descriptionKey = fields.get(DealFields.DESCRIPTION).idAsString();
        final String salesOwnerKey = fields.get(DealFields.SALES_OWNER).idAsString();
        final String territoryKey = fields.get(DealFields.TERRITORY).idAsString();
        final CrmUser user = userService.getUser(orgId, ownerId);

        deal.setOrgId(orgId);
        deal.setId(jsonObject.getLong(idKey));
        deal.setCreatedTime(dateTimeUtil.getTemporal(jsonObject.getLong(DealFields.CREATED_TIME)));
        deal.setModifiedTime(dateTimeUtil.getTemporal(jsonObject.getLong(DealFields.UPDATED_TIME)));
        deal.setCreatedBy(user);
        deal.setTitle(jsonObject.containsKey(titleId) ? jsonObject.getString(titleId) : deal.getTitle());
        deal.setDescription(jsonObject.containsKey(descriptionKey) ? jsonObject.getString(descriptionKey) : deal.getDescription());
        deal.setSource(jsonObject.containsKey(sourceKey)
                ? optionConverter.convert(orgId, sourceField.id(), jsonObject.getLong(sourceKey)) : deal.getSource());
        deal.setState(jsonObject.containsKey(stateIdKey)
                ? Deal.State.getStateById(jsonObject.getInt(stateIdKey)) : Deal.State.OPEN);
        deal.setLostReason(jsonObject.containsKey(lostReasonKey)
                ? optionConverter.convert(orgId, lostReasonField.id(), jsonObject.getLong(lostReasonKey)) : deal.getLostReason());
        deal.setWonReason(jsonObject.containsKey(wonReasonKey)
                ? optionConverter.convert(orgId, wonReasonField.id(), jsonObject.getLong(wonReasonKey)) : deal.getWonReason());
        deal.setType(jsonObject.containsKey(typeKey)
                ? optionConverter.convert(orgId, typeField.id(), jsonObject.getLong(typeKey)) : deal.getType());
        deal.setPaymentStatus(jsonObject.containsKey(paymentKey)
                ? optionConverter.convert(orgId, paymentField.id(), jsonObject.getLong(paymentKey)) : deal.getPaymentStatus());
        deal.setSalesOwner(userService.getUser(orgId, jsonObject.containsKey(salesOwnerKey) ? jsonObject.getLong(salesOwnerKey) : ownerId));
        deal.setUpdatedBy(user);
        deal.setRecentNote(jsonObject.optString(CompanyFields.RECENT_NOTE, null));

        if (jsonObject.containsKey(closedTimeKey)) {
            deal.setClosedTime(dateTimeUtil.getTemporal(jsonObject.getLong(closedTimeKey)));
        }

        if (jsonObject.containsKey(territoryKey)) {
            final Territory territory = new Territory();

            territory.setId(jsonObject.getLong(territoryKey));
            deal.setTerritory(territory);
        }

        if (jsonObject.containsKey(closeDateIdKey)) {
            deal.setExpectedCloseDate(dateUtil.getTemporal(jsonObject.getLong(closeDateIdKey)));
        }

        if (jsonObject.containsKey(contactKey)) {
            final Contact contact = new Contact();

            contact.setId(jsonObject.getLong(contactKey));
            contact.setOrgId(orgId);
            deal.setPrimaryContact(contact);
        }

        if (jsonObject.containsKey(companyKey)) {
            final Company company = new Company();

            company.setId(jsonObject.getLong(companyKey));
            company.setOrgId(orgId);
            deal.setCompany(company);
        }

        if (jsonObject.containsKey(participantsKey)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(participantsKey);

            for (int index = 0; index < jsonArray.size(); index++) {
                final JsonObject participantJson = jsonArray.optJsonObject(index);
                final Participant participant = new Participant();
                final Contact contact = new Contact();

                contact.setId(participantJson.getLong(idKey));
                contact.setOrgId(orgId);
                participant.setContact(contact);
                deal.addParticipant(participant);
            }
        }

        if (jsonObject.containsKey(pipelineKey)) {
            deal.setPipeline(pipelineConverter.convert(converterConfig, jsonObject.getJsonObject(pipelineKey)));
        }

        if (jsonObject.containsKey(stageKey)) {
            deal.setPipelineStage(pipelineStageConverter.convert(converterConfig, jsonObject.getJsonObject(stageKey)));
        }

        if (jsonObject.containsKey(monetaryKey)) {
            final JsonObject monetaryJson = jsonObject.getJsonObject(monetaryKey);
            final Map<StoreSubType, FieldSubType> dealValueSubTypes = FieldTypeService.getInstance().groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);
            final String productsKey = dealValueSubTypes.get(StoreSubType.PRODUCT).idAsString();

            if (monetaryJson.containsKey(productsKey)) {
                final JsonObject productsJson = monetaryJson.optJsonObject(productsKey);

                productsJson.put(ProductDetailsHolderFields.DEAL_ID.getName(), deal.id());
                productsJson.put(ProductDetailsHolderFields.CURRENCY.getName(), monetaryJson.getLong(dealValueSubTypes.get(StoreSubType.CURRENCY).idAsString()));

                final ProductDetailsHolder productDetailsHolder = productDetailsHolderConverter.convert(converterConfig, productsJson);

                if (Objects.nonNull(productDetailsHolder)) {
                    final DealValue dealValue = new DealValue();

                    if (monetaryJson.containsKey(FieldKeys.ID.getName())) {
                        dealValue.setId(monetaryJson.getLong(FieldKeys.ID.getName()));
                    }
                    dealValue.setOrgId(orgId);
                    dealValue.setCurrency(productDetailsHolder.getCurrency());
                    dealValue.setValue(BigDecimal.valueOf(productDetailsHolder.getTotalAmount(productDetailsHolder)));
                    deal.setProductDetailsHolder(productDetailsHolder);

                    deal.setDealValue(dealValue);
                }
            } else {
                final Monetary monetary = monetaryConverter.convert(converterConfig, jsonObject.getJsonObject(monetaryKey));

                deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
            }
        }
        entityBuilder.buildValueHolder(deal, converterConfig, jsonObject);

        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final long currentTime = Instant.now().toEpochMilli();
        final PipelineStage dealPipelineStage = deal.getPipelineStage();
        final Double stageProbability = dealPipelineStage.getProbability();

        deal.setRottenDays(timeUnit.toDays(currentTime) - timeUnit.toDays(dateTimeUtil.getTimeStamp(deal.getModifiedTime())));
        deal.setAge(timeUnit.toDays(currentTime - dateTimeUtil.getTimeStamp(deal.getCreatedTime())));
        deal.setStageProbability(stageProbability);

        final BigDecimal totalDealValue = dealManager.getDealsValueByPipelineAndStage(orgId, deal.getPipeline().id(),
                dealPipelineStage.id());
        final Monetary weightedValue = new Monetary();

        weightedValue.setValue(BigDecimal.valueOf((totalDealValue.multiply(BigDecimal.valueOf(stageProbability)).doubleValue()) / 100));
        deal.setWeightedRevenue(weightedValue);

        final Monetary value = deal.getDealValue();

        if (Objects.nonNull(value)) {
            final Monetary expectedValue = new Monetary();

            expectedValue.setOrgId(orgId);
            expectedValue.setValue(value.getValue().multiply(BigDecimal.valueOf(stageProbability / 100)));
            expectedValue.setCurrency(value.getCurrency());
            deal.setExpectedDealValue(expectedValue);
        }

        if (jsonObject.containsKey(SummaryFields.SUMMARY)) {
            deal.setSummaryInfo(summaryInfoConverter.convert(converterConfig, jsonObject.getJsonObject(SummaryFields.SUMMARY)));
        }

        return deal;
    }

}
