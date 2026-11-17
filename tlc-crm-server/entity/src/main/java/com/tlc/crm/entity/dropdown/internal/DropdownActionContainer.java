package com.tlc.crm.entity.dropdown.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * <p>
 * A container which holds the dropdown action of entity of multiple groups
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class DropdownActionContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropdownActionContainer.class);
    private static final String ENTITY_OPTIONS = "entityOptions";

    private final Map<String, Map<String, DropdownAction>> registeredActions;
    private final Map<String, DropdownAction> registeredStaticActions;
    private final Map<String, AsyncDropdownAction> registeredAsyncActions;

    private DropdownActionContainer() {
        registeredActions = new HashMap<>();
        registeredStaticActions = new HashMap<>();
        registeredAsyncActions = new HashMap<>();
    }

    private static class Instance {
        private static final DropdownActionContainer INSTANCE = new DropdownActionContainer();
    }

    public static DropdownActionContainer getInstance() {
        return DropdownActionContainer.Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the dropdown action into container
     * </p>
     *
     * @param groupName Dropdown group
     * @param dropdown  Holds Dropdown details
     * @param action    Holds dropdown action
     */
    public void addAction(final String groupName, final Dropdown dropdown, final DropdownAction action) {
        final Map<String, DropdownAction> fieldAction = registeredActions.computeIfAbsent(groupName, k -> new HashMap<>());
        final String name = dropdown.name();
        final String fieldAlias = dropdown.fieldAlias();
        final DropdownAction oldAction = fieldAction.get(fieldAlias);
        final DropdownAction registerAction = fieldAction.get(name);

        if (Objects.isNull(oldAction) && Objects.isNull(registerAction)) {
            LOGGER.info("Registering dropdown action for field {}", fieldAlias);
            fieldAction.put(fieldAlias, action);
            fieldAction.put(name, action);
        } else {
            throw ErrorCode.get(EntityErrorCodes.DROPDOWN_ACTION_ALREADY_REGISTERED,
                    String.format("Field Alias : %s, Name : %s", fieldAlias, name));
        }
    }

    /**
     * <p>
     * Adds the static dropdown action into container
     * </p>
     *
     * @param dropdown  Holds static Dropdown details
     * @param action    Holds dropdown action
     */
    public void addAction(final StaticDropdown dropdown, final DropdownAction action) {
        registeredStaticActions.put(dropdown.name(), action);
    }

    /**
     * <p>
     * Adds the asynchronous dropdown action into container
     * </p>
     *
     * @param dropdown  Holds asynchronous Dropdown details
     * @param action    Holds dropdown action
     */
    public void addAction(final AsyncDropdown dropdown, final AsyncDropdownAction action) {
        registeredAsyncActions.put(dropdown.name(), action);
    }

    /**
     * <p>
     * Fetches the dropdown action of a field from container
     * </p>
     *
     * @param fieldAlias Alias name of default field
     * @return If dropdown action available for the given alias, it will return the action
     */
    public DropdownAction fetchAction(final String fieldAlias) {
        final Collection<Map<String, DropdownAction>> fieldActions = registeredActions.values();

        for (final Map<String, DropdownAction> fieldAction : fieldActions) {
            final DropdownAction dropdownAction = fieldAction.get(fieldAlias);

            if (Objects.nonNull(dropdownAction)) {
                return dropdownAction;
            }
        }

        return fetchStaticAction(fieldAlias);
    }

    /**
     * <p>
     * Fetches the static dropdown action of a field from container
     * </p>
     *
     * @param dropdownName name of default field
     * @return If static dropdown action available,
     * it will return the action otherwise it will throw an error
     */
    public DropdownAction fetchStaticAction(final String dropdownName) {
        final Collection<DropdownAction> staticDropdownActions = registeredStaticActions.values();

        if (!staticDropdownActions.isEmpty()) {
            final DropdownAction staticDropdownAction = registeredStaticActions.get(dropdownName);

            if (Objects.isNull(staticDropdownAction)) {
                final DropdownAction dropdownAction = fetchStaticAction(ENTITY_OPTIONS);

                if (Objects.isNull(dropdownAction)) {
                    throw ErrorCode.get(EntityErrorCodes.DROPDOWN_ACTION_NOT_FOUND);
                }
                return dropdownAction;
            }

            return staticDropdownAction;
        }

        return null;
    }

    /**
     * <p>
     * Fetches the async dropdown action of a field from container
     * </p>
     *
     * @param dropdownName name of default field
     * @return If async dropdown action available,
     * it will return the action otherwise it will throw an error
     */
    public AsyncDropdownAction fetchAsyncAction(final String dropdownName) {
        final Collection<AsyncDropdownAction> asyncDropdownActions = registeredAsyncActions.values();

        if (!asyncDropdownActions.isEmpty()) {
            final AsyncDropdownAction asyncDropdownAction = registeredAsyncActions.get(dropdownName);

            if (Objects.nonNull(asyncDropdownAction)) {
                return asyncDropdownAction;
            }
        }
        return null;
    }

    /**
     * <p>
     * Fetches the dropdown action of a field from container
     * </p>
     *
     * @param field  Which contains the field info
     * @return it will return the action otherwise it will throw an error
     */
    public DropdownAction fetchAction(final FieldInfo field) {
        if (field.isPersistent()) {
            return fetchAction(ENTITY_OPTIONS);
        } else {
            final PresetConfig presetConfig = field.presetConfig();

            return fetchAction(presetConfig.contains(PresetConfig.Configuration.DROPDOWN) ? presetConfig.dropdown() : field.aliasName());
        }
    }

    /**
     * <p>
     * Returns the count of registered dropdown actions in a group
     * </p>
     *
     * @param groupName Group name of dropdown action
     * @return Count of registered dropdown actions
     */
    public int count(final String groupName) {
        return count(registeredActions.get(groupName));
    }

    /**
     * <p>
     * Returns the count of registered dropdown actions in a group
     * </p>
     *
     * @param actions Group name of dropdown action
     * @return Count of registered dropdown actions
     */
    private int count(final Map<String, DropdownAction> actions) {
        return Objects.isNull(actions) || actions.isEmpty() ? 0 : actions.values().size();
    }

    /**
     * <p>
     * Clears the dropdown actions registered in a group
     * </p>
     *
     * @param groupName - Group name of dropdown action
     */
    public void clear(final String groupName) {
        final Map<String, DropdownAction> actions = registeredActions.remove(groupName);

        if (Objects.nonNull(actions) && !actions.isEmpty()) {
            LOGGER.info("Total unregistered dropdown actions count {}, group {} ", count(actions), groupName);
        }
    }

    /**
     * Removes all registered dropdown actions across all groups.
     */
    public void clearAll() {
        registeredActions.clear();
    }
}
