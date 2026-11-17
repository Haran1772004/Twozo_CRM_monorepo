package com.tlc.crm.core.api.contact.model;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.auto.assignment.AutoAssignment;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.core.validator.annotation.CompanyValidator;
import com.tlc.crm.core.workflow.internal.annotation.Trigger;
import com.tlc.crm.core.zapier.ZapTrigger;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.type.Address;
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
import com.tlc.validator.type.Group;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import com.tlc.validator.type.URL;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 *     A model that represents the company
 * </p>
 */
@Trigger(source = Source.COMPANY)
@Notifiable(types = AppNotificationEventType.COMPANY_ASSIGNED, modelName = CrmModelType.COMPANY)
@Searchable(index = SearchIndex.COMPANY)
@EmailAlertTemplate(templates = {EmailNotificationTemplate.COMPANY_ASSIGNED, EmailNotificationTemplate.COMPANY_TRANSFERRED})
@FieldsValidator(groups = {Group.Create.class, Update.class}, message = "i18n.entity.fields.validation.failed")
@CompanyValidator(groups = {Group.Create.class, Update.class}, message = "i18n.company.validation.failed")
@ZapTrigger(source = Source.COMPANY)
@AutoAssignment(source = Source.COMPANY)
public class Company extends Entity {

    public static final String ENTITY_TYPE = "Company";
    private final DisplayPictureManager displayPictureManager;

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @AuditEntityName
    @AuditField(aliasName = "name", displayName = "i18n.company.fields.name")
    @SearchField(name = "name")
    @Field(aliasName = "name")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.company.name.length.exceeded")
    private String name;

    @URL(groups = {Create.class, Update.class})
    @SearchField(name = "website")
    @Field(aliasName = "website")
    private String website;

    @URL(groups = {Create.class, Update.class})
    @Field(aliasName = "linkedIn")
    private String linkedIn;

    @SearchField(name = "address")
    @Field(aliasName = "address")
    private Address address;

    @Field(aliasName = "industryType")
    private Option industryType;

    @Field(aliasName = "businessType")
    private Option businessType;

    @Field(aliasName = "organizationStatus")
    private Option organizationStatus;

    @URL(groups = {Create.class, Update.class})
    @Field(aliasName = "facebook")
    private String facebook;

    @Field(aliasName = "parentCompany", autoValidate = false)
    private Company parentCompany;

    @Field(aliasName = "sicCode")
    @Max(value = 9999, groups = {Create.class, Update.class}, message = "i18n.company.sicCode.length.exceeded")
    private Long sicCode;

    @URL(groups = {Create.class, Update.class})
    @Field(aliasName = "twitter")
    private String twitter;

    @Field(aliasName = "employeeCount")
    @Max(value = 9999999999999999L, groups = {Create.class, Update.class}, message = "i18n.company.employeeCount.length.exceeded")
    private Long employeeCount;

    @Field(aliasName = "annualRevenue")
    private Monetary annualRevenue;

    private Long activityCount;

    private Long dealCount;

    private Long contactCount;

    @SearchField(name = "territory")
    private Territory territory;

    @Valid
    private transient SummaryInfo summaryInfo;

    private DisplayPicture thumbnail;
    private DisplayPicture originalPicture;

    @Field(aliasName = "description")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.company.description.length.exceeded")
    private String description;

    private boolean notifiable;

    private String recentNote;

    public Long getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(final Long employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Option getIndustryType() {
        return industryType;
    }

    public void setIndustryType(final Option industryType) {
        this.industryType = industryType;
    }

    public Option getBusinessType() {
        return businessType;
    }

    public void setBusinessType(final Option businessType) {
        this.businessType = businessType;
    }

    public Option getOrganizationStatus() {
        return organizationStatus;
    }

    public void setOrganizationStatus(final Option organizationStatus) {
        this.organizationStatus = organizationStatus;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public String getLinkedIn() {
        return linkedIn;
    }

    public void setLinkedIn(final String linkedIn) {
        this.linkedIn = linkedIn;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(final String facebook) {
        this.facebook = facebook;
    }

    public Company getParentCompany() {
        return parentCompany;
    }

    public void setParentCompany(final Company parentCompany) {
        this.parentCompany = parentCompany;
    }

    public Long getSicCode() {
        return sicCode;
    }

    public void setSicCode(final Long sicCode) {
        this.sicCode = sicCode;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(final String twitter) {
        this.twitter = twitter;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(final String website) {
        this.website = website;
    }

    public SummaryInfo getSummaryInfo() {
        return summaryInfo;
    }

    public void setSummaryInfo(final SummaryInfo summaryInfo) {
        this.summaryInfo = summaryInfo;
    }

    public Company() {
        super(ENTITY_TYPE);
        displayPictureManager = DisplayPictureManager.getInstance();
    }

    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.COMPANY_ASSIGNED, EmailNotificationTemplate.COMPANY_TRANSFERRED},
            placeholderTemplates = {EmailNotificationTemplate.COMPANY_ASSIGNED, EmailNotificationTemplate.COMPANY_TRANSFERRED},
            requirePreviousValue = true
    )
    @SearchField(name = "ownerId")
    private Long ownerId;

    @Override
    public Long id() {
        return this.id;
    }

    public Monetary getAnnualRevenue() {
        return annualRevenue;
    }

    public void setAnnualRevenue(final Monetary annualRevenue) {
        this.annualRevenue = annualRevenue;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Long getActivityCount() {
        return activityCount;
    }

    public void setActivityCount(final Long activityCount) {
        this.activityCount = activityCount;
    }

    public Long getDealCount() {
        return dealCount;
    }

    public void setDealCount(final Long dealCount) {
        this.dealCount = dealCount;
    }

    public Long getContactCount() {
        return contactCount;
    }

    public void setContactCount(final Long contactCount) {
        this.contactCount = contactCount;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
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

    public DisplayPicture getThumbnail() {
        final Long orgId = orgId();

        if (Objects.isNull(thumbnail) && Objects.nonNull(orgId) && Objects.nonNull(id)) {
            final Optional<DisplayPicture> displayPicture = displayPictureManager.getDisplayPicture(orgId, id,
                    Source.COMPANY, ProfilePicture.Quality.THUMBNAIL);

            this.thumbnail = displayPicture.orElse(null);
        }

        return thumbnail;
    }

    public void setOriginalPicture(final DisplayPicture originalPicture) {
        this.originalPicture = originalPicture;
    }

    public DisplayPicture getOriginalPicture() {
        return originalPicture;
    }
}