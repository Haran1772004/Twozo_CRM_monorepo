package com.tlc.crm.core.mcp.tools;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.mcp.CrmMcpIdentity;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.mcp.CrmMcpTool;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * MCP Tool — Get Field Options (with cursor pagination).
 *
 * <p>Returns valid dropdown/picklist values for a specific field within a CRM module,
 * with cursor-based pagination following the MCP 2025-03-26 specification.</p>
 *
 * <p>Pagination is cursor-based: pass the {@code nextCursor} value from the previous
 * response as the {@code cursor} parameter to retrieve the next page. The cursor is
 * an opaque string — do not parse or construct it manually.</p>
 *
 * <p>Supports an optional {@code query} parameter for searching/filtering options
 * when the list is large.</p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "get_field_options",
        title = "Get Field Options",
        description = "Returns valid dropdown/picklist option values for a field in a CRM module. Supports optional 'query' for filtering large option lists.",
        version = "1.0",
        readOnly = true
)
public class GetFieldOptionsTool extends CrmMcpTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetFieldOptionsTool.class);

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 250;

    private final OptionService optionService;
    private final FieldService fieldService;

    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final TagManager tagManager;
    private final TerritoryManager territoryManager;

    public GetFieldOptionsTool() {
        this.optionService = OptionService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
        this.lifeCycleStageManager = LifeCycleStageManager.getInstance();
        this.lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.tagManager = TagManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
    }

    /**
     * Returns available options for a dropdown/picklist field with cursor pagination.
     *
     * @param ctx       MCP tool context
     * @param fieldName Internal name of the dropdown field
     * @param query     Optional search/filter string for option labels
     * @param cursor    Opaque cursor from a previous response's {@code nextCursor} field; omit for the first page
     * @param pageSize  Number of options to return per page (default: 50, max: 200)
     * @return JSON with paginated option list and cursor metadata
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
                                 @ToolParam(name = "fieldName",
                                         description = "Internal name of the field to get options.") final String fieldName,
                                 @ToolParam(name = "query",
                                         description = "Optional search string to filter options by label.",
                                         required = false) final String query,
                                 @ToolParam(name = "cursor",
                                         description = "Opaque pagination cursor returned as 'nextCursor' from a previous response. Omit or pass null to start from the beginning.",
                                         required = false) final String cursor,
                                 @ToolParam(name = "pageSize",
                                         description = "Number of options per page. Default: 100, Maximum: 250.",
                                         required = true,
                                         min = "1",
                                         max = "250") final Integer pageSize) {
        try {
            if (fieldName == null || fieldName.isBlank()) {
                return errorResult("Parameter 'fieldName' is required.");
            }
            final I18nResolver i18nResolver = I18nServiceProvider.get().getResolver(Locale.getDefault());
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final int effectivePageSize = resolvePageSize(pageSize);
            final int offset = decodeCursor(cursor);
            final Optional<FieldInfo> optionalFieldInfo = fieldService.getFieldByInternalName(orgId, fieldName);

            if (optionalFieldInfo.isEmpty()) {
                return errorResult("Field not found");
            }
            final FieldInfo fieldInfo = optionalFieldInfo.get();
            final StoreType storeType = fieldInfo.storeType();
            final DropdownLimit limit = new DropdownLimit(offset, effectivePageSize);
            final DropdownCriteria dropdownCriteria = new DropdownCriteria();
            final JsonArray optionsArray = Json.array();
            long totalRecords = 0;

            dropdownCriteria.setCriteriaType("contains");
            dropdownCriteria.setCriteriaValue(query);

            if (StoreType.isSelectorType(storeType)) {
                switch (fieldInfo.aliasName()) {
                    case ContactFields.LIFECYCLE_STAGE -> {
                        totalRecords = lifeCycleStageManager.getStagesCount(orgId, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<LifeCycleStage> lifeCycleStageDropdownList = lifeCycleStageManager.getLifeCycleStageDropdownList(orgId, false, limit, dropdownCriteria);

                            for (final LifeCycleStage option : lifeCycleStageDropdownList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case ContactFields.LIFECYCLE_STATUS -> {
                        totalRecords = lifeCycleStatusManager.getTotalRecordsCount(orgId, null, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<LifeCycleStatus> lifeCycleStatusDropdownList = lifeCycleStatusManager.getLifeCycleStatusDropdownList(orgId, null, limit, dropdownCriteria);

                            for (final LifeCycleStatus option : lifeCycleStatusDropdownList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case ContactFields.TERRITORY -> {
                        final Criteria criteria = dropdownCriteria.build(Table.get(OMCRMTERRITORY.TABLE).getColumn(OMCRMTERRITORY.NAME));
                        totalRecords = territoryManager.getTotalCount(orgId, criteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<Territory> territoryDropDownList = territoryManager.getTerritoryDropDownList(orgId, limit.getLimitClause(), criteria);

                            for (final Territory option : territoryDropDownList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case ContactFields.TAGS -> {
                        totalRecords = tagManager.getTotalCount(orgId, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<Tag> tagList = tagManager.getTagList(orgId, limit, dropdownCriteria);

                            for (final Tag option : tagList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case DealFields.PIPELINE -> {
                        totalRecords = pipelineManager.getTotalCount(orgId, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<Pipeline> pipelineList = pipelineManager.getPipelineDropdownList(orgId, limit, dropdownCriteria);

                            for (final Pipeline option : pipelineList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case DealFields.PIPELINE_STAGE -> {
                        totalRecords = pipelineStageManager.getTotalCount(orgId, null, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<PipelineStage> pipelineStageList = pipelineStageManager.getDropdownList(orgId, null, limit, dropdownCriteria);

                            for (final PipelineStage option : pipelineStageList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case DealFields.PRIMARY_CONTACT, DealFields.RELATED_CONTACTS -> {
                        totalRecords = contactManager.getTotalCount(orgId, false, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<Contact> primaryContactList = contactManager.getContactDropdownList(orgId, limit, false, dropdownCriteria);

                            for (final Contact option : primaryContactList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getFullName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    case DealFields.COMPANY, CompanyFields.PARENT_COMPANY -> {
                        totalRecords = companyManager.getTotalCount(orgId, dropdownCriteria);

                        if (totalRecords > 0 && limit.start() <= totalRecords) {
                            final Collection<Company> companyDropdownList = companyManager.getCompanyDropdownList(orgId, limit, dropdownCriteria);

                            for (final Company option : companyDropdownList) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getName());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                    default -> {
                        final PresetConfig presetConfig = fieldInfo.presetConfig();
                        final DropdownType dropdownType = presetConfig.dropdownType();

                        if (DropdownType.STATIC == dropdownType || Objects.requireNonNull(dropdownType).isMutable()) {
                            final Collection<Option> options = optionService.getOptions(orgId, fieldInfo, dropdownCriteria, i18nResolver, "");
                            totalRecords = options.size();
                            final List<Option> list = options.stream().skip(offset - 1).limit(effectivePageSize).toList();

                            for (final Option option : list) {
                                final JsonObject optJson = Json.orderedObject();

                                optJson.put(OptionKeys.ID, option.id());
                                optJson.put(OptionKeys.VALUE, option.getOption());

                                optionsArray.put(optJson);
                            }
                        }
                    }
                }
            } else if (storeType.isCompanyType()) {
                totalRecords = companyManager.getTotalCount(orgId, dropdownCriteria);

                if (totalRecords > 0 && limit.start() <= totalRecords) {
                    final Collection<Company> companyDropdownList = companyManager.getCompanyDropdownList(orgId, limit, dropdownCriteria);

                    for (final Company option : companyDropdownList) {
                        final JsonObject optJson = Json.orderedObject();

                        optJson.put(OptionKeys.ID, option.id());
                        optJson.put(OptionKeys.VALUE, option.getName());

                        optionsArray.put(optJson);
                    }
                }
            } else if (storeType.isContactType()) {
                totalRecords = contactManager.getTotalCount(orgId, false, dropdownCriteria);

                if (totalRecords > 0 && limit.start() <= totalRecords) {
                    final Collection<Contact> primaryContactList = contactManager.getContactDropdownList(orgId, limit, false, dropdownCriteria);

                    for (final Contact option : primaryContactList) {
                        final JsonObject optJson = Json.orderedObject();

                        optJson.put(OptionKeys.ID, option.id());
                        optJson.put(OptionKeys.VALUE, option.getFullName());

                        optionsArray.put(optJson);
                    }
                }
            }

            final boolean hasMore = (limit.start() + limit.limit()) <= totalRecords;
            final JsonObject response = Json.orderedObject();

            response.put("fieldName", fieldName);
            response.put("totalOptions", totalRecords);
            response.put("pageSize", effectivePageSize);
            response.put("returnedCount", optionsArray.size());
            response.put("hasMore", hasMore);
            response.put("options", optionsArray);

            if (hasMore) {
                response.put("nextCursor", encodeCursor(limit.start() + limit.limit()));
            }

            return McpToolResult.json(response);
        } catch (Exception exception) {
            LOGGER.error("MCP: Failed to get options for field: {} Reason: {}", fieldName, exception.getMessage(), exception);
            return errorResult("Failed to get field options");
        }
    }

    /**
     * Encodes the given offset into an opaque cursor string.
     * Format: Base64(JSON {"offset": N, "v": 1})
     *
     * @param offset the 0-based offset to encode
     * @return opaque cursor string
     */
    private String encodeCursor(final int offset) {
        final String payload = "{\"offset\":" + offset + ",\"v\":1}";

        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes an opaque cursor string into the offset position.
     * Returns 0 if the cursor is null/blank (first page).
     *
     * @param cursor the opaque cursor string, or null
     * @return the 0-based offset
     * @throws IllegalArgumentException if the cursor is malformed
     */
    private int decodeCursor(final String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 1;
        }

        try {
            final byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            final String payload = new String(decoded, StandardCharsets.UTF_8);
            final int offsetStart = payload.indexOf("\"offset\":") + 9;
            final int offsetEnd = payload.indexOf(',', offsetStart);
            final int offsetValue = Integer.parseInt(payload.substring(offsetStart, offsetEnd > 0 ? offsetEnd : payload.indexOf('}', offsetStart)).trim());

            if (offsetValue < 0) {
                throw new IllegalArgumentException("Cursor offset cannot be negative.");
            }
            return offsetValue;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed cursor. Use the exact 'nextCursor' value from a previous response.", e);
        }
    }

    /**
     * Resolves the effective page size, clamping between 1 and {@link #MAX_PAGE_SIZE}.
     *
     * @param pageSize the requested page size (may be null)
     * @return the effective page size
     */
    private int resolvePageSize(final Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}