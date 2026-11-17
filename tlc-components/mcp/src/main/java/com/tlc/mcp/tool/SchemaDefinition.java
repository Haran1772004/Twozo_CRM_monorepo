package com.tlc.mcp.tool;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SchemaDefinition {

    private final List<FieldDef> fields;

    private SchemaDefinition(List<FieldDef> fields) {
        this.fields = List.copyOf(fields);
    }

    public static SchemaDefinition empty() {
        return new SchemaDefinition(List.of());
    }

    public static SchemaDefinition of(List<FieldDef> fields) {
        return new SchemaDefinition(fields);
    }

    public static SchemaDefinition fromJsonObject(JsonObject schema) {
        final JsonObject propsJson = schema.optJsonObject("properties");
        if (propsJson == null) {
            return SchemaDefinition.empty();
        }

        final Set<String> requiredSet = new HashSet<>();
        final JsonArray reqArray = schema.optJsonArray("required");
        if (reqArray != null) {
            reqArray.toList().forEach(item -> requiredSet.add(String.valueOf(item)));
        }

        final List<FieldDef> fields = new ArrayList<>();
        for (String name : propsJson.keySet()) {
            final JsonObject propDef = propsJson.getJsonObject(name);
            fields.add(buildFieldFromJson(name, propDef, requiredSet.contains(name)));
        }
        return SchemaDefinition.of(fields);
    }

    private static FieldDef buildFieldFromJson(String name, JsonObject prop, boolean required) {
        final FieldDef.Builder b = FieldDef.builder(name);
        b.required(required);

        // ── Handle oneOf union type ──
        final JsonArray oneOfArr = prop.optJsonArray("oneOf");
        if (oneOfArr != null) {
            final List<FieldDef> variants = new ArrayList<>();
            for (int i = 0; i < oneOfArr.size(); i++) {
                final JsonObject variantJson = oneOfArr.getJsonObject(i);
                variants.add(buildFieldFromJson(name + "_variant_" + i, variantJson, false));
            }
            b.oneOf(variants);
            final Object descObj = prop.opt("description");
            if (descObj != null) b.description(String.valueOf(descObj));
            return b.build();
        }

        final Object typeObj = prop.opt("type");
        final String type = typeObj != null ? String.valueOf(typeObj) : "string";

        final Object descObj = prop.opt("description");
        if (descObj != null) b.description(String.valueOf(descObj));

        final Object defaultObj = prop.opt("default");
        if (defaultObj != null) b.defaultValue(String.valueOf(defaultObj));

        final Object minObj = prop.opt("minimum");
        if (minObj != null) b.min(String.valueOf(minObj));

        final Object maxObj = prop.opt("maximum");
        if (maxObj != null) b.max(String.valueOf(maxObj));

        final Object minLenObj = prop.opt("minLength");
        if (minLenObj instanceof Number n) b.minLength(n.intValue());

        final Object maxLenObj = prop.opt("maxLength");
        if (maxLenObj instanceof Number n) b.maxLength(n.intValue());

        final Object patternObj = prop.opt("pattern");
        if (patternObj != null) b.pattern(String.valueOf(patternObj));

        if ("object".equals(type)) {
            b.type("object");
            b.additionalProperties(prop.optBoolean("additionalProperties"));
            final JsonObject nestedPropsJson = prop.optJsonObject("properties");
            if (nestedPropsJson != null) {
                final Set<String> nestedRequired = new HashSet<>();
                final JsonArray nestedReqArr = prop.optJsonArray("required");
                if (nestedReqArr != null) {
                    nestedReqArr.toList().forEach(item -> nestedRequired.add(String.valueOf(item)));
                }
                final List<FieldDef> nested = new ArrayList<>();
                for (String nName : nestedPropsJson.keySet()) {
                    nested.add(buildFieldFromJson(nName, nestedPropsJson.getJsonObject(nName),
                            nestedRequired.contains(nName)));
                }
                b.nestedFields(nested);
            }
        } else if ("array".equals(type)) {
            b.type("array");
            final boolean uniqueItems = prop.optBoolean("uniqueItems");
            b.multiSelect(uniqueItems);
            final JsonObject itemsJson = prop.optJsonObject("items");
            if (itemsJson != null) {
                final Object itemTypeObj = itemsJson.opt("type");
                if (itemTypeObj != null) b.itemType(String.valueOf(itemTypeObj));
                final JsonArray itemEnumArr = itemsJson.optJsonArray("enum");
                if (itemEnumArr != null) {
                    final List<String> opts = new ArrayList<>();
                    itemEnumArr.toList().forEach(v -> opts.add(String.valueOf(v)));
                    b.options(opts);
                }
            }
        } else {
            b.type(type);
            final JsonArray enumArr = prop.optJsonArray("enum");
            if (enumArr != null) {
                final List<String> opts = new ArrayList<>();
                enumArr.toList().forEach(v -> opts.add(String.valueOf(v)));
                b.options(opts);
            }
        }

        return b.build();
    }

    public List<FieldDef> fields() {
        return fields;
    }

    public Optional<FieldDef> field(String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    public SchemaDefinition withField(FieldDef field) {
        List<FieldDef> updated = new ArrayList<>(fields);
        updated.removeIf(f -> f.name().equals(field.name()));
        updated.add(field);
        return new SchemaDefinition(updated);
    }

    public SchemaDefinition withoutField(String name) {
        List<FieldDef> updated = new ArrayList<>(fields);
        updated.removeIf(f -> f.name().equals(name));
        return new SchemaDefinition(updated);
    }

    public JsonObject toJsonSchema() {
        final JsonObject schema = Json.orderedObject();
        schema.put("type", "object");

        final JsonObject properties = Json.orderedObject();
        final List<String> required = new ArrayList<>();

        for (FieldDef f : fields) {
            properties.put(f.name(), fieldToJsonSchema(f));
            if (f.required() && (f.defaultValue() == null || f.defaultValue().isEmpty())) {
                required.add(f.name());
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            final JsonArray reqArray = Json.array();
            required.forEach(reqArray::put);
            schema.put("required", reqArray);
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    private JsonObject fieldToJsonSchema(FieldDef f) {
        final JsonObject prop = Json.orderedObject();

        // ── Handle oneOf union type ──
        if (f.oneOf() != null && !f.oneOf().isEmpty()) {
            final JsonArray oneOfArray = Json.array();
            for (FieldDef variant : f.oneOf()) {
                oneOfArray.put(fieldToJsonSchema(variant));
            }
            prop.put("oneOf", oneOfArray);
            if (f.description() != null && !f.description().isEmpty()) {
                prop.put("description", f.description());
            }
            return prop;
        }

        if ("array".equals(f.fieldType()) && f.nestedFields() != null && !f.nestedFields().isEmpty()) {
            prop.put("type", "array");
            prop.put("items", buildNestedObjectSchema(f));
        } else if (f.nestedFields() != null && !f.nestedFields().isEmpty()) {
            prop.put("type", "object");
            final JsonObject nestedObjectSchema = buildNestedObjectSchema(f);
            prop.put("properties", nestedObjectSchema.getJsonObject("properties"));

            final JsonArray nestedRequired = nestedObjectSchema.optJsonArray("required");
            if (nestedRequired != null) {
                prop.put("required", nestedRequired);
            }

            prop.put("additionalProperties", f.additionalProperties());
        } else if (f.multiSelect()) {
            prop.put("type", "array");
            final JsonObject items = Json.orderedObject();
            if (f.options() != null && !f.options().isEmpty()) {
                items.put("type", "string");
                final JsonArray enumArr = Json.array();
                f.options().forEach(enumArr::put);
                items.put("enum", enumArr);
            } else {
                items.put("type", f.itemType() != null ? f.itemType() : "string");
            }
            prop.put("items", items);
            prop.put("uniqueItems", true);
        } else if ("array".equals(f.fieldType())) {
            prop.put("type", "array");
            if (f.itemType() != null) {
                final JsonObject items = Json.orderedObject();
                items.put("type", f.itemType());
                prop.put("items", items);
            }
        } else {
            prop.put("type", resolveJsonType(f.fieldType()));
            if (f.options() != null && !f.options().isEmpty()) {
                final JsonArray enumArr = Json.array();
                f.options().forEach(enumArr::put);
                prop.put("enum", enumArr);
            }
        }

        if (f.description() != null && !f.description().isEmpty()) {
            prop.put("description", f.description());
        }
        if (f.defaultValue() != null && !f.defaultValue().isEmpty()) {
            prop.put("default", f.defaultValue());
        }
        if (f.min() != null && !f.min().isEmpty()) {
            prop.put("minimum", parseNumber(f.min()));
        }
        if (f.max() != null && !f.max().isEmpty()) {
            prop.put("maximum", parseNumber(f.max()));
        }
        if (f.minLength() >= 0) {
            prop.put("minLength", f.minLength());
        }
        if (f.maxLength() >= 0) {
            prop.put("maxLength", f.maxLength());
        }
        if (f.pattern() != null && !f.pattern().isEmpty()) {
            prop.put("pattern", f.pattern());
        }
        return prop;
    }

    /**
     * Builds the JSON schema for a nested object field based on the given parent field definition.
     *
     * @param parent the parent field definition containing the nested field details
     * @return a JSON object representing the schema for the nested object field
     */
    private JsonObject buildNestedObjectSchema(final FieldDef parent) {
        final JsonObject objectSchema = Json.orderedObject();

        objectSchema.put("type", "object");

        final JsonObject nested = Json.orderedObject();
        final List<String> nestedRequired = new ArrayList<>();

        for (FieldDef nf : parent.nestedFields()) {
            nested.put(nf.name(), fieldToJsonSchema(nf));

            if (nf.required() && (nf.defaultValue() == null || nf.defaultValue().isEmpty())) {
                nestedRequired.add(nf.name());
            }
        }

        objectSchema.put("properties", nested);

        if (!nestedRequired.isEmpty()) {
            final JsonArray nestedReqArray = Json.array();
            nestedRequired.forEach(nestedReqArray::put);
            objectSchema.put("required", nestedReqArray);
        }
        objectSchema.put("additionalProperties", parent.additionalProperties());

        return objectSchema;
    }

    private String resolveJsonType(String javaType) {
        if (javaType == null) return "string";
        return switch (javaType) {
            case "int", "long", "short", "byte",
                 "java.lang.Integer", "java.lang.Long",
                 "java.lang.Short", "java.lang.Byte" -> "integer";
            case "float", "double",
                 "java.lang.Float", "java.lang.Double" -> "number";
            case "boolean", "java.lang.Boolean" -> "boolean";
            case "integer", "number", "object", "array" -> javaType;
            default -> "string";
        };
    }

    private Number parseNumber(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return Double.parseDouble(s);
        }
    }

    public record FieldDef(
            String name,
            String fieldType,
            String description,
            boolean required,
            String defaultValue,
            String min,
            String max,
            int minLength,
            int maxLength,
            String pattern,
            List<String> options,
            boolean multiSelect,
            List<FieldDef> nestedFields,
            String itemType,
            boolean additionalProperties,
            List<FieldDef> oneOf
    ) {
        public static Builder builder(String name) {
            return new Builder(name);
        }

        public static final class Builder {
            private final String name;
            private String fieldType = "string";
            private String description = "";
            private boolean required = true;
            private String defaultValue = "";
            private String min = "";
            private String max = "";
            private int minLength = -1;
            private int maxLength = -1;
            private String pattern = "";
            private List<String> options = List.of();
            private boolean multiSelect = false;
            private List<FieldDef> nestedFields = List.of();
            private String itemType = null;
            private boolean additionalProperties = false;
            private List<FieldDef> oneOf = List.of();

            private Builder(String name) {
                this.name = name;
            }

            public Builder type(String type)              { this.fieldType    = type; return this; }
            public Builder description(String d)          { this.description  = d;    return this; }
            public Builder required(boolean r)            { this.required     = r;    return this; }
            public Builder defaultValue(String d)         { this.defaultValue = d;    return this; }
            public Builder min(String m)                  { this.min          = m;    return this; }
            public Builder max(String m)                  { this.max          = m;    return this; }
            public Builder minLength(int m)               { this.minLength    = m;    return this; }
            public Builder maxLength(int m)               { this.maxLength    = m;    return this; }
            public Builder pattern(String p)              { this.pattern      = p;    return this; }
            public Builder options(List<String> o)        { this.options      = o;    return this; }
            public Builder multiSelect(boolean m)         { this.multiSelect  = m;    return this; }
            public Builder nestedFields(List<FieldDef> n) { this.nestedFields = n;   return this; }
            public Builder itemType(String it)            { this.itemType     = it;   return this; }
            public Builder additionalProperties(boolean a) { this.additionalProperties = a; return this; }
            public Builder oneOf(List<FieldDef> variants) { this.oneOf        = variants; return this; }

            public FieldDef build() {
                return new FieldDef(name, fieldType, description, required, defaultValue,
                        min, max, minLength, maxLength, pattern, options, multiSelect,
                        nestedFields, itemType, additionalProperties, oneOf);
            }
        }
    }
}
