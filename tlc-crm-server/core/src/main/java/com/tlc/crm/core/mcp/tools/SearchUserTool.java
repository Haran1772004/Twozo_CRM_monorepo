package com.tlc.crm.core.mcp.tools;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.mcp.CrmMcpIdentity;

import com.tlc.crm.core.mcp.CrmMcpTool;

import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;

import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;

import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP Tool - Search Users. This tool searches CRM users and returns matching user IDs and names for lookup fields.
 * It supports an optional 'query' parameter to filter results based on usernames.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpTool(
        name = "search_users",
        title = "Search Users",
        description = "Searches CRM users and returns matching user IDs and names for lookup fields. Supports an optional 'query' parameter to filter results.",
        version = "1.0",
        readOnly = true,
        mode = ExecutionMode.ASYNC
)
public class SearchUserTool extends CrmMcpTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchUserTool.class);

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 250;

    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;

    public SearchUserTool() {
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
    }

    /**
     * Returns available options for a dropdown/picklist field with cursor pagination.
     *
     * @param ctx      MCP tool context
     * @param query    Optional search/filter string for option labels
     * @param cursor   Opaque cursor from a previous response's {@code nextCursor} field; omit for the first page
     * @param pageSize Number of options to return per page (default: 50, max: 200)
     * @return JSON with paginated option list and cursor metadata
     */
    public McpToolResult execute(final McpToolContext<CrmMcpIdentity> ctx,
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
            final CrmUser crmUser = ctx.identity().crmUser();
            final long orgId = crmUser.orgId();
            final int effectivePageSize = resolvePageSize(pageSize);
            final int offset = decodeCursor(cursor);
            final DropdownLimit limit = new DropdownLimit(offset, effectivePageSize);
            final DropdownCriteria dropdownCriteria = new DropdownCriteria();
            final JsonArray optionsArray = Json.array();
            final AtomicLong totalRecords = new AtomicLong(0);
            final AtomicReference<Throwable> callbackFailure = new AtomicReference<>();

            dropdownCriteria.setCriteriaType("contains");
            dropdownCriteria.setCriteriaValue(query);

            final SearchFilter searchFilter = new SearchFilter();
            final SearchFilter stateFilter = new SearchFilter();
            final Collection<SearchFilter> searchFilters = new ArrayList<>();

            if (Objects.nonNull(query) && query.isBlank()) {
                stateFilter.setUserIds(Set.of(crmUserService.getUser(orgId, crmUser.id()).getIamId()));
            }
            stateFilter.setType(FieldKeys.STATE.getName());
            stateFilter.setValue(UserStateFilter.ACTIVE);
            searchFilters.add(stateFilter);

            searchFilter.setStart(offset);
            searchFilter.setLimit(effectivePageSize);
            searchFilter.setType(Objects.isNull(dropdownCriteria.getCriteriaType()) ? null : CriteriaType.getCriteriaType(dropdownCriteria.getCriteriaType()));
            searchFilter.setValue(query);
            searchFilter.excludeInvitedUsers(true);
            searchFilter.includeDeactivatedUsers(false);
            searchFilters.add(searchFilter);

            final CountDownLatch latch = new CountDownLatch(1);
            final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Page<User> userPage) {
                    try {
                        final Collection<User> userList = userPage.getData();
                        totalRecords.set(userPage.getNumber());

                        if (totalRecords.get() > 0 && offset <= totalRecords.get()) {
                            final Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::id, Function.identity()));
                            final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());

                            for (final CrmUser crmUser : crmUsers) {
                                final JsonObject jsonObject = Json.object();
                                final User user = userMap.get(crmUser.getIamId());

                                if (Objects.nonNull(user)) {
                                    jsonObject.put(OptionKeys.ID, crmUser.id());
                                    jsonObject.put(OptionKeys.NAME, user.getFullName());
                                    optionsArray.put(jsonObject);
                                }
                            }
                        }
                    } catch (Throwable throwable) {
                        callbackFailure.set(throwable);
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    callbackFailure.set(throwable);
                    latch.countDown();
                }
            };
            userService.searchUsers(organizationService.getIamOrgId(orgId), false, searchFilters, usersFetchCallBack);

            latch.await();

            if (Objects.nonNull(callbackFailure.get())) {
                LOGGER.error("MCP: Failed to search users. Reason: {}", callbackFailure.get().getMessage(), callbackFailure.get());
                return errorResult("Failed to search users");
            }

            final long totalRecordsCount = totalRecords.get();
            final boolean hasMore = (limit.start() + limit.limit()) < totalRecordsCount;
            final JsonObject response = Json.orderedObject();

            response.put("totalOptions", totalRecordsCount);
            response.put("pageSize", effectivePageSize);
            response.put("returnedCount", optionsArray.size());
            response.put("hasMore", hasMore);
            response.put("options", optionsArray);

            if (hasMore) {
                response.put("nextCursor", encodeCursor(limit.start() + limit.limit()));
            }

            return McpToolResult.json(response);
        } catch (Exception exception) {
            LOGGER.error("MCP: Failed to search users. Reason: {}", exception.getMessage(), exception);
            return errorResult("Failed to search users");
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