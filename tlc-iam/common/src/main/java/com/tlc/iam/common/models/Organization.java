package com.tlc.iam.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.models.validator.type.Signup;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * An entity that holds organization related attributes
 *
 * @author Selvakumar
 * @version 1.0
 */
public class Organization implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.iam.accounts.org.id.required")
    private Long id;

    @NotNull(groups = {Group.Update.class}, message = "i18n.iam.accounts.org.name.required")
    private String name;
    private boolean isActive;
    private boolean isInitialized;

    @Valid
    private User adminUser;
    private DateTime createdTime;
    private DateTime stateUpdatedTime;
    private long orgId;
    private long ownerId;
    private OrgSize orgSize;
    private UsersSize usersSize;
    private IndustryType industryType;

    @NotNull(groups = {Group.Create.class, Signup.class}, message = "i18n.iam.accounts.org.state.required")
    private Organization.State orgState;

    public Organization() {
    }

    /**
     * Represents the state of organization
     *
     * ACTIVE   Org is in active state
     * INACTIVE Org is inactive, data can be retrieved later
     * DELETE   Org is deleted, data can't be retrieved forever
     */
    public enum State {

        ACTIVE((short) 1),
        INACTIVE((short) 2),
        DELETE((short) 3);

        private final short id;

        State(final short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        public static Organization.State valueOf(final short id) {
            return switch (id) {
                case 1:
                    yield ACTIVE;
                case 2:
                    yield INACTIVE;
                case 3:
                    yield DELETE;
                default:
                    throw ErrorCode.get(IamErrorCodes.INVALID_ORG_STATE);
            };
        }
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public User getAdminUser() {
//        if (Objects.isNull(this.adminUser) && Objects.nonNull(this.ownerId)) {
//            this.adminUser = UserManager.getInstance().get(this.ownerId);
//        }

        return adminUser;
    }

    public void setAdminUser(final User adminUser) {
        this.adminUser = adminUser;
    }

    public OrgSize getOrgSize() {
        return orgSize;
    }

    public void setOrgSize(final OrgSize orgSize) {
        this.orgSize = orgSize;
    }

    public UsersSize getUsersSize() {
        return usersSize;
    }

    public void setUsersSize(final UsersSize usersSize) {
        this.usersSize = usersSize;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public IndustryType getIndustryType() {
        return this.industryType;
    }

    public void setIndustryType(final IndustryType industryType) {
        this.industryType = industryType;
    }

    public DateTime getCreatedTime() {
        return this.createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getStateUpdatedTime() {
        return stateUpdatedTime;
    }

    public void setStateUpdatedTime(final DateTime stateUpdatedTime) {
        this.stateUpdatedTime = stateUpdatedTime;
    }

    public State getOrgState() {
        return orgState;
    }

    public void setOrgState(final State orgState) {
        this.orgState = orgState;
    }

    /**
     * Represents the industry type
     */
    public enum IndustryType {

        BUSINESS_SERVICE((short) 1),
        SAAS((short) 2),
        FINANCIAL_SERVICES((short) 3),
        MANUFACTURING((short) 4),
        E_COMMERCE((short) 5),
        OTHERS((short) 6);

        private final short id;

        IndustryType(final short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        public static IndustryType valueOf(final short id) {
            return switch (id) {
                case 1 -> BUSINESS_SERVICE;
                case 2 -> SAAS;
                case 3 -> FINANCIAL_SERVICES;
                case 4 -> MANUFACTURING;
                case 5 -> E_COMMERCE;
                case 6 -> OTHERS;
                default -> throw ErrorCode.get(IamErrorCodes.INVALID_INDUSTRY_TYPE);
            };
        }
    }

    /**
     * Represents the org size
     */
    public enum OrgSize {

        ONE_ORGANIZATION("1", (short) 1),
        SMALL_GROUP("2-10 people", (short) 2),
        MID_SIZE_GROUP("11-50 people", (short) 3),
        LARGE_GROUP("51-100 people", (short) 4),
        VERY_LARGE_GROUP("101-250 people", (short) 5),
        MAJOR_GROUP("251-500", (short) 6),
        HUGE_GROUP("501-1000 people", (short) 7),
        LARGE_ORGANIZATION("1000+ people", (short) 8);

        private final short id;
        private final String name;

        OrgSize(final String name, final short id) {
            this.name = name;
            this.id = id;
        }

        public short id() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static OrgSize valueOf(final short id) {
            return switch (id) {
                case 1 -> ONE_ORGANIZATION;
                case 2 -> SMALL_GROUP;
                case 3 -> MID_SIZE_GROUP;
                case 4 -> LARGE_GROUP;
                case 5 -> VERY_LARGE_GROUP;
                case 6 -> MAJOR_GROUP;
                case 7 -> HUGE_GROUP;
                case 8 -> LARGE_ORGANIZATION;
                default -> throw ErrorCode.get(IamErrorCodes.INVALID_ORG_SIZE);
            };
        }
    }

    /**
     * Represents the users size
     */
    public enum UsersSize {

        ONE_USER("1", (short) 1),
        SMALL_GROUP("2-5 people", (short) 2),
        MID_SIZE_GROUP("6-10 people", (short) 3),
        LARGE_GROUP("11-50 people", (short) 4),
        VERY_LARGE_GROUP("50+ people", (short) 5);

        private final short id;
        private final String name;

        UsersSize(final String name, final short id) {
            this.name = name;
            this.id = id;
        }

        public short id() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static UsersSize valueOf(final short id) {
            return switch (id) {
                case 1 -> ONE_USER;
                case 2 -> SMALL_GROUP;
                case 3 -> MID_SIZE_GROUP;
                case 4 -> LARGE_GROUP;
                case 5 -> VERY_LARGE_GROUP;
                default -> throw ErrorCode.get(IamErrorCodes.INVALID_USER_SIZE);
            };
        }
    }
}

