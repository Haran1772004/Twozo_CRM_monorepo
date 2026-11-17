package com.tlc.crm.core.mcp.tools;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.mcp.CrmMcpTool;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.DateIntervalService;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;
import com.tlc.mcp.tool.SchemaDefinition.FieldDef;

import com.tlc.sql.common.api.dml.JoinClause;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for entity list MCP tools (Contact, Company, Product).
 *
 * <p>Encapsulates the common logic for fetching entity lists with filter criteria,
 * sorting, and pagination using {@link EntitySQLListInterpreter} — the same
 * SQL-level list interpreter that the {@code EntityListAction} REST endpoints use.</p>
 *
 * <p>Subclasses only need to provide the {@link Provider} and module name;
 * this base handles criteria building via {@link FilterCriteriaBuilder},
 * query composition, and JSON result formatting.</p>
 *
 * <p><b>IMPORTANT for calling LLMs:</b>
 * <ul>
 *   <li>For relative date filters (today, yesterday, this week, this month), call list_* directly
 *       with {@code dateInterval} (optional {@code dateField}, default {@code created_at}).</li>
 *   <li>When using other filter criteria, call {@code get_list_schema(module='...')} once — it includes
 *       field IDs, comparators, value formats, and {@code dateIntervals}.</li>
 *   <li>For dropdown/owner fields, call {@code get_field_options} or {@code search_users} to resolve value IDs.</li>
 * </ul>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public abstract class AbstractEntityListTool extends CrmMcpTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEntityListTool.class);

    /**
     * Reasonable defaults matching typical CRM configurations.
     */
    private static final int DEFAULT_MAX_FIELD_SELECTION_LIMIT = 50;
    private static final int DEFAULT_MAX_FILTER_CRITERIA_LIMIT = 10;
    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_COUNT = 25;

    private static final String DEFAULT_DATE_FIELD = "created_at";

    private final FilterCriteriaBuilder filterCriteriaBuilder;
    private final RolePermissionService rolePermissionService;
    private final FieldService fieldService;
    private final ComparatorService comparatorService;
    private final FieldTypeService fieldTypeService;
    private final DateIntervalService dateIntervalService;
    private final I18nService i18nService;

    protected AbstractEntityListTool() {
        this.filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.fieldService = FieldService.getSecureInstance();
        this.comparatorService = ComparatorService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.dateIntervalService = DateIntervalService.getInstance();
        this.i18nService = I18nServiceProvider.get();
    }

    /**
     * Returns the {@link Provider} for the entity type this tool lists.
     *
     * @return the list provider (e.g. {@code Provider.CONTACT_LIST})
     */
    protected abstract Provider getProvider();

    /**
     * Returns the module name for display and logging purposes.
     *
     * @return module name (e.g. "contact", "company", "product")
     */
    protected abstract String getModuleName();

    /**
     * Fetches data rows for the specified organisation, user, and entity IDs.
     *
     * @param orgId     The unique identifier of the organisation.
     * @param userId    The unique identifier of the user.
     * @param entityIds The collection of entity IDs for which data rows should be fetched.
     * @return A JSON array containing the fetched data rows.
     */
    protected abstract JsonArray fetchDataRows(final long orgId, final long userId, final Collection<Long> entityIds);

    /**
     * Executes the entity list query with optional filter criteria, sorting,
     * and pagination.
     *
     * @param ctx   MCP tool context containing CRM identity
     * @param input query parameters: criteria, sort, start, count, fetchHits
     * @return tool result with entity list data as JSON
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx, final Map<String, Object> input) {
        try {
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final long userId = crmUser.id();
            final Provider provider = getProvider();
            final String moduleName = getModuleName();

            LOGGER.debug("MCP: List_{}: Starting list query. Org: {}, User: {}", moduleName, orgId, userId);
            final JoinLoader joinLoader = createEmptyJoinLoader();
            final EntitySQLListInterpreter interpreter = new EntitySQLListInterpreter(userId, orgId, provider, joinLoader, null, DEFAULT_MAX_FIELD_SELECTION_LIMIT, DEFAULT_MAX_FILTER_CRITERIA_LIMIT);
            final JsonObject requestJson = Json.object(input);

            if (requestJson.containsKey("sort")) {
                final JsonArray sortArray = requestJson.getJsonArray("sort");

                if (0 == sortArray.size()) {
                    interpreter.applyDefaultOrderBy();
                } else {
                    for (int index = 0; index < sortArray.size(); index++) {
                        final JsonObject fieldInfo = sortArray.getJsonObject(index);
                        final String field = fieldInfo.getString(ListActionFields.FIELD);
                        final Object sortType = fieldInfo.get(ListActionFields.TYPE);

                        interpreter.orderByClause(field, sortType == null || !sortType.toString().equalsIgnoreCase(ListActionFields.SORT_DESC));
                    }
                }
            } else {
                interpreter.applyDefaultOrderBy();
            }

            final Collection<?> criteriaList = resolveCriteriaList(requestJson, orgId, provider);

            if (!criteriaList.isEmpty()) {
                applyFilterCriteria(orgId, provider, interpreter, criteriaList);
            }

            final int start = requestJson.optInt("start", DEFAULT_START);
            final int count = requestJson.optInt("count", DEFAULT_COUNT);

            interpreter.setLimits(start, count);

            final boolean fetchHits = requestJson.optBoolean("fetchHits", true);
            long totalRecords = 0;

            if (fetchHits) {
                totalRecords = interpreter.fetchTotalHits();
            }

            final boolean fetchData = requestJson.optBoolean("fetchData", true);
            final JsonObject response = Json.object();

            if (fetchData) {
                final Collection<Long> ids = new LinkedHashSet<>(interpreter.getPkValues());

                response.put("rows", fetchDataRows(orgId, userId, ids));
            }

            if (fetchHits) {
                response.put("totalRecords", totalRecords);
            }

            response.put("module", moduleName);
            response.put("start", start);
            response.put("count", count);
            response.put("hasMore", (start + count) <= totalRecords);

            LOGGER.info("MCP list_{}: Org: {}, User: {}", moduleName, orgId, userId);

            return McpToolResult.json(response);
        } catch (Exception e) {
            LOGGER.error("MCP: List_{}: Error executing list query", getModuleName(), e);
            return errorResult("Failed to list " + getModuleName() + " records: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param generated the reflection-generated base schema
     * @param ctx       the MCP tool context
     * @return
     */
    @Override
    public SchemaDefinition inputSchema(final SchemaDefinition generated, final McpToolContext<CrmMcpIdentity> ctx) {
        try {
            return SchemaDefinition.of(buildInputSchemaFields());
        } catch (Exception e) {
            LOGGER.error("MCP: Dynamic schema generation failed for list_{}: {}", getModuleName(), e.getMessage(), e);
            return generated;
        }
    }

    /**
     * Builds the JSON schema fields for the input parameter.
     */
    private List<FieldDef> buildInputSchemaFields() {
        final List<FieldDef> fields = new ArrayList<>();

        fields.add(FieldDef.builder("dateInterval")
                .description("""
                Relative date interval for filtering. Accepts friendly aliases: today, yesterday, this_week, last_week,
                next_week, this_month, last_month, next_month, this_year, last_year, this_quarter, last_quarter,
                next_quarter, tomorrow, today_or_later, yesterday_or_later, till_now, etc. Call get_list_schema whenever
                additional information is needed to determine the appropriate value or when sort criteria is used.
                Applied to dateField (default: created_at). Can be combined with criteria (AND).
                """)
                .required(false)
                .type("string")
                .build());
        fields.add(FieldDef.builder("dateField")
                .description("Internal field name to apply dateInterval on. Default: created_at.")
                .required(false)
                .type("string")
                .defaultValue(DEFAULT_DATE_FIELD)
                .build());
        fields.add(FieldDef.builder("criteria")
                .description("""
                        Array of filter criteria objects. Each criterion filters the list by a field condition.
                        BEFORE using criteria: call get_list_schema(module) once to discover field IDs, comparators, value formats, and dateIntervals.
                        For dropdown/owner fields, call get_field_options or search_users to resolve value IDs.
                        For date fields, use valueEntityId from dateIntervals in get_list_schema, or use dateInterval shortcut.
                        """)
                .required(false)
                .type("array")
                .itemType("object")
                .nestedFields(List.of(
                        FieldDef.builder("field")
                                .description("Field info filter on. Discover via get_list_schema(module).")
                                .required(true)
                                .type("object").nestedFields(List.of(
                                        FieldDef.builder("fieldId")
                                                .description("Numeric field ID. Discover via get_list_schema(module).")
                                                .required(true)
                                                .type("integer")
                                                .build()
                                ))
                                .build(),
                        FieldDef.builder("comparatorId")
                                .description("""
                                        Comparator ID defining the filter operation.
                                        Discover valid IDs via get_list_schema(module).
                                        """)
                                .required(true)
                                .type("integer")
                                .build(),
                        FieldDef.builder("values")
                                .description("""
                                        Array of filter value objects. Format depends on field type — see valueFormat in get_list_schema response.
                                        For text fields: [{"value": "text"}]. For dropdown/entity fields: [{"value": "name", "valueEntityId": 123}].
                                        Not required when comparator needsValue=false (e.g., is empty).""")
                                .required(false)
                                .type("array")
                                .itemType("object")
                                .nestedFields(List.of(
                                        FieldDef.builder("value")
                                                .description("The filter value (text representation).")
                                                .required(true)
                                                .type("string")
                                                .build(),
                                        FieldDef.builder("valueEntityId")
                                                .description("Numeric entity/option ID for dropdown, owner, contact, company fields, or date interval ID from dateIntervals in get_list_schema.")
                                                .required(false)
                                                .type("integer")
                                                .build()
                                ))
                                .build(),
                        FieldDef.builder("connectorId")
                                .description("Logical connector to the next criterion. 1=AND, 2=OR. Default is AND.")
                                .required(true)
                                .type("integer")
                                .build()
                ))
                .build());
        fields.add(FieldDef.builder("sort")
                .description("Array of sort criteria objects. Each specifies a field and sort direction.")
                .required(false)
                .type("array")
                .itemType("object")
                .nestedFields(List.of(
                        FieldDef.builder("field")
                                .description("Field alias name to sort by. Discover via get_list_schema(module) where the field 'isSortable' is true")
                                .required(true)
                                .type("string")
                                .build(),
                        FieldDef.builder("type")
                                .description("Sort direction: 'asc' for ascending, 'desc' for descending.")
                                .required(false)
                                .type("string")
                                .options(List.of("asc", "desc"))
                                .build()
                ))
                .build());
        fields.add(FieldDef.builder("start")
                .description("Pagination start index (1-based). Default: 1.")
                .required(false)
                .type("integer")
                .defaultValue("1")
                .min("1")
                .build());
        fields.add(FieldDef.builder("count")
                .description("Number of records to return per page. Default: 25. Maximum: 100.")
                .required(false)
                .type("integer")
                .defaultValue("25")
                .min("1")
                .max("100")
                .build());
        fields.add(FieldDef.builder("fetchHits")
                .description("Whether to calculate and return the total record count. Default: true.")
                .required(true)
                .type("boolean")
                .defaultValue("true")
                .build());
        fields.add(FieldDef.builder("fetchData")
                .description("Whether to return the records row. Default: true.")
                .required(true)
                .type("boolean")
                .defaultValue("true")
                .build());

        return fields;
    }

    /**
     * Creates an empty JoinLoader with no pre-configured joins.
     * Matches the pattern from {@code SecureListAction.getJoinLoader()}.
     */
    private JoinLoader createEmptyJoinLoader() {
        return new JoinLoader() {
            private List<JoinClause> joinClauseList;
            private boolean joinExist;

            @Override
            public boolean isJoinExist() {
                return joinExist;
            }

            @Override
            public List<JoinClause> getJoins() {
                return joinClauseList;
            }

            @Override
            public void addJoinClause(final List<JoinClause> joinClauses) {
                joinClauseList = List.copyOf(joinClauses);
                joinExist = true;
            }
        };
    }

    /**
     * Determines whether the currently authenticated user is authorized to perform
     * all the specified actions within the given subcategory for the provided organization.
     *
     * @param ctx             Refers to the MCP tool context containing the user's identity and other relevant information.
     * @param subcategoryType The subcategory being accessed.
     * @param actionTypes     The actions that require authorization.
     * @return {@code true} if the user has permission for all specified actions;{@code false} otherwise.
     */
    protected final boolean hasPermission(final McpToolContext<CrmMcpIdentity> ctx, final SubCategory.Type subcategoryType, final Collection<Action.Type> actionTypes) {
        final Long orgId = ctx.identity().crmUser().orgId();
        final Map<com.tlc.crm.security.permission.model.Action.Type, RolePermission> permissionMap = rolePermissionService.getPermissionsByActionTypes(orgId,
                LoggedInUserInfo.getSecurityContext().getUser().getRoleId(), Category.Type.SOURCES, subcategoryType, actionTypes);
        final long grantedPermissionCount = permissionMap.values().stream().filter(RolePermission::isGranted).count();

        return grantedPermissionCount == permissionMap.size();
    }

    /**
     * Checks whether the specified source is enabled for the given MCP identity.
     *
     * @param mcpIdentity The MCP identity containing the source configuration
     * @param source      The source whose availability is to be verified
     * @return {@code true} if the source is enabled and can be used; {@code false} otherwise
     */
    protected final boolean isSourceEnabled(final CrmMcpIdentity mcpIdentity, final Source source) {
        return SourceSettingsService.getInstance().isSourceEnabled(mcpIdentity.crmUser().orgId(), source.getName());
    }

    /**
     * Resolves the full criteria list from the request JSON, including an optional
     * dateInterval shortcut criterion. If a dateInterval is specified, it is converted
     * into a filter criterion and added to the list. Any existing criteria in the
     * request are also included.
     *
     * @param requestJson The JSON object containing the request parameters.
     * @param orgId       The organization ID for context.
     * @param provider    The entity provider for which the criteria are being resolved.
     * @return A collection of resolved filter criteria, including any dateInterval criterion.
     */
    private Collection<?> resolveCriteriaList(final JsonObject requestJson, final long orgId, final Provider provider) {
        final List<Object> criteriaList = new ArrayList<>();
        final String dateIntervalAlias = requestJson.optString("dateInterval", null);

        if (dateIntervalAlias != null && !dateIntervalAlias.isBlank()) {
            final String dateField = requestJson.optString("dateField", DEFAULT_DATE_FIELD);

            criteriaList.add(buildDateIntervalCriterion(orgId, dateIntervalAlias, dateField, provider));
        }

        final Object criteriaParam = requestJson.opt("criteria");

        if (criteriaParam instanceof Collection<?> existingCriteria) {
            criteriaList.addAll(existingCriteria);
        }

        return criteriaList;
    }

    /**
     * Constructs a filter criterion map for a relative date interval shortcut.
     * This method resolves the date interval alias to a DateInterval object,
     * retrieves the corresponding field information, and builds a criterion
     * map that can be applied to the list interpreter.
     *
     * @param orgId             The organization ID for context.
     * @param dateIntervalAlias The alias of the relative date interval (e.g., "today", "this_week").
     * @param dateField         The internal name of the date field to filter on (default: "created_at").
     * @param provider          The entity provider for which the criterion is being built.
     * @return A map representing the filter criterion for the specified date interval.
     */
    private Map<String, Object> buildDateIntervalCriterion(final long orgId, final String dateIntervalAlias,
                                                           final String dateField, final Provider provider) {
        final DateIntervals dateIntervals = DateIntervalAliasResolver.resolve(dateIntervalAlias);
        final DateInterval dateInterval = dateIntervalService.get(dateIntervals);

        if (dateInterval == null) {
            throw new IllegalArgumentException(String.format("Unable to resolve date interval: '%s'", dateIntervalAlias));
        }

        final FieldInfo fieldInfo = fieldService.getDefaultFieldByInternalName(provider.getSource(), dateField)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Field '%s' not found for source '%s'", dateField, provider.getSource().getName())));
        final StoreType storeType = fieldInfo.storeType();

        if (!isDateStoreType(storeType)) {
            throw new IllegalArgumentException(String.format("Field '%s' is not a date field.", dateField));
        }

        final long fieldTypeId = fieldTypeService.getFieldTypeByStoreType(storeType).id();
        final Collection<Comparator> comparators = comparatorService.getComparatorsByType(fieldTypeId);
        final Comparator comparator = findEqualComparator(comparators);
        final I18nResolver i18nResolver = i18nService.getResolver(Locale.getDefault());
        final Map<String, Object> valueMap = new HashMap<>();

        valueMap.put("value", i18nResolver.get(dateInterval.getDisplayName()));
        valueMap.put("valueEntityId", dateInterval.getId());

        final Map<String, Object> fieldMap = Map.of("fieldId", fieldInfo.id());
        final Map<String, Object> criterion = new HashMap<>();

        criterion.put("field", fieldMap);
        criterion.put("comparatorId", comparator.getId());
        criterion.put("values", List.of(valueMap));
        criterion.put("connectorId", 1);

        return criterion;
    }

    /**
     * Determines if the given store type corresponds to a date-related field.
     *
     * @param storeType The store type to check.
     * @return {@code true} if the store type is DATE, DATE_TIME, DATE_RANGE, or DATE_TIME_RANGE; {@code false} otherwise.
     */
    private boolean isDateStoreType(final StoreType storeType) {
        return switch (storeType) {
            case DATE, DATE_TIME, DATE_RANGE, DATE_TIME_RANGE -> true;
            default -> false;
        };
    }

    /**
     * Finds the comparator with alias "EQUAL" from the provided collection.
     * If not found, returns the first comparator in the collection.
     *
     * @param comparators The collection of comparators to search.
     * @return The "EQUAL" comparator or the first comparator if "EQUAL" is not found.
     * @throws IllegalArgumentException if the comparators collection is null or empty.
     */
    private Comparator findEqualComparator(final Collection<Comparator> comparators) {
        if (comparators == null || comparators.isEmpty()) {
            throw new IllegalArgumentException("No comparators available for date field.");
        }

        return comparators.stream()
                .filter(comparator -> "EQUAL".equalsIgnoreCase(comparator.getAliasName()))
                .findFirst()
                .orElse(comparators.iterator().next());
    }

    /**
     * Applies filter criteria to the interpreter using the same pattern as
     * {@code EntityListAction.applyCriteria}.
     *
     * @param orgId        organization ID
     * @param provider     entity provider
     * @param interpreter  the SQL list interpreter
     * @param criteriaList raw criteria list from MCP input
     */
    private void applyFilterCriteria(final long orgId, final Provider provider,
                                     final EntitySQLListInterpreter interpreter, final Collection<?> criteriaList) {
        final Collection<FilterCriteria> filterCriteriaCollection = new ArrayList<>();

        for (final Object criteriaObj : criteriaList) {
            if (criteriaObj instanceof Map<?, ?> criteriaMap) {
                final JsonObject criteriaJson = buildCriteriaJson(criteriaMap);

                criteriaJson.put(CriteriaFields.SOURCE.getName(), provider.getSource().getName());
                filterCriteriaCollection.add(filterCriteriaBuilder.build(orgId, criteriaJson));
            }
        }

        if (!filterCriteriaCollection.isEmpty()) {
            interpreter.applyCriteria(orgId, provider, filterCriteriaCollection);
            LOGGER.debug("MCP list_{}: Applied {} filter criteria. Org: {}", getModuleName(), filterCriteriaCollection.size(), orgId);
        }
    }

    /**
     * Converts a raw criteria map from MCP input into the {@link JsonObject}
     * format expected by {@link FilterCriteriaBuilder#build}.
     */
    private JsonObject buildCriteriaJson(final Map<?, ?> criteriaMap) {
        final JsonObject criteriaJson = Json.object();

        criteriaMap.forEach((key, value) -> {
            final String keyStr = String.valueOf(key);

            if ("values".equals(keyStr) && value instanceof List<?> valuesList) {
                final JsonArray valuesArray = Json.array();

                for (final Object valueObj : valuesList) {
                    if (valueObj instanceof Map<?, ?> valueMap) {
                        final JsonObject valueJson = Json.object();

                        valueMap.forEach((vk, vv) -> valueJson.put(String.valueOf(vk), vv));
                        valuesArray.put(valueJson);
                    }
                }
                criteriaJson.put(keyStr, valuesArray);
            } else {
                criteriaJson.put(keyStr, value);
            }
        });

        return criteriaJson;
    }

}