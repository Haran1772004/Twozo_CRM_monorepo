package com.tlc.crm.core.action.dropdown;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.holder.ConfigManagerHolder;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.EntityConfigManager;

import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the entity user list dropdown action. It displays only the users who have permission to access the entity
 *
 * @author Maharaja S
 * @version 1.0
 */
@AsyncDropdown(name = "entityUserList")
public class EntityUserList implements AsyncDropdownAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityUserList.class);

    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final CrmUserConverter crmUserConverter;
    private final ConfigManagerHolder configManagerHolder;

    public EntityUserList() {
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        configManagerHolder = ConfigManagerHolder.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int limit = dropdownLimit.limit();
        final int start = dropdownLimit.start();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final CriteriaType criteriaType = dropdownCriteria.getCriteriaType();
        final Object value = dropdownCriteria.getValue();
        final JsonObject data = dropdownRequest.data();
        final Long entityId = data.getLong(FieldKeys.ENTITY_ID.getName());
        final EntityConfigManager<?> entityManager = (EntityConfigManager<?>) configManagerHolder.getManager(Source.get(data.getString(FieldKeys.SOURCE.getName())));

        if (!entityManager.exists(orgId, entityId)) {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
        final Collection<Long> excludedUserIds = entityManager.getRestrictedIamUserIdsForEntity(orgId, entityId);
        final Collection<SearchFilter> searchFilters = new ArrayList<>(NumericalConstant.TWO);
        final SearchFilter searchFilter = new SearchFilter();
        final SearchFilter stateFilter = new SearchFilter();

        searchFilter.setStart(start);
        searchFilter.setLimit(limit);
        searchFilter.setType(Objects.isNull(criteriaType) ? null : CriteriaType.getCriteriaType(criteriaType));
        searchFilter.setValue(Objects.isNull(value) ? null : value.toString());
        searchFilter.excludeInvitedUsers(true);
        searchFilter.includeDeactivatedUsers(false);
        searchFilter.setExcludeUserIds(excludedUserIds);

        stateFilter.setType(FieldKeys.STATE.getName());
        stateFilter.setValue(UserStateFilter.ACTIVE);

        searchFilters.add(searchFilter);
        searchFilters.add(stateFilter);
        crmResponse.convertIntoAsync();
        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final Collection<User> users = userPage.getData();
                    final Collection<User> authorizedUsers = new ArrayList<>(users.size());

                    for (final User user : users) {
                        if (Objects.nonNull(user) && !excludedUserIds.contains(user.id())) {
                            authorizedUsers.add(user);
                        }
                    }
                    final int totalRecords = userPage.getNumber();
                    final OptionContainer optionContainer = new OptionContainer(totalRecords);

                    if (totalRecords > 0 && start <= totalRecords) {
                        final Map<Long, User> userMap = authorizedUsers.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Map<Long, CrmUser> crmUserMap = crmUserService.getUsersByIamId(orgId, userMap.keySet()).stream()
                                .collect(Collectors.toMap(CrmUser::getIamId, Function.identity()));
                        final String nameKey = FieldKeys.NAME.getName();
                        final String emailKey = FieldKeys.EMAIL.getName();
                        final String jobTitleKey = FieldKeys.JOB_TITLE.getName();
                        final String valueKey = FieldKeys.VALUE.getName();
                        final String activeKey = FieldKeys.IS_ACTIVE.getName();
                        final String currentUserKey = FieldKeys.IS_LOGGED_IN_USER.getName();
                        final User.State activeState = User.State.ACTIVE;
                        final Long currentUserId = dropdownRequest.getUserId();
                        final String configKey = FieldKeys.CONFIG.getName();
                        final String thumbnailKey = FieldKeys.THUMBNAIL.getName();

                        for (final User user : authorizedUsers) {
                            final CrmUser crmUser = crmUserMap.get(user.id());

                            if (Objects.isNull(crmUser)) {
                                continue;
                            }
                            final Long userId = crmUser.id();
                            final JsonObject jsonObject = Json.object();
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
                                jsonObject.put(thumbnailKey, crmUserConverter.fetchProfileJson(thumbnail, orgId, userId));
                            }
                            optionContainer.addOption(jsonObject);
                        }
                    }
                    crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                    crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                    crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                    crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
                } catch (Throwable throwable) {
                    LOGGER.error("Crm User: Retrieved users but failed during conversion. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Crm User: Failed to retrieve users. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed"));
                crmResponse.send();
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }
}