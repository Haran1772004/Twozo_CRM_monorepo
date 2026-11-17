package com.tlc.crm.core.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.CrmSecureRestAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.formatter.ProductRestReqFormatter;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.ValueHolder;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Product} related REST web actions.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/product")
public class ProductRestManagementV1 extends CrmSecureRestAction<Product> {

    private final ProductRestReqFormatter productRestReqFormatter;
    private final ProductConverter productConverter;
    private final ProductManager productManager;
    private final ConverterConfigBuilder converterConfigBuilder;

    public ProductRestManagementV1() {
        this.productRestReqFormatter = ProductRestReqFormatter.getInstance();
        this.productConverter = ProductConverter.getInstance();
        this.productManager = ProductManager.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    @Override
    public void get(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long id = Long.parseLong(request.getParameter("id"));

        if (productManager.exists(orgId, id)) {
            fetchAndDispatchUser(request, productManager.get(request.orgId(), id), response);
        } else {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PUT)
    @Override
    public void put(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        final Product product = convert(request, request.getRequestJson());

        update(response, request.orgId(), request.userId(), eventData, List.of(product));
    }

    @Override
    public void getAll(final CrmRequest request, final CrmResponse response) {
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload of the resource to be created.
     * @param response The CRM response used to send the created resource or creation status.
     */
    @ApiRoute(method = HttpMethod.POST)
    @Override
    public void post(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final Product model = convert(request, request.getRequestJson());
        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setLocale(request.locale());
        create(response, orgId, userId, eventData, List.of(model));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the partial update payload.
     * @param response The CRM response used to return the modified resource or status details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PATCH)
    @Override
    public void patch(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        update(response, request.orgId(), request.userId(), eventData, List.of(convertPartialData(request, requestJson)));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.DELETE)
    @Override
    public void delete(final CrmRequest request, final CrmResponse response) {
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }
        delete(response, request.orgId(), request.userId(), eventData, List.of(productManager.get(request.orgId(), Long.parseLong(request.getParameter("id")))));
    }

    @Override
    public void options(final CrmRequest request, final CrmResponse response) {
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The CRM request containing contextual information.
     * @param jsonObject The JSON payload to be converted.
     * @return
     */
    @Override
    protected Product convert(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject productJson = productRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, productJson);

        return productConverter.convert(converterConfig, productJson);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Product> getConfigManager() {
        return productManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The CRM request containing contextual information.
     * @param jsonObject The JSON payload to be converted.
     */
    @Override
    protected Product convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject productJson = productRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, productJson);

        return productConverter.convert(converterConfig, productJson);
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link Product}
     * </p>
     *
     * @param product Represents the product details
     * @param users   Contains the collection of {@link User}
     */
    @Override
    public void setUser(final Product product, final Collection<User> users) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final ValueHolder valueHolder = product.getValueHolder();
        final CrmUser salesOwner = product.getSalesOwner();
        final CrmUser createdBy = product.getCreatedBy();
        final CrmUser updatedBy = product.getUpdatedBy();

        if (Objects.nonNull(salesOwner)) {
            salesOwner.setUser(userMap.get(salesOwner.getIamId()));
            product.setSalesOwner(salesOwner);
        }

        if (Objects.nonNull(createdBy)) {
            createdBy.setUser(userMap.get(createdBy.getIamId()));
            product.setCreatedBy(createdBy);
        }

        if (Objects.nonNull(updatedBy)) {
            updatedBy.setUser(userMap.get(updatedBy.getIamId()));
            product.setUpdatedBy(updatedBy);
        }

        if (Objects.nonNull(valueHolder)) {

            for (final CrmUser crmUser : valueHolder.getUserValues()) {
                crmUser.setUser(userMap.get(crmUser.getIamId()));
            }
        }
    }

    /**
     * <p>
     * Fetches the current models based on the request information
     * </p>
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of {@link TlcModel}
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final long id = Long.parseLong(request.getParameter("id"));
        final long orgId = request.orgId();

        return List.of(productManager.get(orgId, id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest crmRequest, final Product product) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, false, null);

        return productConverter.convertToRestJson(product, converterConfig);
    }
}
