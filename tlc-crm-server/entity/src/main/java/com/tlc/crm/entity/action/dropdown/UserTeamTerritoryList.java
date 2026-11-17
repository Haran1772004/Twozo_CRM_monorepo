package com.tlc.crm.entity.action.dropdown;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link CrmUser} related dropdown actions.
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@AsyncDropdown(name = "userTeamTerritoryList")
public class UserTeamTerritoryList implements AsyncDropdownAction {

    private static final int DEFAULT_START_VALUE = 1;
    private static final String USER_TYPE = "USER";
    private static final String TEAM_TYPE = "TEAM";
    private static final String TERRITORY_TYPE = "TERRITORY";
    private static final String COUNT = "count";
    private static final String REQUIRED_USER_COUNT = "requiredUserCount";

    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;

    public UserTeamTerritoryList() {
        iamUserService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
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
        final Long iamOrgId = organizationService.getIamOrgId(orgId);
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final JsonObject dropdownData = dropdownRequest.data();
        final int limit = dropdownLimit.limit();
        final int start = dropdownLimit.start();
        final SearchFilter searchFilter = new SearchFilter();
        final CriteriaType criteriaType = criteria.getCriteriaType();
        final Object value = criteria.getValue();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();
        final boolean requiredUserCount = dropdownData.optBoolean( REQUIRED_USER_COUNT );

        stateFilter.setType(FieldKeys.STATE.getName());
        stateFilter.setValue(UserStateFilter.ALL_USERS);
        searchFilters.add(stateFilter);
        searchFilter.setLimit(limit);
        searchFilter.setStart(start);
        searchFilter.setType(Objects.isNull(criteriaType) ? null : CriteriaType.getCriteriaType(criteriaType));
        searchFilter.setValue(Objects.isNull(value) ? null : value.toString());
        searchFilter.excludeInvitedUsers(true);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<User> userPage) {
                final OptionContainer optionContainer = new OptionContainer();
                final int usersSize = userPage.getNumber();
                final Collection<User> users = userPage.getData();
                final String valueKey = FieldKeys.VALUE.getName();
                final String nameKey = FieldKeys.NAME.getName();
                final String typeKey = FieldKeys.TYPE.getName();
                final Collection<Team> teams = new ArrayList<>();
                final Collection<Territory> territories = new ArrayList<>();

                if (usersSize > 0 && start <= usersSize) {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                    final String activeKey = FieldKeys.IS_ACTIVE.getName();
                    final String loggedInUserKey = FieldKeys.IS_LOGGED_IN_USER.getName();
                    final User.State activeState = User.State.ACTIVE;
                    final Long currentUserId = dropdownRequest.getUserId();
                    final String configKey = FieldKeys.CONFIG.getName();

                    for (final CrmUser crmUser : crmUsers) {
                        final User user = userMap.get(crmUser.getIamId());
                        final JsonObject jsonObject = Json.object();
                        final JsonObject configJson = Json.object();

                        jsonObject.put(valueKey, crmUser.id());
                        jsonObject.put(nameKey, user.getFullName());
                        jsonObject.put(typeKey, USER_TYPE);
                        configJson.put(activeKey, Objects.equals(activeState, user.getUserState()));
                        configJson.put(loggedInUserKey, Objects.equals(currentUserId, crmUser.id()));
                        jsonObject.put(configKey, configJson);
                        optionContainer.addOption(jsonObject);
                    }
                }
                final long teamSize = teamManager.getTeamsCount(orgId, searchFilter);
                final int requiredSize = start + limit - 1;

                if (usersSize < requiredSize) {
                    final int teamStart = usersSize == 0 ? start : start > usersSize ? start - usersSize : DEFAULT_START_VALUE;
                    final int teamLimit = requiredSize - usersSize;

                    if (teamStart <= teamSize) {
                        final LimitClause limitClause = new DropdownLimit(teamStart, teamLimit).getLimitClause();
                        final Criteria teamCriteria = criteria.build(Table.get(OMCRMTEAM.TABLE).getColumn(OMCRMTEAM.NAME));

                        teams.addAll( teamManager.getTeamDropDownList( orgId, requiredUserCount, limitClause, teamCriteria ) );

                        for( final Team team : teams )
                        {
                            final JsonObject jsonObject = Json.object();

                            jsonObject.put(valueKey, team.id());
                            jsonObject.put(nameKey, team.getName());
                            jsonObject.put(typeKey, TEAM_TYPE);

                            if( requiredUserCount )
                            {
                                jsonObject.put( COUNT, team.getUsersCount() );
                            }

                            optionContainer.addOption(jsonObject);
                        }
                    }
                }
                final int teamUsersSize = (int) (usersSize + teamSize);
                final long territoryCount = territoryManager.getTerritoriesCount(orgId, searchFilter);

                if (teamUsersSize < requiredSize) {
                    final int territoryStart = teamUsersSize == 0 ? start : start > teamUsersSize ? start - teamUsersSize : DEFAULT_START_VALUE;
                    final int territoryLimit = requiredSize - teamUsersSize;

                    if (territoryStart <= territoryCount) {
                        final LimitClause limitClause = new DropdownLimit(territoryStart, territoryLimit).getLimitClause();
                        final Criteria territoryCriteria = criteria.build(Table.get(OMCRMTERRITORY.TABLE).getColumn(OMCRMTERRITORY.NAME));
                        territories.addAll(territoryManager.getTerritoryDropDownList(orgId, requiredUserCount, limitClause, territoryCriteria));

                        for( final Territory territory : territories )
                        {
                            final JsonObject jsonObject = Json.object();

                            jsonObject.put(valueKey, territory.id());
                            jsonObject.put(nameKey, territory.getName());
                            jsonObject.put(typeKey, TERRITORY_TYPE);

                            if( requiredUserCount )
                            {
                                jsonObject.put( COUNT, territory.getUsersCount() );
                            }
                            optionContainer.addOption(jsonObject);
                        }
                    }
                }
                final long totalCount = teamUsersSize + territoryCount;

                crmResponse.put(FieldKeys.HAS_MORE.getName(), requiredSize < totalCount);
                crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                crmResponse.setErrorCode(ErrorCode.get(EntityErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed"));
                crmResponse.put(CrmUserFields.ERRORS.getName(), crmResponse.resolveI18nString("i18n.iam.user.retrieval.failed"));
                crmResponse.send();
            }
        };
        iamUserService.fetchUserPage(false, iamOrgId, searchFilters, usersFetchCallBack);
    }
}
