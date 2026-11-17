package com.tlc.crm.webform.api.model;

import com.tlc.commons.template.model.TemplateModel;
import com.tlc.crm.entity.currency.api.model.CurrencyData;
import com.tlc.crm.entity.models.type.Option;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Represents the web form render details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class WebFormTemplateRequest implements TemplateModel {

    private WebForm webForm;
    private String formSubmitUrl;
    private Map<Long, Collection<Option>> optionMap;
    private Collection<CurrencyData> currencyDataList;
    private boolean isIframe;

    public WebForm getWebForm() {
        return webForm;
    }

    public void setWebForm(final WebForm webForm) {
        this.webForm = webForm;
    }

    public Map<Long, Collection<Option>> getOptionMap() {
        return optionMap;
    }

    public void setOptionMap(final Map<Long, Collection<Option>> optionMap) {
        this.optionMap = optionMap;
    }

    public String getFormSubmitUrl() {
        return formSubmitUrl;
    }

    public void setFormSubmitUrl(final String formSubmitUrl) {
        this.formSubmitUrl = formSubmitUrl;
    }

    public Collection<CurrencyData> getCurrencyDataList() {
        return currencyDataList;
    }

    public void setCurrencyDataList(final Collection<CurrencyData> currencyDataList) {
        this.currencyDataList = currencyDataList;
    }

    public boolean isIframe() {
        return isIframe;
    }

    public void setIframe(final boolean isIframe) {
        this.isIframe = isIframe;
    }
}
