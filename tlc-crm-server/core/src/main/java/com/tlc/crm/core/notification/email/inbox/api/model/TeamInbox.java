package com.tlc.crm.core.notification.email.inbox.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.email.notification.email.EmailNotificationModel;
import com.tlc.crm.core.notification.email.inbox.validator.annotation.TeamInboxValidator;
import com.tlc.crm.core.notification.email.inbox.status.TeamInboxErrorCodes;

import com.tlc.iam.common.status.IamErrorCodes;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that manages the team inbox with email address which is shared among the users of an organization
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@TeamInboxValidator(groups = Group.Update.class, message = "i18n.notifications.team.inbox.details.required")
public class TeamInbox implements EmailNotificationModel {

    private final TeamInboxManager teamInboxManager;

    public TeamInbox() {
        this.teamInboxManager = TeamInboxManager.getInstance();
    }

    @NotNull(groups = {Group.Delete.class, Group.Update.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = Group.Create.class, message = "i18n.notifications.team.inbox.invalid.emailAddress")
    private EmailAddress address;

    private Collection<CrmUser> users;
    private Collection<Team> teams;
    private Collection<Territory> territories;
    private State state;
    private Long ownerId;
    private VerificationState verificationState;
    private Long autoForwardedTime;
    private VisibilityStatus visibilityStatus;
    private CrmEmailDomain domain;

    public CrmEmailDomain getDomain() {
        return domain;
    }

    public void setDomain(final CrmEmailDomain domain) {
        this.domain = domain;
    }

    /**
     * Represents the verification state of team inbox
     * <p>
     * VERIFIED     Team inbox address has been verified
     * NOT_VERIFIED Team inbox address has not yet verified
     */
    public enum VerificationState {

        VERIFIED((short) 1),
        NOT_VERIFIED((short) 2);

        private final short id;

        VerificationState(final short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        public static VerificationState get(final short id) {
            return switch (id) {
                case 1 -> VERIFIED;
                case 2 -> NOT_VERIFIED;
                default -> throw ErrorCode.get(TeamInboxErrorCodes.INVALID_VERIFICATION_STATE);
            };
        }

        public boolean isVerified() {
            return Objects.equals(this, VERIFIED);
        }
    }

    /**
     * Represents the state of team inbox
     * <p>
     * ACTIVE   Team inbox is in active state
     * INACTIVE Team inbox is inactive state, removed by the user
     */
    public enum State {

        ACTIVE((short) 1),
        INACTIVE((short) 2);

        private final short id;

        State(final short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        public static State valueOf(final short id) {
            return switch (id) {
                case 1:
                    yield ACTIVE;
                case 2:
                    yield INACTIVE;
                default:
                    throw ErrorCode.get(IamErrorCodes.INVALID_USER_STATE);
            };
        }
    }

    /**
     * <p>
     * Represents the team inbox visibility status
     * </p>
     *
     * @author Ajith venkadesh
     * @version 1.0
     */
    public enum VisibilityStatus {
        EVERYONE((short) 1),
        DESIGNATED((short) 2);

        private final short id;

        VisibilityStatus(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static VisibilityStatus getVisibilityStatus(final String name) {
            return switch (name.toLowerCase()) {
                case "everyone" -> EVERYONE;
                case "designated" -> DESIGNATED;
                default -> throw ErrorCode.get(TeamInboxErrorCodes.INVALID_VISIBILITY_STATUS,
                        "i18n.notifications.team.inbox.invalid.visibility.status");
            };
        }

        public static VisibilityStatus get(final short id) {
            return switch (id) {
                case 1 -> EVERYONE;
                case 2 -> DESIGNATED;
                default -> throw ErrorCode.get(TeamInboxErrorCodes.INVALID_VISIBILITY_STATUS,
                        "i18n.notifications.team.inbox.invalid.visibility.status");
            };
        }
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public EmailAddress getAddress() {
        return address;
    }

    public void setAddress(final EmailAddress address) {
        this.address = address;
    }

    public Collection<CrmUser> getUsers() {
        return users;
    }

    public void setUsers(final Collection<CrmUser> users) {
        this.users = users;
    }

    public void addUser(final CrmUser crmUser) {
        this.users = this.users == null ? new ArrayList<>() : this.users;

        this.users.add(crmUser);
    }

    public Collection<Team> getTeams() {
        return teams;
    }

    public void setTeams(final Collection<Team> teams) {
        this.teams = teams;
    }

    public void addTeam(final Team team) {
        this.teams = this.teams == null ? new ArrayList<>() : this.teams;

        this.teams.add(team);
    }

    public Collection<Territory> getTerritories() {
        return territories;
    }

    public void setTerritories(final Collection<Territory> territories) {
        this.territories = territories;
    }

    public void addTerritory(final Territory territory) {
        this.territories = this.territories == null ? new ArrayList<>() : this.territories;

        this.territories.add(territory);
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public VerificationState getVerificationState() {
        return verificationState;
    }

    public void setVerificationState(final VerificationState verificationState) {
        this.verificationState = verificationState;
    }

    public Long getAutoForwardedTime() {
        return autoForwardedTime;
    }

    public void setAutoForwardedTime(final Long autoForwardedTime) {
        this.autoForwardedTime = autoForwardedTime;
    }

    public VisibilityStatus getVisibilityStatus() {
        return visibilityStatus;
    }

    public void setVisibilityStatus(final VisibilityStatus visibilityStatus) {
        this.visibilityStatus = visibilityStatus;
    }

    public Collection<Team> resolveTeams() {
        return Objects.isNull(teams) ? teams = teamInboxManager.fetchAssociatedTeams(orgId, id) : teams;
    }

    public Collection<Territory> resolveTerritories() {
        return Objects.isNull(territories) ? territories = teamInboxManager.fetchAssociatedTerritories(orgId, id) : territories;
    }

    public Collection<CrmUser> resolveUsers() {
        return Objects.isNull(users) ? users = teamInboxManager.fetchAssociatedUsers(orgId, id) : users;
    }

    public boolean getDomainVerificationStatus() {
        return Objects.nonNull(domain) && domain.getDomainStatus().isVerified() && domain.isActive();
    }
}