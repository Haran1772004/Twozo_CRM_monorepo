package com.tlc.crm.entity.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.crm.common.list.transformer.TransformerActionContainer;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.dropdown.internal.DropdownActionContainer;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.internal.FilterActionContainer;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * <p>
 * The registrar that registers the given actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class EntityResourceRegistrar implements ResourceRegistrar<CrmServerRegistrarData> {

    private final DropdownActionContainer dropdownActionContainer;
    private final FilterActionContainer filterActionContainer;
    private final TransformerActionContainer transformerActionContainer;

    private EntityResourceRegistrar() {
        dropdownActionContainer = DropdownActionContainer.getInstance();
        filterActionContainer = FilterActionContainer.getInstance();
        transformerActionContainer = TransformerActionContainer.getInstance();
    }

    private static final class Instance {
        private static final EntityResourceRegistrar ENTITY_REGISTRAR = new EntityResourceRegistrar();
    }

    public static EntityResourceRegistrar getInstance() {
        return Instance.ENTITY_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final CrmServerRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();

        for (final Class<?> classObj : registrarData.getClasses()) {
            try {
                final String className = classObj.getName();

                if (className.contains("action.dropdown")) {
                    final Dropdown annotation = classObj.getAnnotation(Dropdown.class);
                    final StaticDropdown staticDropdownAnnotation = classObj.getAnnotation(StaticDropdown.class);
                    final AsyncDropdown asyncDropdownAnnotation = classObj.getAnnotation(AsyncDropdown.class);

                    if (Objects.nonNull(annotation)) {
                        final Constructor<DropdownAction> constructor = (Constructor<DropdownAction>) classObj.getConstructor();

                        dropdownActionContainer.addAction(moduleName, annotation, constructor.newInstance());
                    } else if (Objects.nonNull(staticDropdownAnnotation)) {
                        final Constructor<DropdownAction> constructor = (Constructor<DropdownAction>) classObj.getConstructor();

                        dropdownActionContainer.addAction(staticDropdownAnnotation, constructor.newInstance());
                    } else if (Objects.nonNull(asyncDropdownAnnotation)) {
                        final Constructor<AsyncDropdownAction> constructor = (Constructor<AsyncDropdownAction>) classObj.getConstructor();

                        dropdownActionContainer.addAction(asyncDropdownAnnotation, constructor.newInstance());
                    }
                } else if (className.contains("action.filter")) {
                    final Filter annotation = classObj.getAnnotation(Filter.class);

                    if (Objects.nonNull(annotation)) {
                        final Constructor<FilterAction> constructor = (Constructor<FilterAction>) classObj.getConstructor();

                        filterActionContainer.addAction(moduleName, annotation, constructor.newInstance());
                    }
                }
            } catch (Exception exp) {
                throw ErrorCode.get(EntityErrorCodes.FAILED_TO_REGISTER_ACTIONS, exp);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final CrmServerRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();

        dropdownActionContainer.clear(moduleName);
        filterActionContainer.clear(moduleName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        dropdownActionContainer.clearAll();
        filterActionContainer.clearAll();
        transformerActionContainer.clearAll();
    }
}
