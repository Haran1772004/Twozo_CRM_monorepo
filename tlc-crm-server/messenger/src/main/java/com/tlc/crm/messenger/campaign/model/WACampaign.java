package com.tlc.crm.messenger.campaign.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.campaign.validator.annotation.WACampaignValidator;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents the WhatsApp-specific campaign extension.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@WACampaignValidator(groups = {Group.Create.class, Group.Update.class})
public class WACampaign extends Campaign {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.invalid.wa.phone.number")
    private CrmWAPhoneNumber businessPhoneNumber;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.campaign.invalid.template")
    private WATemplate template;

    private String templateConfig;
    private JsonObject templateConfigJson;


    public WACampaign() {
        super(CampaignCategory.WHATSAPP);
    }

    public CrmWAPhoneNumber getBusinessPhoneNumber() {
        return businessPhoneNumber;
    }

    public void setBusinessPhoneNumber(final CrmWAPhoneNumber businessPhoneNumber) {
        this.businessPhoneNumber = businessPhoneNumber;
    }

    public WATemplate getTemplate() {
        return template;
    }

    public void setTemplate(final WATemplate template) {
        this.template = template;
    }

    public String getTemplateConfig() {
        return templateConfig;
    }

    public void setTemplateConfig(final String templateConfig) {
        this.templateConfig = templateConfig;
        this.templateConfigJson = Objects.nonNull(templateConfig) && !templateConfig.isBlank() ? Json.object(templateConfig) : null;
    }

    public JsonObject getTemplateConfigJson() {
        return templateConfigJson;
    }

    public void setTemplateConfigJson(final JsonObject templateConfigJson) {
        this.templateConfigJson = templateConfigJson;
        this.templateConfig = Objects.nonNull(templateConfigJson) ? templateConfigJson.toString() : null;
    }

    public Long getBusinessPhoneNumberId() {
        return Objects.nonNull(businessPhoneNumber) ? businessPhoneNumber.getCrmPhoneNumberId() : null;
    }

    public Long getTemplateId() {
        return Objects.nonNull(template) ? template.id() : null;
    }
}