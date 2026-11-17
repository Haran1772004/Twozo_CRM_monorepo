package com.tlc.crm.security.permission.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.io.Serializable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * A model that represents the requisites of the sub-category/action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Requisites implements Serializable {

    private final String value;
    private final String dependsOn;
    private final boolean hasSubType;
    private final Collection<ApiDetails> relativeApis;
    private final String subType;
    private final Collection<ApiDetails> ignorableApis;

    public Requisites(final JsonObject requisites) {
        this.value = requisites.optString(RequisitesAttributes.VALUE.getName(), "");
        this.dependsOn = requisites.optString(RequisitesAttributes.DEPENDS_ON.getName(), "");
        this.hasSubType = requisites.optBoolean(RequisitesAttributes.HAS_SUB_TYPE.getName());
        this.subType = requisites.optString(RequisitesAttributes.SUB_TYPE.getName(), "");
        this.relativeApis = buildApiDetails(RequisitesAttributes.RELATIVE_APIS.getName(), requisites);
        this.ignorableApis = buildApiDetails(RequisitesAttributes.IGNORABLE_APIS.getName(), requisites);
    }

    /**
     * <p>
     * Builds the {@link ApiDetails} from the requisites
     * </p>
     *
     * @param key        Represents the key in json
     * @param requisites Represents the json data
     * @return the collection of {@link ApiDetails}
     */
    private Collection<ApiDetails> buildApiDetails(final String key, final JsonObject requisites) {
        if (requisites.containsKey(key)) {
            final JsonArray relativeApisArray = requisites.getJsonArray(key);

            return relativeApisArray.toList().stream().map(relativeApi ->
                    new ApiDetails(Json.object((Map<String, ?>) relativeApi))).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * <p>
     * Provides the API types which related to the sub-category/action.
     * </p>
     *
     * @return Collection of API types.
     */
    public Collection<ApiDetails> getRelativeApis() {
        return relativeApis;
    }

    /**
     * <p>
     * Provides the value which related to the dependent type.
     * </p>
     *
     * @return value of the type.
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Provides the dependent value which represents the dependent type value.
     * </p>
     *
     * @return value of the type.
     */
    public String getDependsOn() {
        return dependsOn;
    }

    /**
     * <p>
     * Provides a flag that indicates whether the current requisite has subtype or not.
     * </p>
     *
     * @return Subtype available or not.
     */
    public boolean hasSubType() {
        return hasSubType;
    }

    /**
     * <p>
     * Gets the value of subtype.
     * </p>
     *
     * @return value of the type.
     */
    public String getSubType() {
        return subType;
    }

    /**
     * <p>
     * Checks whether the {@link ApiDetails} is ignorable or not
     * </p>
     *
     * @param apiDetails Represents the {@link ApiDetails}
     * @return true if the {@link ApiDetails} is ignorable, false otherwise
     */
    public boolean isIgnorableApi(final ApiDetails apiDetails) {
        return ignorableApis.contains(apiDetails);
    }
}
