package com.tlc.crm.zapier.internal.event.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;

import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.zapier.internal.event.ZapEventData;

import com.tlc.i18n.I18nResolver;

import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the conversion of model data into a JSON format suitable for Zapier event payloads.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DealZapEventDataConverter extends AbstractZapEventDataConverter<Deal> {

    private static final class Instance {
        private static final DealZapEventDataConverter EVENT_DATA_CONVERTER = new DealZapEventDataConverter();
    }

    public static DealZapEventDataConverter getInstance() {
        return Instance.EVENT_DATA_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal         The specific model instance to convert.
     * @param zapEventData The {@code ZapEventData}.
     * @return {@link JsonObject} representing the converted model data.
     */
    @Override
    protected JsonObject convert(final Deal deal, final ZapEventData zapEventData) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(deal.orgId(), Source.DEAL.getName());
        final JsonObject dealJson = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(zapEventData.getLocale());
        final Map<Long, User> userMap = zapEventData.getGroupedUsers();
        final ValueHolder valueHolder = deal.getValueHolder();

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(fieldInfo, valueHolder.get(fieldInfo.id()), dealJson, i18nResolver, userMap);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "title" -> dealJson.put(aliasName, deal.getTitle());
                    case "pipeline" -> {
                        final Pipeline pipeline = deal.getPipeline();
                        final JsonObject pipelineJson = Json.object();

                        pipelineJson.put(NAME, pipeline.getName());
                        pipelineJson.put(ID, pipeline.id());

                        dealJson.put(aliasName, pipelineJson);
                    }
                    case "pipelineStage" -> {
                        final PipelineStage pipelineStage = deal.getPipelineStage();
                        final JsonObject pipelineStageJson = Json.object();

                        pipelineStageJson.put(NAME, pipelineStage.getName());
                        pipelineStageJson.put(ID, pipelineStage.id());

                        dealJson.put(aliasName, pipelineStageJson);
                    }
                    case "wonReason" -> {
                        final Option wonReason = deal.getWonReason();

                        if (Objects.nonNull(wonReason)) {
                            dealJson.put(aliasName, i18nResolver.get(wonReason.getOption()));
                        }
                    }
                    case "lostReason" -> {
                        final Option lostReason = deal.getLostReason();

                        if (Objects.nonNull(lostReason)) {
                            dealJson.put(aliasName, i18nResolver.get(lostReason.getOption()));
                        }
                    }
                    case "dealClosedOn", "closedTime" -> {
                        final DateTime closedTime = deal.getClosedTime();

                        if (Objects.nonNull(closedTime)) {
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(closedTime.getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "wonTime" -> {
                        final DateTime wonTime = deal.getWonTime();

                        if (Objects.nonNull(wonTime)) {
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(wonTime.getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "lostTime" -> {
                        final DateTime lostTime = deal.getLostTime();

                        if (Objects.nonNull(lostTime)) {
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(lostTime.getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "primaryContact" -> {
                        final Contact contact = deal.getPrimaryContact();

                        if (Objects.nonNull(contact)) {
                            final JsonObject contactJson = Json.object();

                            contactJson.put(NAME, contact.getFullName());
                            contactJson.put(ID, contact.id());

                            dealJson.put(aliasName, contactJson);
                        }
                    }
                    case "relatedContacts" -> {
                        final Collection<Participant> participants = deal.getParticipants();

                        if (Objects.nonNull(participants)) {
                            final JsonArray array = Json.array();

                            for (final Participant participant : participants) {
                                final JsonObject participantJson = Json.object();

                                participantJson.put(NAME, participant.getContact().getFullName());
                                participantJson.put(ID, participant.id());

                                array.put(participantJson);
                            }
                            dealJson.put(aliasName, array);
                        }
                    }
                    case "company" -> {
                        final Company company = deal.getCompany();

                        if (Objects.nonNull(company)) {
                            final JsonObject companyJson = Json.object();

                            companyJson.put(NAME, company.getName());
                            companyJson.put(ID, company.id());

                            dealJson.put(aliasName, companyJson);
                        }
                    }
                    case "salesOwner" -> {
                        final JsonObject saleOwnerJson = Json.object();
                        final CrmUser salesOwner = deal.getSalesOwner();

                        saleOwnerJson.put(NAME, userMap.get(salesOwner.getIamId()).getFullName());
                        saleOwnerJson.put(ID, salesOwner.id());

                        dealJson.put(aliasName, saleOwnerJson);
                    }
                    case "dealValue" -> {
                        final Monetary dealValue = deal.getDealValue();

                        if (Objects.nonNull(dealValue)) {
                            final JsonObject subTypeJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CURRENCY).displayName()),
                                    i18nResolver.get(dealValue.getCurrency().getI18nKey()));
                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.VALUE).displayName()), dealValue.getValue());

                            dealJson.put(aliasName, subTypeJson);
                        }
                    }
                    case "products" -> {
                        final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                        if (Objects.nonNull(productDetailsHolder)) {
                            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

                            if (Objects.nonNull(productDetails)) {
                                final JsonArray array = Json.array();

                                for (final ProductDetail productDetail : productDetails) {
                                    final JsonObject productJson = Json.object();

                                    productJson.put(NAME, productDetail.getProduct().getName());
                                    productJson.put(ID, productDetail.id());

                                    array.put(productJson);
                                }
                                dealJson.put(aliasName, array);
                            }
                        }
                    }
                    case "expectedCloseDate" -> {
                        final Date expectedCloseDate = deal.getExpectedCloseDate();

                        if (Objects.nonNull(expectedCloseDate)) {
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(expectedCloseDate.getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "dealSource" -> {
                        final Option dealSource = deal.getSource();

                        if (Objects.nonNull(dealSource)) {
                            dealJson.put(aliasName, i18nResolver.get(dealSource.getOption()));
                        }
                    }
                    case "tags" -> {
                        final SummaryInfo dealSummary = deal.getSummaryInfo();

                        if (Objects.nonNull(dealSummary)) {
                            final Collection<Tag> dealTags = dealSummary.getTags();

                            if (Objects.nonNull(dealTags)) {
                                final JsonArray tags = Json.array();

                                for (final Tag tag : dealTags) {
                                    final JsonObject tagJson = Json.object();

                                    tagJson.put(NAME, tag.getName());
                                    tagJson.put(ID, tag.id());

                                    tags.put(tagJson);
                                }
                                dealJson.put(aliasName, tags);
                            }
                        }
                    }
                    case "territory" -> {
                        final Territory territory = deal.getTerritory();

                        if (Objects.nonNull(territory)) {
                            final JsonObject territoryJson = Json.object();

                            territoryJson.put(NAME, territory.getName());
                            territoryJson.put(ID, territory.id());

                            dealJson.put(aliasName, territoryJson);
                        }
                    }
                    case "rottingDays" -> dealJson.put(aliasName, deal.getRottenDays());
                    case "age" -> dealJson.put(aliasName, deal.getAge());
                    case "stageProbability" -> dealJson.put(aliasName, deal.getStageProbability());
                    case "weightedValue" -> {
                        final Monetary weightedValue = deal.getWeightedRevenue();

                        if (Objects.nonNull(weightedValue)) {
                            final JsonObject subTypeJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CURRENCY).name()),
                                    i18nResolver.get(weightedValue.getCurrency().getI18nKey()));
                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.VALUE).name()), weightedValue.getValue());

                            dealJson.put(aliasName, subTypeJson);
                        }
                    }
                    case "expectedDealValue" -> {
                        final Monetary expectedDealValue = deal.getExpectedDealValue();

                        if (Objects.nonNull(expectedDealValue)) {
                            final JsonObject subTypeJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.CURRENCY).name()),
                                    i18nResolver.get(expectedDealValue.getCurrency().getI18nKey()));
                            subTypeJson.put(i18nResolver.get(fieldSubTypes.get(StoreSubType.VALUE).name()), expectedDealValue.getValue());

                            dealJson.put(aliasName, subTypeJson);
                        }
                    }
                    case "description" -> dealJson.put(aliasName, deal.getDescription());
                    case "type" -> {
                        final Option type = deal.getType();

                        if (Objects.nonNull(type)) {
                            dealJson.put(aliasName, i18nResolver.get(type.getOption()));
                        }
                    }
                    case "paymentStatus" -> {
                        final Option paymentStatus = deal.getPaymentStatus();

                        if (Objects.nonNull(paymentStatus)) {
                            dealJson.put(aliasName, i18nResolver.get(paymentStatus.getOption()));
                        }
                    }
                    case "status" -> dealJson.put(aliasName, deal.getState().getName());
                    case "updatedAt" ->
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(deal.getModifiedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "dealStageUpdatedAt" -> {
                        final DateTime stageUpdatedAt = deal.getStageUpdatedAt();

                        if (Objects.nonNull(stageUpdatedAt)) {
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(stageUpdatedAt.getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "dealCreated" ->
                            dealJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(deal.getCreatedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "createdBy" -> {
                        final JsonObject creatorJson = Json.object();
                        final CrmUser creator = deal.getCreatedBy();

                        creatorJson.put(NAME, userMap.get(creator.getIamId()).getFullName());
                        creatorJson.put(ID, creator.id());

                        dealJson.put(aliasName, creatorJson);
                    }
                    case "updatedBy" -> {
                        final JsonObject updaterJson = Json.object();
                        final CrmUser updatedBy = deal.getUpdatedBy();

                        updaterJson.put(NAME, userMap.get(updatedBy.getIamId()).getFullName());
                        updaterJson.put(ID, updatedBy.id());

                        dealJson.put(aliasName, updaterJson);
                    }
                }
            }
        }

        return dealJson;
    }
}
