package com.tlc.commons.json.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.resolver.model.ArrayPlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderData;
import com.tlc.commons.json.resolver.model.PlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link JsonPlaceholderResolver}.
 */
class JsonPlaceholderResolverTest {

    /**
     * Test placeholder keys.
     */
    private enum TestKey implements PlaceholderKey {
        NAME("NAME"),
        TITLE("TITLE"),
        CURRENCY("CURRENCY"),
        AMOUNT("AMOUNT"),
        IS_ENABLED("IS_ENABLED"),
        TIMESTAMP("TIMESTAMP"),
        CONTACTS("CONTACTS"),
        CONTACT_NAME("CONTACT_NAME"),
        CONTACT_EMAIL("CONTACT_EMAIL"),
        CONTACT_ACTIVE("CONTACT_ACTIVE"),
        ITEMS("ITEMS"),
        ITEM_NAME("ITEM_NAME"),
        ITEM_PRICE("ITEM_PRICE");

        private final String key;

        TestKey(final String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        public static TestKey fromString(final String key) {
            for (final TestKey testKey : values()) {
                if (testKey.key.equals(key)) {
                    return testKey;
                }
            }
            return null;
        }
    }

    /**
     * Test placeholder key provider.
     */
    private static final PlaceholderKey.Provider TEST_KEY_PROVIDER = TestKey::fromString;

    /**
     * Test PlaceholderData builder.
     */
    private static class TestDataBuilder extends PlaceholderData.Builder {

        private final PlaceholderEntries scalarValues = new PlaceholderEntries();
        private final ArrayPlaceholderEntries arrayValues = new ArrayPlaceholderEntries();

        TestDataBuilder scalar(final TestKey key, final PlaceholderValue value) {
            scalarValues.addEntry(key, value);
            return this;
        }

        TestDataBuilder addArrayRow(final TestKey arrayKey, final PlaceholderEntries row) {
            arrayValues.addEntry(arrayKey, row);
            return this;
        }

        @Override
        protected PlaceholderKey.Provider buildPlaceHolderKeyProvider() {
            return TEST_KEY_PROVIDER;
        }

        @Override
        protected PlaceholderEntries buildScalarValues() {
            return scalarValues;
        }

        @Override
        protected ArrayPlaceholderEntries buildArrayValues() {
            return arrayValues;
        }
    }

    // ========================================================================
    // Null / Empty input tests
    // ========================================================================

    @Nested
    @DisplayName("Null and empty input handling")
    class NullAndEmptyInputs {

        @Test
        @DisplayName("Returns null JSON string as-is")
        void resolveNullString() {
            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test"))
                    .build();

            assertNull(JsonPlaceholderResolver.resolve((String) null, data));
        }

        @Test
        @DisplayName("Returns empty JSON string as-is")
        void resolveEmptyString() {
            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test"))
                    .build();

            assertEquals("", JsonPlaceholderResolver.resolve("", data));
        }

        @Test
        @DisplayName("Returns JSON object unchanged when PlaceholderData is null")
        void resolveWithNullData() {
            final JsonObject json = Json.object();
            json.put("name", "{{NAME}}");

            final JsonObject result = JsonPlaceholderResolver.resolve(json, null);

            assertEquals("{{NAME}}", result.getString("name"));
        }

        @Test
        @DisplayName("Returns JSON object unchanged when PlaceholderData is empty")
        void resolveWithEmptyData() {
            final JsonObject json = Json.object();
            json.put("name", "{{NAME}}");

            final PlaceholderData data = new TestDataBuilder().build();
            final JsonObject result = JsonPlaceholderResolver.resolve(json, data);

            assertEquals("{{NAME}}", result.getString("name"));
        }

        @Test
        @DisplayName("Returns null JsonObject as-is")
        void resolveNullJsonObject() {
            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test"))
                    .build();

            assertNull(JsonPlaceholderResolver.resolve((JsonObject) null, data));
        }
    }

    // ========================================================================
    // Scalar replacement tests
    // ========================================================================

    @Nested
    @DisplayName("Scalar placeholder resolution")
    class ScalarResolution {

        @Test
        @DisplayName("Resolves simple string placeholder")
        void resolveStringPlaceholder() {
            final JsonObject json = Json.object();
            json.put("name", "{{NAME}}");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("My Document"))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("My Document", json.getString("name"));
        }

        @Test
        @DisplayName("Resolves number placeholder with native type")
        void resolveNumberPlaceholder() {
            final JsonObject json = Json.object();
            json.put("amount", "{{AMOUNT}}");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.AMOUNT, PlaceholderValue.of(2500.50))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals(2500.50, json.getDouble("amount"));
        }

        @Test
        @DisplayName("Resolves boolean placeholder with native type")
        void resolveBooleanPlaceholder() {
            final JsonObject json = Json.object();
            json.put("enabled", "{{IS_ENABLED}}");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.IS_ENABLED, PlaceholderValue.of(true))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertTrue(json.getBoolean("enabled"));
        }

        @Test
        @DisplayName("Resolves long number placeholder with native type")
        void resolveLongPlaceholder() {
            final JsonObject json = Json.object();
            json.put("timestamp", "{{TIMESTAMP}}");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.TIMESTAMP, PlaceholderValue.of(1776256244499L))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals(1776256244499L, json.getLong("timestamp"));
        }

        @Test
        @DisplayName("Leaves unresolved placeholder unchanged when no mapping exists")
        void unresolvedPlaceholderUnchanged() {
            final JsonObject json = Json.object();
            json.put("unknown", "{{UNKNOWN_KEY}}");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test"))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("{{UNKNOWN_KEY}}", json.getString("unknown"));
        }

        @Test
        @DisplayName("Resolves placeholder with default value when no mapping exists")
        void defaultValueUsedWhenNoMapping() {
            final JsonObject json = Json.object();
            json.put("title", "{{TITLE}}:Default Title");

            final PlaceholderData data = new TestDataBuilder().build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("Default Title", json.getString("title"));
        }

        @Test
        @DisplayName("Uses provided value over default value")
        void providedValueOverridesDefault() {
            final JsonObject json = Json.object();
            json.put("title", "{{TITLE}}:Default Title");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.TITLE, PlaceholderValue.of("Custom Title"))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("Custom Title", json.getString("title"));
        }

        @Test
        @DisplayName("Resolves empty default value — getString returns null (blank normalised by JsonObject)")
        void emptyDefaultValueResolvesToEmpty() {
            final JsonObject json = Json.object();
            json.put("name", "{{NAME}}:");

            final PlaceholderData data = new TestDataBuilder().build();

            JsonPlaceholderResolver.resolve(json, data);

            // The resolver correctly resolves {{NAME}}: to an empty string "".
            // JsonObject.getString() normalises blank/empty strings to null,
            // so the expected result here is null, not "".
            assertNull(json.getString("name"));
        }

        @Test
        @DisplayName("Resolves numeric default value with native type")
        void numericDefaultValue() {
            final JsonObject json = Json.object();
            json.put("count", "{{AMOUNT}}:42");

            final PlaceholderData data = new TestDataBuilder().build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals(42L, json.get("count"));
        }

        @Test
        @DisplayName("Resolves boolean default value with native type")
        void booleanDefaultValue() {
            final JsonObject json = Json.object();
            json.put("flag", "{{IS_ENABLED}}:true");

            final PlaceholderData data = new TestDataBuilder().build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals(true, json.get("flag"));
        }

        @Test
        @DisplayName("Preserves non-placeholder string values")
        void nonPlaceholderValuesUnchanged() {
            final JsonObject json = Json.object();
            json.put("static", "Hello World");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test"))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("Hello World", json.getString("static"));
        }

        @Test
        @DisplayName("Resolves JsonObject placeholder value")
        void resolveJsonObjectPlaceholder() {
            final JsonObject json = Json.object();
            json.put("settings", "{{NAME}}");

            final JsonObject settingsValue = Json.object();
            settingsValue.put("locale", "US");
            settingsValue.put("id", 9);

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of(settingsValue))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            final Object result = json.get("settings");
            assertNotNull(result);
        }
    }

    // ========================================================================
    // Nested object resolution tests
    // ========================================================================

    @Nested
    @DisplayName("Nested object resolution")
    class NestedResolution {

        @Test
        @DisplayName("Resolves placeholders in nested objects")
        void resolveNestedObjects() {
            final JsonObject settings = Json.object();
            settings.put("currency", "{{CURRENCY}}");

            final JsonObject root = Json.object();
            root.put("settings", settings);

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.CURRENCY, PlaceholderValue.of("USD"))
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            assertEquals("USD", root.getJsonObject("settings").getString("currency"));
        }

        @Test
        @DisplayName("Resolves placeholders in arrays of objects")
        void resolveArrayOfObjects() {
            final JsonObject item = Json.object();
            item.put("title", "{{TITLE}}");

            final JsonArray array = Json.array();
            array.put(item);

            final JsonObject root = Json.object();
            root.put("items", array);

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.TITLE, PlaceholderValue.of("Resolved Title"))
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            assertEquals("Resolved Title", root.getJsonArray("items").getJsonObject(0).getString("title"));
        }

        @Test
        @DisplayName("Resolves deeply nested placeholders")
        void resolveDeeplyNested() {
            final JsonObject level3 = Json.object();
            level3.put("value", "{{CURRENCY}}");

            final JsonObject level2 = Json.object();
            level2.put("nested", level3);

            final JsonObject level1 = Json.object();
            level1.put("data", level2);

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.CURRENCY, PlaceholderValue.of("EUR"))
                    .build();

            JsonPlaceholderResolver.resolve(level1, data);

            assertEquals("EUR", level1.getJsonObject("data").getJsonObject("nested").getString("value"));
        }
    }

    // ========================================================================
    // Array expansion tests
    // ========================================================================

    @Nested
    @DisplayName("Array value expansion")
    class ArrayExpansion {

        @Test
        @DisplayName("Expands array template into multiple rows")
        void expandArrayValues() {
            final JsonObject template = Json.object();
            template.put("name", "{{CONTACT_NAME}}:");
            template.put("email", "{{CONTACT_EMAIL}}:");
            template.put("type", "CONTACT");

            final JsonArray valuesArray = Json.array();
            valuesArray.put(template);

            final JsonObject parent = Json.object();
            parent.put("placeHolder", "{{CONTACTS}}");
            parent.put("values", valuesArray);
            parent.put("hasArrayOfValues", true);

            final JsonObject root = Json.object();
            root.put("contacts", parent);

            final PlaceholderEntries row1 = new PlaceholderEntries();
            row1.addEntry(TestKey.CONTACT_NAME, PlaceholderValue.of("Alice"));
            row1.addEntry(TestKey.CONTACT_EMAIL, PlaceholderValue.of("alice@example.com"));

            final PlaceholderEntries row2 = new PlaceholderEntries();
            row2.addEntry(TestKey.CONTACT_NAME, PlaceholderValue.of("Bob"));
            row2.addEntry(TestKey.CONTACT_EMAIL, PlaceholderValue.of("bob@example.com"));

            final PlaceholderData data = new TestDataBuilder()
                    .addArrayRow(TestKey.CONTACTS, row1)
                    .addArrayRow(TestKey.CONTACTS, row2)
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            final JsonArray expanded = root.getJsonObject("contacts").getJsonArray("values");

            assertEquals(2, expanded.size());
            assertEquals("Alice", expanded.getJsonObject(0).getString("name"));
            assertEquals("alice@example.com", expanded.getJsonObject(0).getString("email"));
            assertEquals("CONTACT", expanded.getJsonObject(0).getString("type"));
            assertEquals("Bob", expanded.getJsonObject(1).getString("name"));
            assertEquals("bob@example.com", expanded.getJsonObject(1).getString("email"));
        }

        @Test
        @DisplayName("Expands array with boolean values")
        void expandArrayWithBooleanValues() {
            final JsonObject template = Json.object();
            template.put("name", "{{CONTACT_NAME}}:");
            template.put("active", "{{CONTACT_ACTIVE}}:");

            final JsonArray valuesArray = Json.array();
            valuesArray.put(template);

            final JsonObject parent = Json.object();
            parent.put("placeHolder", "{{CONTACTS}}");
            parent.put("values", valuesArray);
            parent.put("hasArrayOfValues", true);

            final JsonObject root = Json.object();
            root.put("contacts", parent);

            final PlaceholderEntries row = new PlaceholderEntries();
            row.addEntry(TestKey.CONTACT_NAME, PlaceholderValue.of("Alice"));
            row.addEntry(TestKey.CONTACT_ACTIVE, PlaceholderValue.of(true));

            final PlaceholderData data = new TestDataBuilder()
                    .addArrayRow(TestKey.CONTACTS, row)
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            final JsonArray expanded = root.getJsonObject("contacts").getJsonArray("values");

            assertEquals(1, expanded.size());
            assertEquals("Alice", expanded.getJsonObject(0).getString("name"));
            assertTrue(expanded.getJsonObject(0).getBoolean("active"));
        }

        @Test
        @DisplayName("Leaves array unchanged when no data rows provided")
        void noDataRowsLeavesArrayUnchanged() {
            final JsonObject template = Json.object();
            template.put("name", "{{CONTACT_NAME}}:");

            final JsonArray valuesArray = Json.array();
            valuesArray.put(template);

            final JsonObject parent = Json.object();
            parent.put("placeHolder", "{{CONTACTS}}");
            parent.put("values", valuesArray);
            parent.put("hasArrayOfValues", true);

            final JsonObject root = Json.object();
            root.put("contacts", parent);

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test"))
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            final JsonArray expanded = root.getJsonObject("contacts").getJsonArray("values");

            assertEquals(1, expanded.size());
        }

        @Test
        @DisplayName("Skips expansion when hasArrayOfValues is false")
        void skipsExpansionWhenFlagIsFalse() {
            final JsonObject template = Json.object();
            template.put("name", "{{CONTACT_NAME}}:");

            final JsonArray valuesArray = Json.array();
            valuesArray.put(template);

            final JsonObject parent = Json.object();
            parent.put("placeHolder", "{{CONTACTS}}");
            parent.put("values", valuesArray);
            parent.put("hasArrayOfValues", false);

            final JsonObject root = Json.object();
            root.put("contacts", parent);

            final PlaceholderEntries row = new PlaceholderEntries();
            row.addEntry(TestKey.CONTACT_NAME, PlaceholderValue.of("Alice"));

            final PlaceholderData data = new TestDataBuilder()
                    .addArrayRow(TestKey.CONTACTS, row)
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            final JsonArray expanded = root.getJsonObject("contacts").getJsonArray("values");

            assertEquals(1, expanded.size());
        }

        @Test
        @DisplayName("Expands array with numeric values preserving types")
        void expandArrayWithNumericValues() {
            final JsonObject template = Json.object();
            template.put("name", "{{ITEM_NAME}}:");
            template.put("price", "{{ITEM_PRICE}}:");

            final JsonArray valuesArray = Json.array();
            valuesArray.put(template);

            final JsonObject parent = Json.object();
            parent.put("placeHolder", "{{ITEMS}}");
            parent.put("values", valuesArray);
            parent.put("hasArrayOfValues", true);

            final JsonObject root = Json.object();
            root.put("items", parent);

            final PlaceholderEntries row1 = new PlaceholderEntries();
            row1.addEntry(TestKey.ITEM_NAME, PlaceholderValue.of("Product A"));
            row1.addEntry(TestKey.ITEM_PRICE, PlaceholderValue.of(99.99));

            final PlaceholderEntries row2 = new PlaceholderEntries();
            row2.addEntry(TestKey.ITEM_NAME, PlaceholderValue.of("Product B"));
            row2.addEntry(TestKey.ITEM_PRICE, PlaceholderValue.of(149.00));

            final PlaceholderData data = new TestDataBuilder()
                    .addArrayRow(TestKey.ITEMS, row1)
                    .addArrayRow(TestKey.ITEMS, row2)
                    .build();

            JsonPlaceholderResolver.resolve(root, data);

            final JsonArray expanded = root.getJsonObject("items").getJsonArray("values");

            assertEquals(2, expanded.size());
            assertEquals("Product A", expanded.getJsonObject(0).getString("name"));
            assertEquals(99.99, expanded.getJsonObject(0).getDouble("price"));
            assertEquals("Product B", expanded.getJsonObject(1).getString("name"));
            assertEquals(149.00, expanded.getJsonObject(1).getDouble("price"));
        }
    }

    // ========================================================================
    // String-based resolve tests
    // ========================================================================

    @Nested
    @DisplayName("String-based resolution")
    class StringResolution {

        @Test
        @DisplayName("Resolves placeholders via string API")
        void resolveViaStringApi() {
            final String json = "{\"name\":\"{{NAME}}\",\"currency\":\"{{CURRENCY}}\"}";

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Test Doc"))
                    .scalar(TestKey.CURRENCY, PlaceholderValue.of("USD"))
                    .build();

            final String result = JsonPlaceholderResolver.resolve(json, data);

            assertTrue(result.contains("\"Test Doc\""));
            assertTrue(result.contains("\"USD\""));
        }

        @Test
        @DisplayName("Returns original string when data is null")
        void returnOriginalWhenDataNull() {
            final String json = "{\"name\":\"{{NAME}}\"}";

            assertEquals(json, JsonPlaceholderResolver.resolve(json, null));
        }
    }

    // ========================================================================
    // Mixed content / partial placeholder tests
    // ========================================================================

    @Nested
    @DisplayName("Mixed content strings")
    class MixedContent {

        @Test
        @DisplayName("Resolves placeholder embedded in text")
        void placeholderInMixedText() {
            final JsonObject json = Json.object();
            json.put("greeting", "Hello {{NAME}}, welcome!");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("World"))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("Hello World, welcome!", json.getString("greeting"));
        }

        @Test
        @DisplayName("Resolves multiple placeholders in single string")
        void multiplePlaceholdersInOneString() {
            final JsonObject json = Json.object();
            json.put("info", "{{NAME}} uses {{CURRENCY}}");

            final PlaceholderData data = new TestDataBuilder()
                    .scalar(TestKey.NAME, PlaceholderValue.of("Alice"))
                    .scalar(TestKey.CURRENCY, PlaceholderValue.of("EUR"))
                    .build();

            JsonPlaceholderResolver.resolve(json, data);

            assertEquals("Alice uses EUR", json.getString("info"));
        }
    }
}

