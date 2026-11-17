package com.tlc.crm.entity.filter.api.models;

/**
 * <p>
 * A model that represents an additional option data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AdditionalOption {

    private Long id;
    private Long optionEntityId;
    private String optionName;
    private AdditionalOption additionalOption;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(final String optionName) {
        this.optionName = optionName;
    }

    public Long getOptionEntityId() {
        return optionEntityId;
    }

    public void setOptionEntityId(final Long optionEntityId) {
        this.optionEntityId = optionEntityId;
    }

    public AdditionalOption getAdditionalOption() {
        return additionalOption;
    }

    public void setAdditionalOption(final AdditionalOption additionalOption) {
        this.additionalOption = additionalOption;
    }
}
