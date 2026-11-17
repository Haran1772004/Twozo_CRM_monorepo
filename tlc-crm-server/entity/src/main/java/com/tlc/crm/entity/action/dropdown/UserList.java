package com.tlc.crm.entity.action.dropdown;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.RestApiKeys;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.commons.page.Page;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link CrmUser} related drop-down actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@AsyncDropdown(name = "userList")
public class UserList implements AsyncDropdownAction {

    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final CrmUserConverter crmUserConverter;

    public UserList() {
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final Long orgId = dropdownRequest.getOrgId();
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int start = dropdownLimit.start();
        final int limit = dropdownLimit.limit();
        final SearchFilter searchFilter = new SearchFilter();
        final CriteriaType criteriaType = criteria.getCriteriaType();
        final Object value = criteria.getValue();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final String stateKey = FieldKeys.STATE.getName();
        final JsonObject data = dropdownRequest.data();
        final UserStateFilter userStateFilter = data.containsKey(stateKey) ? UserStateFilter.get(data.getString(stateKey)) : UserStateFilter.ALL_USERS;

        if (Objects.nonNull(value) && value.toString().isBlank()) {
            stateFilter.setUserIds(Set.of(crmUserService.getUser(orgId, dropdownRequest.getUserId()).getIamId()));
        }
        stateFilter.setType(stateKey);
        stateFilter.setValue(userStateFilter);
        searchFilters.add(stateFilter);
        searchFilter.setStart(start);
        searchFilter.setLimit(limit);
        searchFilter.setType(Objects.isNull(criteriaType) ? null : CriteriaType.getCriteriaType(criteriaType));
        searchFilter.setValue(Objects.isNull(value) ? null : value.toString());
        searchFilter.excludeInvitedUsers(true);
        searchFilter.includeDeactivatedUsers(UserStateFilter.DEACTIVATED == userStateFilter || UserStateFilter.ALL_USERS == userStateFilter);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                final Collection<User> userList = userPage.getData();
                final int totalRecords = userPage.getNumber();
                final OptionContainer optionContainer = new OptionContainer(totalRecords);

                if (totalRecords > 0 && start <= totalRecords) {
                    final Map<Long, User> userMap = userList.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                    final String nameKey = FieldKeys.NAME.getName();
                    final String emailKey = FieldKeys.EMAIL.getName();
                    final String jobTitleKey = FieldKeys.JOB_TITLE.getName();
                    final String valueKey = FieldKeys.VALUE.getName();
                    final String activeKey = FieldKeys.IS_ACTIVE.getName();
                    final String currentUserKey = FieldKeys.IS_LOGGED_IN_USER.getName();
                    final User.State activeState = User.State.ACTIVE;
                    final Long currentUserId = dropdownRequest.getUserId();
                    final String configKey = FieldKeys.CONFIG.getName();

                    if (CrmRequest.ClientType.ZAPIER == dropdownRequest.getClientType()) {
                        for (final CrmUser crmUser : crmUsers) {
                            final JsonObject jsonObject = Json.object();

                            jsonObject.put(OptionKeys.ID, crmUser.id());
                            jsonObject.put(OptionKeys.NAME, userMap.get(crmUser.getIamId()).getFullName());

                            optionContainer.addOption(jsonObject);
                        }
                        crmResponse.put(FieldKeys.LIST.getName(), optionContainer.getOptions());
                        crmResponse.send();

                        return;
                    } else if (CrmRequest.ClientType.REST_API == dropdownRequest.getClientType()) {
                        for (final CrmUser crmUser : crmUsers) {
                            final JsonObject jsonObject = Json.object();

                            jsonObject.put(OptionKeys.ID, crmUser.id());
                            jsonObject.put(OptionKeys.NAME, userMap.get(crmUser.getIamId()).getFullName());

                            optionContainer.addOption(jsonObject);
                        }
                        crmResponse.put(RestApiKeys.OPTIONS, optionContainer.getOptions());
                        crmResponse.put(RestApiKeys.TOTAL_RECORDS, totalRecords);
                        crmResponse.put(RestApiKeys.HAS_MORE, (start + limit) <= totalRecords);
                        crmResponse.put(RestApiKeys.SIZE, optionContainer.size());
                        crmResponse.send();

                        return;
                    } else {
                        for (final CrmUser crmUser : crmUsers) {
                            final Long userId = crmUser.id();
                            final JsonObject jsonObject = Json.object();
                            final User user = userMap.get(crmUser.getIamId());
                            final DisplayPicture thumbnail = user.getThumbnail();
                            final JsonObject configJson = Json.object();

                            jsonObject.put(valueKey, userId);
                            jsonObject.put(nameKey, user.getFullName());
                            jsonObject.put(emailKey, user.getEmail());
                            jsonObject.put(jobTitleKey, user.getJobTitle());
                            configJson.put(activeKey, Objects.equals(activeState, user.getUserState()));
                            configJson.put(currentUserKey, Objects.equals(currentUserId, crmUser.id()));
                            jsonObject.put(configKey, configJson);

                            if (Objects.nonNull(thumbnail)) {
                                jsonObject.put(FieldKeys.THUMBNAIL.getName(), crmUserConverter.fetchProfileJson(thumbnail, orgId, userId));
                            }
                            optionContainer.addOption(jsonObject);
                        }
                    }
                }
                crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                crmResponse.send();
            }
        };
        userService.searchUsers(iamOrgId, false, searchFilters, usersFetchCallBack);
    }
}
