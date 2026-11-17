package com.tlc.crm.workflow.action.dropdown.filter;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.page.Page;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

import com.tlc.i18n.I18nResolver;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;

import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the workflow filter user list dropdown action
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@AsyncDropdown(name = "workflowFilterUserList")
public class WorkflowFilterUserList implements AsyncDropdownAction {

    private static final String ALL_USERS_I18N_KEY = "i18n.workflow.filter.all.users";
    private static final String CURRENT_USER_I18N_KEY = "i18n.workflow.filter.me";

    private final OrganizationService organizationService;
    private final UserService userService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;

    public WorkflowFilterUserList() {
        organizationService = OrganizationService.getInstance();
        userService = UserService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final SearchFilter searchFilter = new SearchFilter();
        final SearchFilter stateFilter = new SearchFilter();
        final CriteriaType criteriaType = criteria.getCriteriaType();
        final Object value = criteria.getValue();
        final int start = dropdownLimit.start();
        final int limit = dropdownLimit.limit();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final I18nResolver i18nResolver = dropdownRequest.i18nResolver();

        stateFilter.setType(FieldKeys.STATE.getName());
        stateFilter.setValue(UserStateFilter.ALL_USERS);
        searchFilters.add(stateFilter);
        searchFilter.setStart(start);
        searchFilter.setLimit(limit);
        searchFilter.setType(Objects.isNull(criteriaType) ? null : CriteriaType.getCriteriaType(criteriaType));
        searchFilter.setValue(Objects.isNull(value) ? null : value.toString());
        searchFilter.includeDeactivatedUsers(true);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param userPage Represents the {@link Collection} of users
             */
            @Override
            public void onSuccess(final Page<User> userPage) {
                final int totalRecords = userPage.getNumber();
                final OptionContainer optionContainer = new OptionContainer(totalRecords);
                final Collection<User> userList = userPage.getData();

                if (!userList.isEmpty()) {
                    final long userId = dropdownRequest.getUserId();
                    final Map<Long, User> users = userList.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, users.keySet());

                    optionContainer.addOption(-1, i18nResolver.get(ALL_USERS_I18N_KEY));
                    optionContainer.addOption(userId, i18nResolver.get(CURRENT_USER_I18N_KEY));

                    for (final CrmUser crmUser : crmUsers) {

                        if (!Objects.equals(crmUser.id(), userId)) {
                            final User user = users.get(crmUser.getIamId());

                            optionContainer.addOption(crmUser.id(), user.getFullName());
                        }
                    }
                }
                crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
                crmResponse.send();
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                crmResponse.setErrorCode(ErrorCode.get(WorkflowErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                crmResponse.send();
            }
        };
        userService.fetchUserPage(true, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }
}
