package com.tlc.email.common.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.FolderInterval;
import com.tlc.email.common.model.config.FolderType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Responsible for converting the {@link FolderInterval}.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class FolderIntervalConverter implements Converter<JsonObject, FolderInterval> {

    private FolderIntervalConverter() {

    }

    /**
     * <p>
     * Gets the instance of {@link FolderIntervalConverter}.
     * </p>
     */
    private static class FolderIntervalConverterInstance {
        private static final FolderIntervalConverter INSTANCE = new FolderIntervalConverter();
    }

    public static FolderIntervalConverter getInstance() {
        return FolderIntervalConverterInstance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject represents the {@link JsonObject}
     * @return {@link FolderInterval}
     */
    @Override
    public FolderInterval convert(final JsonObject jsonObject) {
        return null;
    }

    /**
     * Gets the {@link Collection} of {@link FolderInterval}.
     *
     * @param jsonObject Represents the {@link JsonObject} of folder interval
     * @return {@link Collection} of {@link FolderInterval}
     */
    public Collection<FolderInterval> getFolderIntervals(final JsonObject jsonObject) {
        final Collection<FolderInterval> folderIntervals = new ArrayList<>();

        for (final String folderName : jsonObject.keySet()) {
            final JsonArray folderArray = jsonObject.getJsonArray(folderName);
            final FolderInterval folderInterval = new FolderInterval();
            final int folderSize = folderArray.size();

            for (int folderIndex = 0; folderIndex < folderSize; folderIndex++) {
                final JsonObject emailIntervalJson = folderArray.getJsonObject(folderIndex);

                folderInterval.addEmailInterval(new EmailInterval(
                        emailIntervalJson.getLong(EmailFields.START_TIME),
                        emailIntervalJson.getLong(EmailFields.END_TIME))
                );
            }
            folderInterval.setFolder(FolderType.valueOf(folderName.toUpperCase()));
            folderIntervals.add(folderInterval);
        }

        return folderIntervals;
    }

    /**
     * Gets the {@link Collection} of {@link FolderInterval}.
     *
     * @param jsonAsString Represents the folder interval {@link JsonObject} as string
     * @return {@link Collection} of {@link FolderInterval}
     */
    public Collection<FolderInterval> getFolderIntervals(final String jsonAsString) {
        return getFolderIntervals(Json.object(jsonAsString));
    }

    /**
     * Gets the date config as {@link JsonObject}.
     *
     * @param folderIntervals Represents the {@link Collection} of {@link FolderInterval}
     * @return date config {@link JsonObject}
     */
    public JsonObject getDateConfigAsJson(final Collection<FolderInterval> folderIntervals) {
        final JsonObject folderJson = Json.object();

        for (final FolderInterval folderInterval : folderIntervals) {
            final JsonArray folderArray = Json.array();

            folderInterval.getEmailIntervals().forEach(emailInterval -> {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(EmailFields.START_TIME, emailInterval.startTime());
                jsonObject.put(EmailFields.END_TIME, emailInterval.endTime());

                folderArray.put(jsonObject);
            });

            folderJson.put(folderInterval.getFolder().name(), folderArray);
        }

        return folderJson;
    }
}
