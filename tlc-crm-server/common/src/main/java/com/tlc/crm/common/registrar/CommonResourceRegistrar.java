package com.tlc.crm.common.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.crm.common.export.action.DataExportAction;
import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.UserDataMigrator;
import com.tlc.crm.common.iam.UserDataTransferAction;
import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.builder.annotation.PreferenceModelBuilder;
import com.tlc.crm.common.iam.api.preferences.model.builder.internal.store.PreferenceBuilderStore;
import com.tlc.crm.common.iam.internal.DataMigrationContainer;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.internal.OrgInitiatorContainer;

import com.tlc.crm.common.internal.export.code.DataExportErrorCodes;
import com.tlc.crm.common.internal.export.container.DataExportActionContainer;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.store.PermissionsVerifierStore;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.common.list.transformer.TransformerActionContainer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import java.lang.reflect.Constructor;

import java.lang.reflect.Method;
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
public class CommonResourceRegistrar implements ResourceRegistrar<CrmServerRegistrarData> {

    private final DataExportActionContainer dataExportActionContainer;
    private final OrgInitiatorContainer orgInitiatorContainer;
    private final DataMigrationContainer dataMigrationContainer;
    private final PermissionsVerifierStore permissionsVerifierStore;
    private final TransformerActionContainer transformerActionContainer;
    private final PreferenceBuilderStore preferenceBuilderStore;

    private CommonResourceRegistrar() {
        dataExportActionContainer = DataExportActionContainer.getInstance();
        orgInitiatorContainer = OrgInitiatorContainer.getInstance();
        dataMigrationContainer = DataMigrationContainer.getInstance();
        permissionsVerifierStore = PermissionsVerifierStore.getInstance();
        transformerActionContainer = TransformerActionContainer.getInstance();
        preferenceBuilderStore = PreferenceBuilderStore.getInstance();
    }

    private static final class Instance {
        private static final CommonResourceRegistrar COMMON_REGISTRAR = new CommonResourceRegistrar();
    }

    public static CommonResourceRegistrar getInstance() {
        return Instance.COMMON_REGISTRAR;
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
            final String className = classObj.getName();

            if (className.contains("action.export")) {
                try {
                    final DataExporter dataExporter = classObj.getAnnotation(DataExporter.class);

                    if (dataExporter != null) {
                        final Constructor<DataExportAction> constructor = (Constructor<DataExportAction>) classObj.getConstructor();

                        dataExportActionContainer.addExportAction(moduleName, dataExporter.name(), constructor.newInstance());
                    }
                } catch (Exception exception) {
                    throw ErrorCode.get(DataExportErrorCodes.FAILED_TO_REGISTER_DATA_EXPORTER_ACTIONS, exception);
                }
            } else if (className.contains("model.builder")) {
                try {
                    final PreferenceModelBuilder preferenceBuilder = classObj.getAnnotation(PreferenceModelBuilder.class);

                    if (Objects.nonNull(preferenceBuilder)) {
                        for (final Method method : classObj.getMethods()) {
                            if (method.isAnnotationPresent(InstanceProvider.class)
                                    && method.invoke(null) instanceof final PreferenceBuilder<?> builder) {
                                preferenceBuilderStore.add(moduleName, preferenceBuilder.type(), builder);
                                break;
                            }
                        }
                    }
                } catch (Exception exception) {
                    throw ErrorCode.get(PermissionErrorCodes.FAILED_TO_REGISTER_PERMISSION_VERIFIER_ACTIONS, exception);
                }
            } else if (className.contains("iam")) {
                try {
                    registerOrgInitiatorActions(moduleName, classObj);
                    registerUserDataMigrationActions(moduleName, classObj);
                } catch (ErrorCode errorCode) {
                    throw errorCode;
                } catch (Exception exception) {
                    throw ErrorCode.get(IamErrorCodes.FAILED_TO_REGISTER_ORG_INITIATOR_ACTIONS, exception);
                }
            } else if (className.contains("action.transformer")) {
                final Transformer annotation = classObj.getAnnotation(Transformer.class);

                if (annotation != null) {

                    try {
                        if (Transformer.Type.FIELD == annotation.type()) {
                            final Constructor<FieldTransformer> constructor = (Constructor<FieldTransformer>) classObj.getConstructor();

                            transformerActionContainer.addAction(moduleName, annotation, constructor.newInstance());
                        } else {
                            final Constructor<ModelTransformer> constructor = (Constructor<ModelTransformer>) classObj.getConstructor();

                            transformerActionContainer.addEntityTransformer(moduleName, annotation, constructor.newInstance());
                        }
                    } catch (Exception exception) {
                        throw ErrorCode.get(CommonErrorCodes.FAILED_TO_REGISTER_TRANSFORMER_ACTIONS, exception);
                    }
                }
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

        dataExportActionContainer.clear(moduleName);
        permissionsVerifierStore.remove(moduleName);
        orgInitiatorContainer.remove(moduleName);
        dataMigrationContainer.remove(moduleName);
        transformerActionContainer.clear(moduleName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        dataExportActionContainer.clearAll();
        permissionsVerifierStore.clear();
        orgInitiatorContainer.clearAll();
        dataMigrationContainer.clearAll();
        transformerActionContainer.clearAll();
    }

    /**
     * <p>
     * Registers the org initiator actions by given bundle
     * </p>
     *
     * @param moduleName Represents the name of the module.
     * @param classObj   Represents the class object.
     */
    private void registerOrgInitiatorActions(final String moduleName, final Class<?> classObj) {
        try {
            final OrgInitiator orgInitiator = classObj.getAnnotation(OrgInitiator.class);

            if (Objects.nonNull(orgInitiator)) {
                final Constructor<OrgInitiatorAction> constructor = (Constructor<OrgInitiatorAction>) classObj.getConstructor();

                orgInitiatorContainer.addAction(moduleName, orgInitiator.name(), constructor.newInstance());
            }
        } catch (final Exception exception) {
            throw ErrorCode.get(IamErrorCodes.FAILED_TO_REGISTER_ORG_INITIATOR_ACTIONS, exception);
        }
    }

    /**
     * <p>
     * Registers the user transformer actions
     * </p>
     *
     * @param moduleName Represents the name of the module.
     * @param classObj   Represents the class object.
     */
    private void registerUserDataMigrationActions(final String moduleName, final Class<?> classObj) {
        try {
            final UserDataMigrator userDataMigrator = classObj.getAnnotation(UserDataMigrator.class);

            if (Objects.nonNull(userDataMigrator)) {
                final Constructor<UserDataTransferAction> constructor = (Constructor<UserDataTransferAction>) classObj.getConstructor();

                dataMigrationContainer.addAction(moduleName, userDataMigrator.name(), constructor.newInstance());
            }
        } catch (final Exception exception) {
            throw ErrorCode.get(IamErrorCodes.FAILED_TO_REGISTER_USER_DATA_MIGRATION_ACTION, exception);
        }
    }
}
