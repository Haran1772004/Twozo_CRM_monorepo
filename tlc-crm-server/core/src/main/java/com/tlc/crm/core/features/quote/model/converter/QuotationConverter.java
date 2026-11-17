package com.tlc.crm.core.features.quote.model.converter;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.converter.ModelConverter;
import com.tlc.commons.converter.model.ConverterData;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.model.QuotationTemplate;
import com.tlc.crm.core.internal.quote.status.QuotationErrorCodes;

import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;

import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.l10n.temporal.service.TemporalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * Converter class responsible for bidirectional data transformation between
 * {@link Quotation} domain models and {@link JsonObject} representations.
 * This class handles the conversion logic needed for API serialization/deserialization
 * and ensures proper mapping of all quotation fields including nested entities.
 * </p>
 *
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Convert JSON request data to Quotation domain models</li>
 *   <li>Convert Quotation domain models to JSON response data</li>
 *   <li>Handle complex field mappings (monetary values, timestamps, etc.)</li>
 *   <li>Resolve relationships with users, contacts, deals, and companies</li>
 *   <li>Manage null safety and data validation during conversion</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see Quotation
 * @see QuotationFields
 * @see ModelConverter
 */
public class QuotationConverter extends CrmModelConverter<Quotation, JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotationConverter.class);
    private static final String DOT_REGEX = "\\.";
    private static final String PRIMARY_CONTACT_ACCESS_REQUIRED_ERROR_MESSAGE = "i18n.quotation.contact.access.required.message";

    private final QuotationManager quotationManager;
    private final DealManager dealManager;
    private final RandomIdGenerator randomIdGenerator;
    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;

    private QuotationConverter() {
        this.quotationManager = QuotationManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentConverter = AttachmentConverter.getInstance();
    }

    private static final class InstanceHolder {
        private static final QuotationConverter QUOTATION_CONVERTER = new QuotationConverter();
    }

    public static QuotationConverter getInstance() {
        return InstanceHolder.QUOTATION_CONVERTER;
    }

    /**
     * Converts JSON input data to a Quotation domain model.
     *
     * @param converterData container holding the JSON data to convert along with
     *                      contextual information (orgId, userId, locale, etc.)
     * @return a fully populated Quotation domain model
     */
    @Override
    public Quotation convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject data = converterData.dataToBeConverted();
        final long orgId = converterData.orgId();
        final CrmUser crmUser = converterData.getUser();
        final TemporalService temporalService = TemporalServiceProvider.get();

        if (converterData.isUpdateAction()) {
            final long quotationId = data.getLong(QuotationFields.ID);
            final Quotation quotation = quotationManager.get(orgId, quotationId);

            quotation.setStatus(Quotation.Status.NOT_PUBLISHED);
            quotation.setUpdatedBy(crmUser);
            quotation.setId(quotationId);

            if (data.containsKey(QuotationFields.NAME)) {
                quotation.setName(data.getString(QuotationFields.NAME));
            }

            if (data.containsKey(QuotationFields.QUOTE_JSON)) {
                final JsonObject quoteJson = data.getJsonObject(QuotationFields.QUOTE_JSON);
                final Monetary amount = quotation.getAmount();

                loadCustomSectionId(quoteJson);
                quotation.setQuoteJson(quoteJson);

                if (Objects.nonNull(amount)) {
                    final JsonObject settingsJson = quoteJson.getJsonObject(QuotationFields.SETTINGS);

                    amount.setValue(getAmountFromQuoteJson(quoteJson));
                    Currency.getCurrencyByCode(settingsJson.getString(QuotationFields.CURRENCY)).ifPresent(amount::setCurrency);
                }

                final Long expirationDateValue = getExpirationDateFromQuoteJson(quoteJson);

                if (Objects.nonNull(expirationDateValue)) {
                    quotation.setExpirationTime(new DateTime(expirationDateValue));
                }

                loadCompanyAndContactsIntoQuotation(quoteJson, quotation);
            }
            return quotation;
        } else {
            final Optional<QuotationTemplate> optionalDefaultTemplate = quotationManager.getDefaultTemplate(orgId);
            final QuotationTemplate quotationTemplate;

            if (optionalDefaultTemplate.isEmpty()) {
                final Collection<QuotationTemplate> defaultTemplates = quotationManager.getDefaultTemplates();
                final Supplier<ErrorCode> errorCodeProvider = () -> ErrorCode.get(QuotationErrorCodes.QUOTATION_TEMPLATE_NOT_FOUND,
                        "No default quotation template found in the system for organization " + orgId);

                if (defaultTemplates.isEmpty()) {
                    LOGGER.warn("No default quotation template found in the system for organization {}", orgId);
                    throw errorCodeProvider.get();
                } else {
                    final Collection<Long> templateIds = defaultTemplates.stream().map(QuotationTemplate::id)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    if (templateIds.isEmpty()) {
                        throw errorCodeProvider.get();
                    }
                    quotationManager.createDefaultTemplateMapping(orgId, crmUser.id(), templateIds);
                    quotationTemplate = quotationManager.getDefaultTemplate(orgId).orElseThrow(errorCodeProvider);
                }
            } else {
                quotationTemplate = optionalDefaultTemplate.get();
            }
            final Quotation quotation = new Quotation();

            quotation.setOrgId(orgId);
            quotation.setTemplateId(data.optLong(QuotationFields.TEMPLATE_ID, quotationTemplate.id()));
            quotation.setStatus(Quotation.Status.DRAFT);
            quotation.setOwner(crmUser);
            quotation.setCreatedBy(crmUser);
            quotation.setExpirationTime(new DateTime(temporalService.getUtcDateTime()));
            quotation.setUuid(randomIdGenerator.generateUniqueASCII(NumericalConstant.FIFTY));
            quotation.setNumber(quotationManager.generateQuotationNumber(orgId));

            if (data.containsKey(QuotationFields.DEAL_ID)) {
                final Deal deal = dealManager.getDealWithBasicDetails(orgId, data.getLong(QuotationFields.DEAL_ID));
                final Collection<Contact> contacts = new ArrayList<>();
                final DealValue dealValue = deal.getDealValue();
                final Contact primaryContact = deal.getPrimaryContact();

                if (Objects.isNull(primaryContact)) {
                    throw ErrorCode.get(QuotationErrorCodes.PRIMARY_CONTACT_REQUIRED, PRIMARY_CONTACT_ACCESS_REQUIRED_ERROR_MESSAGE);
                }
                contacts.add(primaryContact);
                deal.getParticipants().forEach(participant -> contacts.add(participant.getContact()));

                dealValue.setId(null);
                quotation.setName(deal.getTitle());
                quotation.setAmount(dealValue);
                quotation.setDeal(deal);
                quotation.setCompany(deal.getCompany());
                quotation.setContacts(contacts);
                quotation.setSchemaJson(quotationTemplate.getSchemaJson());

                return quotation;
            } else {
                throw ErrorCode.get(QuotationErrorCodes.DEAL_ASSOCIATION_REQUIRED, "Deal association is required to create a quotation.");
            }
        }
    }

    /**
     * Extracts and loads company and contact information from the quotation JSON into the
     * given {@link Quotation} domain model.
     *
     * <p>
     * This method iterates over the {@code formAttributes} array within the provided
     * {@code quoteJson} and looks for an attribute of type {@code PARTIES}. Within that
     * section, it processes the {@code buyerDetails} array to identify:
     * <ul>
     *   <li><b>Buyer Company</b> – If a detail of type {@code BUYER_COMPANY} is found and
     *       contains a valid company ID, a {@link Company} instance is created and set on
     *       the quotation.</li>
     *   <li><b>Buyer Contacts</b> – If a detail of type {@code BUYER_CONTACTS} is found,
     *       all valid contact IDs from the {@code values} array are mapped to {@link Contact}
     *       instances and set on the quotation.</li>
     * </ul>
     * </p>
     *
     * @param quoteJson  the JSON object representing the full quotation data, expected to
     *                   contain a {@code formAttributes} array with a {@code PARTIES} section.
     * @param quotation  the {@link Quotation} domain model to be populated with company
     *                   and contact data extracted from the JSON.
     */
    private void loadCompanyAndContactsIntoQuotation(final JsonObject quoteJson, final Quotation quotation) {
        quoteJson.getJsonArray(QuotationFields.FORM_ATTRIBUTES).forEach(formAttribute -> {
            if (formAttribute instanceof JsonObject formAttributeJson) {
                final String attributeType = formAttributeJson.getString(QuotationFields.TYPE);

                if (Objects.equals(attributeType, QuotationFields.PARTIES)) {
                    final JsonArray buyerDetails = formAttributeJson.getJsonArray(QuotationFields.BUYER_DETAILS);

                    buyerDetails.forEach(buyerDetail -> {
                        if (buyerDetail instanceof JsonObject buyerDetailJson) {
                            final String detailType = buyerDetailJson.getString(QuotationFields.TYPE);

                            if (Objects.equals(detailType, QuotationFields.BUYER_COMPANY) && buyerDetailJson.containsKey(QuotationFields.COMPANY)) {
                                final JsonObject companyJson = buyerDetailJson.getJsonObject(QuotationFields.COMPANY);

                                if (Objects.nonNull(companyJson) && companyJson.containsKey(QuotationFields.ID)) {
                                    final Long companyId = companyJson.getLong(QuotationFields.ID);
                                    final Company company = new Company();

                                    company.setId(companyId);
                                    quotation.setCompany(company);
                                } else {
                                    quotation.setCompany(null);
                                }
                            } else if (Objects.equals(detailType, QuotationFields.BUYER_CONTACTS)) {
                                final JsonArray contactJsonArray = buyerDetailJson.getJsonArray(QuotationFields.VALUES);
                                final Collection<Contact> contacts = new ArrayList<>();

                                contactJsonArray.forEach(contactObj -> {
                                    if (contactObj instanceof JsonObject contactJson && contactJson.containsKey(QuotationFields.VALUE)) {
                                        final Long contactId = contactJson.getLong(QuotationFields.VALUE);
                                        final Contact contact = new Contact();

                                        contact.setId(contactId);
                                        contacts.add(contact);
                                    }
                                });

                                if (!contacts.isEmpty()) {
                                    quotation.setContacts(contacts);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Converts a domain model instance wrapped inside a {@link ConverterData} container into its JSON representation.
     * Converts a Quotation domain model to JSON representation for API responses.
     *
     * @param converterData the container encapsulating the domain model instance along with contextual metadata
     * @return a JSON representation of type {@code J} corresponding to the domain model
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<Quotation> converterData) {
        final Quotation quotation = converterData.dataToBeConverted();
        final JsonObject json = Json.object();
        final CrmUser owner = quotation.getOwner();
        final Quotation.Status status = quotation.getStatus();
        final Long templateId = quotation.getTemplateId();
        final JsonObject quoteJson = quotation.getQuoteJson();
        final Monetary amount = quotation.getAmount();
        final DateTime expirationTime = quotation.getExpirationTime();
        final DateTime createdAt = quotation.getCreatedAt();
        final DateTime updatedAt = quotation.getUpdatedAt();
        final CrmUser createdBy = quotation.getCreatedBy();
        final Deal deal = quotation.getDeal();
        final Company company = quotation.getCompany();
        final Collection<Contact> contacts = quotation.getContacts();
        final Contact primaryContact = quotation.getDeal().getPrimaryContact();
        final Long pdfFileId = quotation.getPdfFileId();

        json.put(QuotationFields.ID, quotation.id());
        json.put(QuotationFields.NAME, quotation.getName());
        json.put(QuotationFields.NUMBER, quotation.getFullyQualifiedNumber());
        json.put(QuotationFields.UUID, quotation.getUuid());

        if (Objects.nonNull(pdfFileId)) {
            json.put(QuotationFields.PDF_FILE, getAttachmentJson(pdfFileId));
        }
        json.put(QuotationFields.STATUS, Objects.isNull(status) ? null : status.getId());

        if (Objects.nonNull(templateId)) {
            json.put(QuotationFields.TEMPLATE_ID, templateId);
        }

        if (Objects.nonNull(quoteJson)) {
            json.put(QuotationFields.QUOTE_JSON, quoteJson);
        }

        if (Objects.nonNull(amount)) {
            json.put(QuotationFields.AMOUNT, StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, amount.getSymbol(), amount.getValue().toString()));
        }

        if (Objects.nonNull(expirationTime)) {
            json.put(QuotationFields.EXPIRATION_TIME, expirationTime.getTimeStamp());
        }

        if (Objects.nonNull(createdAt)) {
            json.put(QuotationFields.CREATED_AT, createdAt.getTimeStamp());
        }

        if (Objects.nonNull(updatedAt)) {
            json.put(QuotationFields.UPDATED_AT, updatedAt.getTimeStamp());
        }

        if (Objects.nonNull(owner)) {
            json.put(QuotationFields.OWNER, Json.object()
                    .put(QuotationFields.ID, owner.id())
                    .put(QuotationFields.NAME, owner.getFullName()));
        }

        if (Objects.nonNull(createdBy)) {
            json.put(QuotationFields.CREATED_BY, Json.object()
                    .put(QuotationFields.ID, createdBy.id())
                    .put(QuotationFields.NAME, createdBy.getFullName()));
        }

        if (Objects.nonNull(deal)) {
            json.put(QuotationFields.DEAL_ID, deal.id());
        }
        final int actualContactsCount = quotation.getActualContactsCount();

        if (Objects.isNull(contacts) || contacts.isEmpty()) {
            if (actualContactsCount > 0) {
                json.put(QuotationFields.HAS_NO_ENTITY_ACCESS, true);
            }
        } else {
            final JsonArray contactsArray = Json.array();

            contacts.forEach(contact -> contactsArray.put(Json.object().put(QuotationFields.ID, contact.id())));
            json.put(QuotationFields.CONTACTS, contactsArray);

            if (actualContactsCount > contacts.size()) {
                json.put(QuotationFields.HAS_NO_ENTITY_ACCESS, true);
            }
        }

        if (Objects.isNull(company)) {
            if (quotation.isCompanyPresent()) {
                json.put(QuotationFields.HAS_NO_ENTITY_ACCESS, true);
            }
        } else {
            json.put(QuotationFields.COMPANY_ID, company.id());
        }

        if (Objects.nonNull(primaryContact)) {
            final Email email = primaryContact.getPrimaryEmail();

            if (Objects.nonNull(email)) {
                final JsonObject contactJson = Json.object();
                final JsonObject emailJson = Json.object();

                contactJson.put(QuotationFields.ID, primaryContact.id());
                contactJson.put(QuotationFields.NAME, primaryContact.getFullName());
                emailJson.put(QuotationFields.ID, email.id());
                emailJson.put(QuotationFields.VALUE, email.getValue());
                contactJson.put(QuotationFields.EMAIL, emailJson);

                json.put(QuotationFields.PRIMARY_CONTACT, contactJson);
            }
        }

        return json;
    }

    /**
     * <p>
     * Retrieves the JSON representation of an attachment by its ID.
     * </p>
     *
     * @param attachmentId Represents the unique identifier of the attachment to be retrieved.
     * @return the JSON representation of the attachment if found; otherwise, an empty JSON object.
     */
    private JsonObject getAttachmentJson(final Long attachmentId) {
        return attachmentService.getAttachments(List.of(attachmentId)).stream().findFirst().map(attachmentConverter::convert)
                .orElseGet(Json::object);
    }

    /**
     * Extracts the total monetary value from the quotation JSON.
     *
     * Parses the first valid value from SUMMARY → TOTAL_VALUE and sets currency
     * and orgId in the returned Monetary object.
     *
     * @param quoteJson the quotation JSON
     * @return populated Monetary object
     */
    private BigDecimal getAmountFromQuoteJson(final JsonObject quoteJson) {
        final JsonArray formAttributes = quoteJson.getJsonArray(QuotationFields.FORM_ATTRIBUTES);
        final int formAttributeSize = formAttributes.size();

        formAttributesLoop:
        for (int formAttributeIndex = 0; formAttributeIndex < formAttributeSize; formAttributeIndex++) {
            final JsonObject formAttribute = formAttributes.getJsonObject(formAttributeIndex);
            final String attributeType = formAttribute.getString(QuotationFields.TYPE);

            if (Objects.equals(attributeType, QuotationFields.SUMMARY)) {
                final JsonArray summaryDetails = formAttribute.getJsonArray(QuotationFields.SUMMARY_DETAILS);
                final int summaryDetailSize = summaryDetails.size();

                for (int summaryDetailIndex = 0; summaryDetailIndex < summaryDetailSize; summaryDetailIndex++) {
                    final JsonObject summaryDetailsJson = summaryDetails.getJsonObject(summaryDetailIndex);
                    final String summaryAttributeType = summaryDetailsJson.getString(QuotationFields.TYPE);

                    if (Objects.equals(summaryAttributeType, QuotationFields.TOTAL_VALUE)) {
                        final JsonArray values = summaryDetailsJson.getJsonArray(QuotationFields.VALUES);
                        final int valuesSize = values.size();

                        for (int valuesIndex = 0; valuesIndex < valuesSize; valuesIndex++) {
                            final JsonObject valueJson = values.getJsonObject(valuesIndex);
                            final String monetaryString = valueJson.optString(QuotationFields.VALUE, null);

                            if (Objects.nonNull(monetaryString) && !monetaryString.isBlank()) {
                                final String numeric = monetaryString.replaceAll("[^0-9.\\-]", StringUtil.Delimiter.EMPTY_STRING.getDelimiter());

                                try {
                                    return new BigDecimal(numeric);
                                } catch (final NumberFormatException nfe) {
                                    LOGGER.warn("Unable to parse monetary value from '{}'", monetaryString, nfe);
                                }
                                break formAttributesLoop;
                            }
                        }
                    }
                }
            }
        }
        return BigDecimal.valueOf(0);
    }

    /**
     * Extracts the expiration date from the quotation JSON.
     *
     * Looks at the HEADER section and finds the field whose value has a
     * 'minDate' key — this is what identifies it as the "Expiration date"
     * field, since "Issue date" has the same type but no 'minDate'.
     *
     * @param quoteJson the quotation JSON
     * @return the expiration date timestamp, or null if not found
     */
    private Long getExpirationDateFromQuoteJson(final JsonObject quoteJson) {
        final JsonArray formAttributes = quoteJson.getJsonArray(QuotationFields.FORM_ATTRIBUTES);
        final int formAttributeSize = formAttributes.size();

        for (int index = 0; index < formAttributeSize; index++) {
            final JsonObject formAttribute = formAttributes.getJsonObject(index);

            if (Objects.equals(formAttribute.getString(QuotationFields.TYPE), QuotationFields.HEADER)) {
                final JsonArray details = formAttribute.getJsonArray(QuotationFields.DETAILS);
                final int detailSize = details.size();

                for (int detailIndex = 0; detailIndex < detailSize; detailIndex++) {
                    final JsonArray values = details.getJsonObject(detailIndex).getJsonArray(QuotationFields.VALUES);

                    if (Objects.nonNull(values) && !(values.size() == 0)) {
                        final JsonObject valueJson = values.getJsonObject(0);

                        // TODO: Handle this using the section ID.
                        if (valueJson.containsKey(QuotationFields.MIN_DATE)) {
                            final long expirationDateValue = valueJson.optLong(QuotationFields.VALUE, -1);

                            if (expirationDateValue > 0) {
                                return expirationDateValue;
                            }
                        }
                    }
                }
                break;
            }
        }
        return null;
    }

    /**
     * Iterates through the form attributes of a quotation and assigns sequential IDs
     * to any custom sections that are missing them.
     * <p>
     * The method extracts the current prefix and counter from the {@code NEXT_CUSTOM_SECTION_ID}
     * field. It applies new IDs using a dot-notation format (e.g., "prefix.counter") to
     * newly identified custom sections, and finally updates the quotation JSON with
     * the next available ID sequence.
     *
     * @param quoteJson The JSON object representing the quotation. This object is mutated
     * in-place to include missing section IDs and to update the {@code NEXT_CUSTOM_SECTION_ID} tracker.
     */
    private void loadCustomSectionId(final JsonObject quoteJson) {
        final JsonArray formAttributes = quoteJson.getJsonArray(QuotationFields.FORM_ATTRIBUTES);
        final String[] customSectionIdWithPrefixId = quoteJson.getString(QuotationFields.NEXT_CUSTOM_SECTION_ID).split(DOT_REGEX);
        final String prefixId = customSectionIdWithPrefixId[0];
        final Counter customSectionIdCounter = new Counter(NumberUtils.parseNumber(customSectionIdWithPrefixId[1]).intValue());

        if (Objects.nonNull(formAttributes)) {
            final int formAttributeCount = formAttributes.size();

            for (int formAttributeIndex = 0; formAttributeIndex < formAttributeCount; formAttributeIndex++) {
                final JsonObject formAttribute = formAttributes.getJsonObject(formAttributeIndex);
                final String attributeType = formAttribute.getString(QuotationFields.TYPE);

                if (Objects.equals(attributeType, QuotationFields.CUSTOM_SECTION) && !formAttribute.containsKey(QuotationFields.SECTION_ID)) {
                    formAttribute.put(QuotationFields.SECTION_ID,
                            StringUtil.join(StringUtil.Delimiter.DOT, prefixId, String.valueOf(customSectionIdCounter.getAndIncrement())));
                }
            }

            quoteJson.put(QuotationFields.NEXT_CUSTOM_SECTION_ID,
                    StringUtil.join(StringUtil.Delimiter.DOT, prefixId, String.valueOf(customSectionIdCounter.getCount())));
        }
    }
}
