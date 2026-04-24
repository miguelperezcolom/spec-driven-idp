package com.acme.generator.util;

import java.util.Map;
import java.util.Set;

/**
 * Maps DSL type expressions to Java type names.
 */
public final class TypeMapper {

    private static final Map<String, String> SCALAR_MAP = Map.of(
            "string",  "String",
            "integer", "Integer",
            "long",    "Long",
            "decimal", "java.math.BigDecimal",
            "boolean", "Boolean",
            "date",    "java.time.LocalDate",
            "instant", "java.time.Instant",
            "uuid",    "java.util.UUID"
    );

    private static final Set<String> NEEDS_IMPORT = Set.of(
            "java.math.BigDecimal",
            "java.time.LocalDate",
            "java.time.Instant",
            "java.util.UUID",
            "java.util.List"
    );

    private TypeMapper() {}

    /**
     * Converts a DSL type expression (e.g. "string", "list", "Money", "list<Booking>") to Java.
     *
     * @param dslType  the raw type string from the DSL
     * @param itemType the item type if dslType == "list"
     * @param domainPackage base domain package (for custom types)
     */
    public static String toJava(String dslType, String itemType, String domainPackage) {
        if (dslType == null) return "Object";

        // list<X> or list with itemType
        if ("list".equals(dslType)) {
            String item = itemType != null ? itemType : "Object";
            return "java.util.List<" + toJava(item, null, domainPackage) + ">";
        }

        // generic syntax: SomeType<Param>
        if (dslType.contains("<")) {
            return dslType; // pass through generics as-is
        }

        // scalar
        String scalar = SCALAR_MAP.get(dslType);
        if (scalar != null) return scalar;

        // custom type (assumed to be in domain package)
        return dslType;
    }

    /** Simple version without itemType. */
    public static String toJava(String dslType, String domainPackage) {
        return toJava(dslType, null, domainPackage);
    }

    /** Converts to Java primitive (unboxed) where applicable. */
    public static String toPrimitive(String javaType) {
        return switch (javaType) {
            case "Integer" -> "int";
            case "Long"    -> "long";
            case "Boolean" -> "boolean";
            default        -> javaType;
        };
    }

    /** Returns true if the type requires an import statement. */
    public static boolean needsImport(String javaType) {
        return NEEDS_IMPORT.contains(javaType) || javaType.startsWith("java.util.List<");
    }

    /** Capitalises the first letter of a string. */
    public static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Converts a camelCase or PascalCase name to UPPER_SNAKE_CASE. */
    public static String toUpperSnake(String s) {
        return s.replaceAll("([A-Z])", "_$1").toUpperCase().replaceAll("^_", "");
    }
}
