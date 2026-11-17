package com.tlc.crm.entity.search.fields;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Holds the entity of common search fields and provides access to those field names
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see FieldNameHolder
 */
public final class CommonSearchFields {

    private CommonSearchFields() {
    }

    public static final String ID = "id";
    public static final String INDEX = "index";
    public static final String ENTRY_ID = "entryId";
    public static final String RECENTLY_VIEWED = "recentlyViewed";
    public static final String RECENTLY_SEARCHED = "recentlySearched";
    public static final String ORDER = "order";
    public static final String RANGE = "range";
    public static final String FROM = "from";
    public static final String COUNT = "count";
    public static final String CONTACT = "contact";
    public static final String CONTACT_IDS = "contactIds";
    public static final String COMPANY = "company";
    public static final String DEAL = "deal";
    public static final String ACTIVITY = "activity";
    public static final String PRODUCT = "product";
    public static final String NOTE = "note";
    public static final String NOTES = "notes";
    public static final String FILE = "file";
    public static final String DATA = "data";
    public static final String TABLE = "table";
    public static final String ENTITY_SOURCE = "entitySource";
    public static final String ENTITY_SOURCE_ID = "entitySourceId";
    public static final String ENTITY_SOURCE_NAME = "entitySourceName";
    public static final String EMAILS = "emails";
    public static final String PHONES = "phones";
    public static final String LIST = "list";
    public static final String TOTAL_RECORDS = "totalRecords";
    public static final String COUNTRY = "country";
    public static final String COUNTRY_CODE = "countryCode";
    public static final String HAS_MORE = "hasMore";
    public static final String SEARCH_TEXT = "searchText";
    public static final String SIZE = "size";
    public static final String RESULTS = "results";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_LINE_1 = "addressLine1";
    public static final String ADDRESS_LINE_2 = "addressLine2";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String ZIPCODE = "zipcode";
    public static final String VALUE = "value";
    public static final String NUMBER = "number";
    public static final String ALL = "all";
    public static final String ASSOCIATED_FIELDS = "associatedFields";
    public static final String ASSOCIATED_FIELD = "associatedField";
    public static final String FIRST_NAME = "firstName";
    public static final String NAME = "name";
    public static final String TITLE = "title";
    public static final String CONTACT_DOT_ID = StringUtil.joinStringsWithDot(CommonSearchFields.CONTACT, CommonSearchFields.ID);
    public static final String COMPANY_DOT_ID = StringUtil.joinStringsWithDot(CommonSearchFields.COMPANY, CommonSearchFields.ID);
    public static final String DEAL_DOT_ID = StringUtil.joinStringsWithDot(CommonSearchFields.DEAL, CommonSearchFields.ID);
    public static final String LAST_NAME = "lastName";
    public static final String DETAILS = "details";
    public static final String MATCHED_RESULTS = "matchedResults";
    public static final String WEBSITE = "website";
    public static final String ACTIVITY_TYPE = "activityType";
    public static final String STATUS = "status";
    public static final String IS_DONE = "isDone";
    public static final String PRODUCT_CODE = "productCode";
    public static final String IS_OVERDUE = "isOverdue";
    public static final String DEAL_STATE = "dealState";
    public static final String OPEN = "open";
    public static final String ORG_ID = "orgId";
    public static final String CUSTOM_FIELDS = "customFields";
    public static final String ENTITY_ID = "entityId";
    public static final String MESSAGE = "message";
    public static final String DISPLAY_NAME = "displayName";
    public static final String OWNER_ID = "ownerId";
    public static final String SALES_OWNER = "salesOwner";
    public static final String TERRITORY = "territory";
    public static final String TERRITORY_ID = "territoryId";
    public static final String TOTAL_PRODUCT_COUNT = "totalProductCount";
    public static final String IS_PERMISSIBLE = "isPermissible";
    public static final String RESTRICTED = "restricted";
    public static final String DEAL_VALUE = "dealValue";
    public static final String END_TIME = "endTime";
    public static final String FACEBOOK = "facebook";
    public static final String LINKEDIN = "linkedin";
    public static final String EXPECTED_CLOSE_DATE = "expectedCloseDate";
    public static final String PIPELINE_STAGE = "pipelineStage";
    public static final String PIPELINE_STAGE_ID = "pipelineStageId";
    public static final String PIPELINE = "pipeline";
    public static final String START_TIME = "startTime";
    public static final String GLOBAL = "global";
    public static final String TERRITORY_IDS = "territoryIds";
    public static final String PRIMARY_EMAIL = "primaryEmail";
    public static final String PRIMARY_PHONE = "primaryPhone";
    public static final String CURRENCY = "currency";
    public static final String FOLLOWER_IDS = "followerIds";
    public static final String DEAL_FOLLOWER = "isDealFollower";
    public static final String CONTACT_FOLLOWER = "isContactFollower";
    public static final String COMPANY_FOLLOWER = "isCompanyFollower";
    public static final String FOLLOWER = "follower";
    public static final String OWNER_IDS = "ownerIds";
    public static final String OWNER = "owner";
    public static final String IS_ARCHIVED = "isArchived";
    public static final String INDEXES = "indexes";
    public static final String PROFILE_URL = "profileUrl";
    public static final String CHECK_IN_INFO = "checkInInfo";
    public static final String CHECK_OUT_INFO = "checkOutInfo";
    public static final String CHECK_IN_TIME = "checkInTime";
    public static final String CHECK_OUT_TIME = "checkOutTime";
    public static final String ACTIVITY_ICON = "activityIcon";
    public static final String REQUIRE_CHECK_IN = "requireCheckIn";
    public static final String REQUIRE_CHECK_OUT = "requireCheckOut";
    public static final String TIME_SPENT = "timeSpent";
    public static final String THUMBNAIL = "thumbnail";
    public static final String URL = "url";
    public static final String ICON = "icon";
    public static final String TYPE = "type";
    public static final String INDEX_COUNT = "indexCount";
    public static final String META_COUNT = "metaCount";
}