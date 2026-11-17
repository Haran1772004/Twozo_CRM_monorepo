package com.tlc.crm.core.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.models.type.Monetary;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Manages {@link Deal} related dropdown actions.
 *
 * @author karthickv
 * @version 1.0
 */
@Dropdown(name = "dealList", fieldAlias = "deal")
public class DealList extends AbstractDropdownAction {

    private static final String DISABLE_REASON = "i18n.quotation.entity.access.required";

    private final DealManager dealManager;
    private final BaseCurrencyService baseCurrencyService;
    private final CompanyConverter companyConverter;
    private final ContactConverter contactConverter;

    public DealList() {
        this.dealManager = DealManager.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.companyConverter = CompanyConverter.getInstance();
        this.contactConverter = ContactConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @return {@link OptionContainer} that holds the requested dropdown values
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final JsonObject data = dropdownRequest.data();
        final boolean skipArchived = data.optBoolean(DealFields.SKIP_ARCHIVED);
        final String sourceKey = FieldKeys.SOURCE.getName();
        final String entityIdKey = FieldKeys.ENTITY_ID.getName();
        final Long entityId = data.containsKey(entityIdKey) ? data.getLong(entityIdKey) : null;
        final Source source = data.containsKey(sourceKey) ? Source.get(data.getString(sourceKey)) : null;
        final long totalRecords = dealManager.getTotalCount(orgId, entityId, source, dropdownCriteria, skipArchived);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Deal> deals = dealManager.getDeals(orgId, entityId, source, dropdownLimit, dropdownCriteria, skipArchived);
            final CrmRequest.ClientType clientType = dropdownRequest.getClientType();

            if (CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType) {
                deals.forEach(deal -> {
                    final JsonObject dealJson = Json.object();

                    dealJson.put(OptionKeys.ID, deal.id());
                    dealJson.put(OptionKeys.NAME, deal.getTitle());

                    optionContainer.addOption(dealJson);
                });
            } else {
                final Locale locale = dropdownRequest.locale();

                deals.forEach(deal -> optionContainer.addOption(convertDealIntoJson(orgId, locale, deal)));
            }
        }

        return optionContainer;
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @return {@link OptionContainer} that holds the requested dropdown values
     */
    @Override
    public OptionContainer fetchMobileData(final DropdownRequest dropdownRequest) {
        final long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final boolean skipArchived = dropdownRequest.data().optBoolean(DealFields.SKIP_ARCHIVED);
        final long totalRecords = dealManager.getTotalCount(orgId, dropdownCriteria, skipArchived);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Deal> deals = dealManager.fetchDeals(orgId, dropdownLimit, dropdownCriteria, skipArchived);
            final Locale locale = dropdownRequest.locale();

            deals.forEach(deal -> optionContainer.addOption(convertDealIntoJson(orgId, locale, deal)));
        }

        return optionContainer;
    }

    /**
     * <p>
     * Convert the {@link Deal} into {@link JsonObject}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param locale Represents the {@link Locale}
     * @param deal   Represents the {@link Deal}
     * @return {@link JsonObject}
     */
    private JsonObject convertDealIntoJson(final long orgId, final Locale locale, final Deal deal) {
        final JsonObject dealJson = Json.object();
        final Monetary monetary = deal.getDealValue();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Contact contact = deal.getPrimaryContact();
        final Company company = deal.getCompany();
        final BigDecimal amount;

        if (Objects.isNull(monetary)) {
            amount = BigDecimal.ZERO;
        } else {
            amount = monetary.getValue();
            final Currency currency = monetary.getCurrency();

            if (Objects.nonNull(currency)) {
                dealJson.put(DealFields.CURRENCY, currency.getCode());
                dealJson.put(DealFields.SYMBOL, currency.getSymbol());
            }
        }

        if (Objects.nonNull(pipelineStage)) {
            dealJson.put(DealFields.PIPELINE_STAGE, pipelineStage.getName());
        }

        if (Objects.nonNull(company)) {
            dealJson.put(DealFields.COMPANY, companyConverter.convertPartialData(company));
        }

        if (Objects.isNull(contact)) {
            final I18nResolver i18nResolver = I18nServiceProvider.get().getResolver(locale);

            dealJson.put(DealFields.IS_DISABLED, true);
            dealJson.put(DealFields.DISABLE_REASON, i18nResolver.get(DISABLE_REASON));
        } else {
            dealJson.put(DealFields.CONTACT, contactConverter.convertPartialData(contact));
        }

        dealJson.put(DealFields.DEAL_VALUE, baseCurrencyService.applyPrecision(orgId, amount));
        dealJson.put(DealFields.NAME, deal.getTitle());
        dealJson.put(DealFields.VALUE, deal.id());

        return dealJson;
    }
}
