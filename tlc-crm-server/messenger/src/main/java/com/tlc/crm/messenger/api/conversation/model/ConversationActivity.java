package com.tlc.crm.messenger.api.conversation.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.l10n.temporal.model.DateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * <p>
 * Pertains to conversation's activity contents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
@Setter
@NoArgsConstructor
public class ConversationActivity {

    private Long id;
    private Long orgId;
    private ConversationActivityTemplate conversationActivityTemplate;
    private Long timeStamp;
    private PlaceHolderValue placeHolderValue;

    /**
     * <p>
     * Inscribes place holder value in Json format.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    public static class PlaceHolderValue {

        private final JsonObject valueAsJson;

        public PlaceHolderValue(final Object... values) {
            final JsonObject jsonObject = Json.object();

            if (Objects.nonNull(values)) {
                IntStream.range(NumericalConstant.ZERO, values.length)
                        .forEach(index -> jsonObject.put(new StringBuilder(MessengerFields.PLACEHOLDER).
                                append("_").append(index + NumericalConstant.ONE).toString(), values[index]));
            }
            this.valueAsJson = jsonObject;
        }

        public PlaceHolderValue(final String value) {
            this.valueAsJson = Objects.isNull(value) || value.isBlank() ? Json.object() : Json.object(value);
        }

        /**
         * <p>
         * Gets the place holder value in Json format.
         * </p>
         *
         * @return lace holder value in Json format.
         */
        public JsonObject getValueAsJson() {
            return valueAsJson;
        }

        /**
         * <p>
         * Gets the place holder value in String format.
         * </p>
         *
         * @return lace holder value in Json format.
         */
        public String getValueAsString() {
            return valueAsJson.toString();
        }
    }
}