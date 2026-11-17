package com.tlc.crm.core.api.deal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.auto.assignment.AutoAssignment;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.validator.annotation.DealValidator;
import com.tlc.crm.core.workflow.internal.annotation.Trigger;
import com.tlc.crm.core.zapier.ZapTrigger;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.entity.validator.annotation.FieldsValidator;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Model which represents the Deal
 *
 * @author Sundar
 * @version 1.0
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.DEAL_ASSIGNED, EmailNotificationTemplate.DEAL_TRANSFERRED})
@Trigger(source = Source.DEAL)
@Notifiable(types = AppNotificationEventType.DEAL_ASSIGNED, modelName = CrmModelType.DEAL)
@Searchable(index = SearchIndex.DEAL)
@FieldsValidator(groups = {Create.class, Update.class}, message = "i18n.entity.fields.validation.failed")
@DealValidator(groups = {Create.class, Update.class}, message = "i18n.deal.validation.failed")
@ZapTrigger(source = Source.DEAL)
@AutoAssignment(source = Source.DEAL)
public class Deal extends Entity implements CrmModel {

    public static final String ENTITY_TYPE = "Deal";

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.deal.invalid.notnull.id")
    @Min(value = 1, groups = {Update.class, Delete.class}, message = "i18n.deal.invalid.id")
    private Long id;

    @AuditEntityName
    @AuditField(aliasName = "title", displayName = "i18n.deal.fields.title")
    @SearchField(name = "name")
    @Field(aliasName = "title")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.deal.title.length.exceeded")
    private String title;

    @AuditField(aliasName = "pipeline", displayName = "i18n.deal.fields.pipeline.name")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.deal.invalid.notnull.pipeline")
    @SearchField(name = "pipeline")
    private Pipeline pipeline;

    @AuditField(aliasName = "pipelineStage", displayName = "i18n.deal.fields.pipeline.stage.name")
    @SearchField(name = "pipelineStage")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.deal.invalid.notnull.pipelineStage")
    private PipelineStage pipelineStage;

    @SearchField(name = "ownerId")
    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.DEAL_ASSIGNED, EmailNotificationTemplate.DEAL_TRANSFERRED},
            placeholderTemplates = {EmailNotificationTemplate.DEAL_ASSIGNED, EmailNotificationTemplate.DEAL_TRANSFERRED},
            requirePreviousValue = true
    )
    private Long ownerId;

    @AuditField(aliasName = "primaryContact", displayName = "i18n.deal.fields.primary.contact.placeholder")
    @SearchField(name = "contact")
    @Field(aliasName = "primaryContact", autoValidate = false)
    private Contact primaryContact;

    @AuditField(aliasName = "company", displayName = "i18n.deal.fields.company.name.placeholder")
    @SearchField(name = "company")
    @Field(aliasName = "company", autoValidate = false)
    private Company company;

    @AuditField(aliasName = "expectedCloseDate", displayName = "i18n.deal.fields.expected.close.date")
    @SearchField(name = "expectedCloseDate")
    @Field(aliasName = "expectedCloseDate")
    private Date expectedCloseDate;

    @SearchField(name = "dealState")
    @NotNull(groups = {Update.class}, message = "i18n.deal.invalid.notnull.state")
    private State state;

    @Field(aliasName = "dealSource")
    private Option source;

    // TODO: We need to move the campaign validation to DealValidatorImpl
    private String campaign;

    @Field(aliasName = "description")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.deal.description.length.exceeded")
    private String description;

    @Field(aliasName = "dealClosedOn", autoValidate = false)
    private DateTime closedTime;

    @Field(aliasName = "lostReason", autoValidate = false)
    private Option lostReason;

    @Field(aliasName = "wonReason", autoValidate = false)
    private Option wonReason;

    @Field(aliasName = "type")
    private Option type;

    @Field(aliasName = "paymentStatus")
    private Option paymentStatus;

    private Monetary dealValueInBaseCurrency;

    private Monetary productAmount;

    public Collection<Participant> getParticipants() {
        return Objects.isNull(participants) ? Collections.emptyList() : participants;
    }

    public void setParticipants(final Collection<Participant> participants) {
        this.participants = participants;
    }

    public void addParticipant(final Participant participant) {
        this.participants = this.participants == null ? new ArrayList<>() : this.participants;

        this.participants.add(participant);
    }

    @Field(aliasName = "relatedContacts", autoValidate = false)
    private Collection<Participant> participants;

    private DateTime stageUpdatedAt;

    @SearchField(name = "totalProductCount")
    private long totalProductCount;

    private Long age;

    private Long inactiveDays;

    @Valid
    private transient SummaryInfo summaryData;

    private Double stageProbability;

    private Monetary expectedDealValue;

    private DateTime wonTime;

    private DateTime lostTime;

    private Monetary weightedRevenue;

    private long rottenDays;

    @SearchField(name = "dealValue")
    @Field(aliasName = "dealValue")
    private DealValue dealValue;

    @SearchField(name = "territory")
    private Territory territory;

    private boolean notifiable;

    private String recentNote;

    private ProductDetailsHolder productDetailsHolder;

    private Status status;

    private boolean isArchived;

    private ForecastCategory forecastCategory;

    public ProductDetailsHolder getProductDetailsHolder() {
        return productDetailsHolder;
    }

    public void setProductDetailsHolder(final ProductDetailsHolder productDetailsHolder) {
        this.productDetailsHolder = productDetailsHolder;
    }

    public void setSalesOwner(final CrmUser salesOwner) {
        this.ownerId = salesOwner.id();
        super.setSalesOwner(salesOwner);
    }

    public Territory getTerritory() {
        return territory;
    }

    public void setTerritory(final Territory territory) {
        this.territory = territory;
    }

    public ForecastCategory getForecastCategory() {
        return forecastCategory;
    }

    public void setForecastCategory(final ForecastCategory forecastCategory) {
        this.forecastCategory = forecastCategory;
    }

    public enum State implements TlcModel, OptionJsonConverter<State> {
        OPEN("open",  "Open", 1),
        WON("won",  "Won", 2),
        LOST("lost", "Lost", 3);

        @SearchField(name = "name")
        private final String name;
        @SearchField(name = "id", readMethodName = "id")
        private final int id;
        private final String displayName;

        State(final String name, final String displayName, final int id) {
            this.name = Objects.requireNonNull(name);
            this.id = id;
            this.displayName = displayName;
        }

        /**
         * <p>
         * Gets the id of the state
         * </p>
         *
         * @return the id of State
         */
        public int getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the name of the state
         * </p>
         *
         * @return the name of State
         */
        public String getName() {
            return this.name;
        }

        /**
         * <p>
         * Gets the display name of the state
         * </p>
         *
         * @return The name of State
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Gets the {@link State} by id
         *
         * @param id represents the id of the State
         * @return {@link State}
         */
        public static State getStateById(final int id) {
            return switch (id) {
                case 1 -> OPEN;
                case 2 -> WON;
                case 3 -> LOST;
                default -> throw ErrorCode.get(DealErrorCodes.INVALID_DEAL_STATE, "i18n.deal.state.invalid");
            };
        }

        /**
         * Gets the {@link State} by name
         *
         * @param name represents the name of the state
         * @return {@link State}
         */
        public static State getStateByName(final String name) {
            return switch (name.toLowerCase()) {
                case "won" -> WON;
                case "lost" -> LOST;
                default -> OPEN;
            };
        }

        /**
         * Gets the {@link State} by name
         *
         * @param name represents the name of the state
         * @return {@link State}
         */
        public static State getState(final String name) {
            return switch (name.toLowerCase()) {
                case "won" -> WON;
                case "lost" -> LOST;
                case "open" -> OPEN;
                default -> throw ErrorCode.get(DealErrorCodes.INVALID_DEAL_STATE, "i18n.deal.state.invalid");
            };
        }

        /**
         * <p>
         * Checks if the current stage name is equal to the provided name is case sensitive or not
         * </p>
         *
         * @param name Represents the name of the state
         * @return If the name is equal, it will return true, otherwise false
         */
        public boolean equalsIgnoreCase(final String name) {
            return this.name.equalsIgnoreCase(name);
        }

        @Override
        public Long id() {
            return (long) this.id;
        }

        @Override
        public Long orgId() {
            return null;
        }

        @Override
        public Object identity() {
            return null;
        }

        /**
         * Converts the specified {@code State} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
         *
         * @param state the {@code State} object to be converted
         * @param converterConfig the {@code ConverterConfig} containing configuration details.
         * @return a {@code JsonObject} representing the converted state with fields for value and name
         */
        public JsonObject convertModelToOptionJson(final State state, final ConverterConfig converterConfig) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(FieldKeys.VALUE.getName(), state.id);
            jsonObject.put(FieldKeys.NAME.getName(), I18nServiceProvider.get().getResolver(converterConfig.locale()).get(state.displayName));

            return jsonObject;
        }
    }

    /**
     * <p>
     * An enum that contains the deal status type
     * </p>
     */
    public enum Status {
        ACTIVE((short) 1),
        ARCHIVED((short)2),
        ;

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link Status}
         * </p>
         *
         * @param id Represents the status Id
         * @return The {@link Status}
         */
        public static Status get(final short id) {
           return ACTIVE.id == id ? ACTIVE : ARCHIVED;
        }
    }

    public Deal() {
        super(ENTITY_TYPE);
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DealValue getDealValue() {
        return dealValue;
    }

    public void setDealValue(final DealValue dealValue) {
        this.dealValue = dealValue;
    }

    public Date getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(final Date expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public PipelineStage getPipelineStage() {
        return pipelineStage;
    }

    public void setPipelineStage(PipelineStage pipelineStage) {
        this.pipelineStage = pipelineStage;
    }

    public Contact getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(Contact primaryContact) {
        this.primaryContact = primaryContact;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public Option getSource() {
        return source;
    }

    public void setSource(final Option source) {
        this.source = source;
    }

    public DateTime getClosedTime() {
        return closedTime;
    }

    public void setClosedTime(final DateTime closedTime) {
        this.closedTime = closedTime;
    }

    public Option getLostReason() {
        return lostReason;
    }

    public void setLostReason(final Option lostReason) {
        this.lostReason = lostReason;
    }

    public Option getWonReason() {
        return wonReason;
    }

    public void setWonReason(final Option wonReason) {
        this.wonReason = wonReason;
    }

    public Option getType() {
        return type;
    }

    public void setType(final Option type) {
        this.type = type;
    }

    public Option getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(final Option paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(final String campaign) {
        this.campaign = campaign;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public DateTime getStageUpdatedAt() {
        return stageUpdatedAt;
    }

    public void setStageUpdatedAt(final DateTime stageUpdatedAt) {
        this.stageUpdatedAt = stageUpdatedAt;
    }

    public long getTotalProductCount() {
        return totalProductCount;
    }

    public void setTotalProductCount(final long totalProductCount) {
        this.totalProductCount = totalProductCount;
    }

    public void setAge(final Long age) {
        this.age = age;
    }

    public Long getAge() {
        return age;
    }

    public void setInactiveDays(final Long inactiveDays) {
        this.inactiveDays = inactiveDays;
    }

    public Long getInactiveDays() {
        return inactiveDays;
    }

    public SummaryInfo getSummaryInfo() {
        return summaryData;
    }

    public void setSummaryInfo(final SummaryInfo summaryData) {
        this.summaryData = summaryData;
    }

    public Double getStageProbability() {
        return stageProbability;
    }

    public void setStageProbability(final Double stageProbability) {
        this.stageProbability = stageProbability;
    }

    public Monetary getExpectedDealValue() {
        return expectedDealValue;
    }

    public void setExpectedDealValue(final Monetary expectedDealValue) {
        this.expectedDealValue = expectedDealValue;
    }

    public DateTime getWonTime() {
        return wonTime;
    }

    public void setWonTime(final DateTime wonTime) {
        this.wonTime = wonTime;
    }

    public DateTime getLostTime() {
        return lostTime;
    }

    public void setLostTime(final DateTime lostTime) {
        this.lostTime = lostTime;
    }

    public Monetary getWeightedRevenue() {
        return weightedRevenue;
    }

    public void setWeightedRevenue(final Monetary weightedRevenue) {
        this.weightedRevenue = weightedRevenue;
    }

    public long getRottenDays() {
        return rottenDays;
    }

    public void setRottenDays(final long rottenDays) {
        this.rottenDays = rottenDays;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean notifiable() {
        return notifiable;
    }

    @Override
    public void setNotifiable(final boolean notifiable) {
        this.notifiable = notifiable;
    }

    public String getRecentNote() {
        return recentNote;
    }

    public void setRecentNote(final String recentNote) {
        this.recentNote = recentNote;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Monetary getDealValueInBaseCurrency() {
        return dealValueInBaseCurrency;
    }

    public void setDealValueInBaseCurrency(final Monetary dealValueInBaseCurrency) {
        this.dealValueInBaseCurrency = dealValueInBaseCurrency;
    }

    public Monetary getProductAmount() {
        return productAmount;
    }

    public void setProductAmount(final Monetary productAmount) {
        this.productAmount = productAmount;
    }

    public boolean isRotten() {
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final DateTime modifiedTime = getModifiedTime();

        if (Objects.isNull(modifiedTime)) {
            return false;
        } else {
            final long timeDifference = timeUnit.toDays(Instant.now().toEpochMilli()) - timeUnit.toDays(modifiedTime.getTimeStamp());
            final Pipeline dealPipeline = getPipeline();

            if (Objects.isNull(dealPipeline)) {
                throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
            }

            return timeDifference > pipeline.getRottenDays();
        }
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setStatus(final Short statusId) {
        status = Status.get(statusId);
        isArchived = Status.ARCHIVED == status;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the deal
     */
    @Override
    public String getName() {
        return title;
    }
}