package com.tlc.crm.core.features.quote.fields;

/**
 * <p>
 * Constants class that defines all field names used in quotation-related JSON objects, 
 * API requests, and database operations. This class serves as a centralized registry 
 * of field identifiers to ensure consistency across the quotation management system.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see com.tlc.crm.core.features.quote.model.Quotation
 * @see com.tlc.crm.core.features.quote.model.converter.QuotationConverter
 * @see com.tlc.crm.core.features.quote.action.QuotationManagement
 */
public class QuotationFields {

	public static final String TYPE = "type";
	public static final String DATA = "data";
	public static final String ID = "id";
	public static final String NAME = "name";
	public static final String NUMBER = "number";
	public static final String UUID = "uuid";
	public static final String TEMPLATE_ID = "templateId";
	public static final String QUOTE_JSON = "quoteJson";
	public static final String STATUS = "status";
	public static final String OWNER = "owner";
	public static final String OWNER_ID = "ownerId";
	public static final String DEAL_ID = "dealId";
	public static final String CONTACTS = "contacts";
	public static final String CONTACT_IDS = "contactIds";
	public static final String COMPANY_ID = "companyId";
	public static final String AMOUNT = "amount";
	public static final String EXPIRATION_TIME = "expirationTime";
	public static final String CREATED_AT = "createdAt";
	public static final String UPDATED_AT = "updatedAt";
	public static final String CREATED_BY = "createdBy";
	public static final String MESSAGE = "message";
	public static final String QUOTATION = "quotation";
	public static final String QUOTATIONS = "quotations";
	public static final String CREATED_RECORDS = "createdRecords";
	public static final String UPDATED_RECORDS = "updatedRecords";
	public static final String SETTINGS = "settings";
	public static final String QUOTE_URL = "quoteUrl";
	public static final String FORM_ATTRIBUTES = "formAttributes";
	public static final String LINE_ITEMS = "LINE_ITEMS";
	public static final String ITEMS = "items";
	public static final String SUMMARY = "SUMMARY";
	public static final String SUMMARY_DETAILS = "summaryDetails";
	public static final String TOTAL_VALUE = "TOTAL_VALUE";
	public static final String VALUES = "values";
	public static final String CURRENCY = "currency";
	public static final String VALUE = "value";
	public static final String VIEW_QUOTE = "View Quote";
	public static final String LABEL = "label";
	public static final String CODE = "code";
	public static final String FIRST_NAME = "firstName";
	public static final String LAST_NAME = "lastName";
	public static final String EMAIL = "email";
	public static final String PHONE = "phone";
	public static final String CONTACT_ID = "contactId";
	public static final String PRIMARY_CONTACT = "primaryContact";
	public static final String HEADER = "HEADER";
	public static final String DETAILS = "details";
	public static final String MIN_DATE = "minDate";
	public static final String ATTACHMENT_ID = "attachmentId";
	public static final String ACTION = "action";
	public static final String DOWNLOAD = "download";
	public static final String QUOTE_DOWNLOAD_URL = "quoteDownloadUrl";
	public static final String PDF_FILE = "pdfFile";
	public static final String CUSTOM_SECTION = "CUSTOM_SECTION";
	public static final String NEXT_CUSTOM_SECTION_ID = "nextCustomSectionId";
	public static final String SECTION_ID = "sectionId";
    public static final String PARTIES = "PARTIES";
	public static final String BUYER_DETAILS = "buyerDetails";
	public static final String BUYER_COMPANY = "BUYER_COMPANY";
	public static final String COMPANY = "company";
	public static final String BUYER_CONTACTS = "BUYER_CONTACTS";
	public static final String HAS_NO_ENTITY_ACCESS = "hasNoEntityAccess";
	public static final String QUOTE_ID = "quoteId";
	public static final String FIELD = "field";
	public static final String ORG_ID = "orgId";
	public static final String COMPANY_LOGO = "companyLogo";
	public static final String USER_PROFILE_PIC = "userProfilePic";
	public static final String IMAGE_ID = "imageId";
	public static final String USER_ID = "userId";
	public static final String URL = "url";
	public static final String IMAGE_PREVIEW_CONF = "image.preview";

	private QuotationFields() {
	}
}
