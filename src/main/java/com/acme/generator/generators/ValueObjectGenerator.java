package com.acme.generator.generators;

import com.acme.generator.model.Models.*;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Generates Value Objects as Java records.
 *
 * <p>Strategy (abstract class + extension pattern):
 * <ul>
 *   <li>{@code generated/AbstractXxx.java} — generated abstract record (always overwritten)</li>
 *   <li>{@code Xxx.java} — manual extension stub (only created if absent)</li>
 * </ul>
 */
public class ValueObjectGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var vos = ctx.spec().valueObjects();
        if (vos == null || vos.isEmpty()) return;

        System.out.println("[ValueObjectGenerator] Generating " + vos.size() + " value object(s)...");

        for (var entry : vos.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            ValueObjectSpec spec = entry.getValue();

            if ("object".equals(spec.type())) {
                generateObjectVO(ctx, name, spec);
            } else {
                generateScalarVO(ctx, name, spec);
            }
        }
    }

    // ── Object VO (with fields) ───────────────────────────────────────────────

    private void generateObjectVO(GeneratorContext ctx, String name, ValueObjectSpec spec) throws Exception {
        String genPkg = ctx.generatedPackage() + ".domain.vo";
        String manPkg = ctx.manualPackage() + ".domain.vo";
        String basePkg = ctx.basePackage();

        // Generate abstract sealed record
        String abstractName = "Abstract" + name;
        String generated = buildObjectVO(abstractName, genPkg, name, manPkg, spec, basePkg);
        writer.writeGenerated(ctx.generatedDir().resolve("domain/vo"), abstractName, generated);

        // Manual stub extending the abstract
        String manual = buildManualVO(name, manPkg, abstractName, genPkg, spec);
        writer.writeManualStub(ctx.manualDir().resolve("domain/vo"), name, manual);
    }

    private String buildObjectVO(String abstractName, String genPkg, String concreteName,
                                  String manPkg, ValueObjectSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(genPkg).append(";\n\n");
        sb.append("// AUTO-GENERATED — do not edit. Extend ").append(concreteName).append(" instead.\n\n");

        if (spec.description() != null) {
            sb.append("/** ").append(spec.description()).append(" */\n");
        }

        sb.append("public abstract class ").append(abstractName).append(" {\n\n");

        // Fields as final private
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg);
                sb.append("    private final ").append(javaType).append(" ").append(f.getKey()).append(";\n");
            }
            sb.append("\n");

            // Constructor
            sb.append("    protected ").append(abstractName).append("(");
            var params = new StringJoiner(", ");
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg);
                params.add(javaType + " " + f.getKey());
            }
            sb.append(params).append(") {\n");

            // Constraint validations
            if (spec.fields() != null) {
                for (var f : spec.fields().entrySet()) {
                    appendFieldValidations(sb, f.getKey(), f.getValue());
                }
            }

            for (var f : spec.fields().entrySet()) {
                sb.append("        this.").append(f.getKey()).append(" = ").append(f.getKey()).append(";\n");
            }
            sb.append("    }\n\n");

            // Getters
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg);
                sb.append("    public ").append(javaType).append(" ").append(f.getKey()).append("() { return ")
                  .append(f.getKey()).append("; }\n");
            }
            sb.append("\n");
        }

        // Invariants
        appendInvariants(sb, spec.invariants());

        sb.append("}\n");
        return sb.toString();
    }

    private String buildManualVO(String name, String manPkg, String abstractName, String genPkg, ValueObjectSpec spec) {
        var sb = new StringBuilder();
        sb.append("package ").append(manPkg).append(";\n\n");
        sb.append("import ").append(genPkg).append(".").append(abstractName).append(";\n\n");

        if (spec.description() != null) {
            sb.append("/** ").append(spec.description()).append(" */\n");
        }
        sb.append("public final class ").append(name).append(" extends ").append(abstractName).append(" {\n\n");

        // Constructor forwarding
        if (spec.fields() != null && !spec.fields().isEmpty()) {
            sb.append("    public ").append(name).append("(");
            var params = new StringJoiner(", ");
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), "");
                params.add(javaType + " " + f.getKey());
            }
            sb.append(params).append(") {\n");
            sb.append("        super(");
            var args = new StringJoiner(", ");
            spec.fields().keySet().forEach(args::add);
            sb.append(args).append(");\n");
            sb.append("    }\n\n");
        }

        sb.append("    // Add domain behaviour here\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ── Scalar VO ─────────────────────────────────────────────────────────────

    private void generateScalarVO(GeneratorContext ctx, String name, ValueObjectSpec spec) throws Exception {
        String genPkg = ctx.generatedPackage() + ".domain.vo";
        String manPkg = ctx.manualPackage() + ".domain.vo";
        String basePkg = ctx.basePackage();
        String javaType = TypeMapper.toJava(spec.type(), basePkg);

        String abstractName = "Abstract" + name;
        String generated = buildScalarAbstract(abstractName, genPkg, name, spec, javaType);
        writer.writeGenerated(ctx.generatedDir().resolve("domain/vo"), abstractName, generated);

        String manual = buildScalarManual(name, manPkg, abstractName, genPkg, javaType);
        writer.writeManualStub(ctx.manualDir().resolve("domain/vo"), name, manual);
    }

    private String buildScalarAbstract(String abstractName, String pkg, String concreteName,
                                        ValueObjectSpec spec, String javaType) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// AUTO-GENERATED — do not edit. Extend ").append(concreteName).append(" instead.\n\n");

        if (spec.description() != null) sb.append("/** ").append(spec.description()).append(" */\n");

        sb.append("public abstract class ").append(abstractName).append(" {\n\n");
        sb.append("    private final ").append(javaType).append(" value;\n\n");
        sb.append("    protected ").append(abstractName).append("(").append(javaType).append(" value) {\n");

        // Basic null check
        if (!javaType.equals("Boolean") && !javaType.equals("Integer") && !javaType.equals("Long")) {
            sb.append("        if (value == null) throw new IllegalArgumentException(\"")
              .append(concreteName).append(" value must not be null\");\n");
        }

        // Constraint validation from spec
        if (spec.constraints() != null && !spec.constraints().isEmpty()) {
            appendConstraintValidations(sb, spec.constraints().get(0), "value", javaType);
        }

        sb.append("        this.value = value;\n");
        sb.append("    }\n\n");
        sb.append("    public ").append(javaType).append(" value() { return value; }\n\n");

        sb.append("    @Override\n");
        sb.append("    public boolean equals(Object o) {\n");
        sb.append("        if (this == o) return true;\n");
        sb.append("        if (!(o instanceof ").append(abstractName).append(" that)) return false;\n");
        sb.append("        return java.util.Objects.equals(value, that.value);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public int hashCode() { return java.util.Objects.hash(value); }\n\n");

        sb.append("    @Override\n");
        sb.append("    public String toString() { return getClass().getSimpleName() + \"(\" + value + \")\"; }\n");

        appendInvariants(sb, spec.invariants());
        sb.append("}\n");
        return sb.toString();
    }

    private String buildScalarManual(String name, String manPkg, String abstractName,
                                      String genPkg, String javaType) {
        var sb = new StringBuilder();
        sb.append("package ").append(manPkg).append(";\n\n");
        sb.append("import ").append(genPkg).append(".").append(abstractName).append(";\n\n");
        sb.append("public final class ").append(name).append(" extends ").append(abstractName).append(" {\n\n");
        sb.append("    public ").append(name).append("(").append(javaType).append(" value) {\n");
        sb.append("        super(value);\n");
        sb.append("    }\n\n");
        sb.append("    // Add domain behaviour here\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendFieldValidations(StringBuilder sb, String fieldName, FieldSpec field) {
        if (field.constraints() == null) return;
        appendConstraintValidations(sb, field.constraints(), fieldName,
                TypeMapper.toJava(field.type(), field.itemType(), ""));
    }

    private void appendConstraintValidations(StringBuilder sb, ConstraintSpec c, String varName, String javaType) {
        if (c == null) return;

        if (Boolean.TRUE.equals(c.required())) {
            sb.append("        if (").append(varName).append(" == null) throw new IllegalArgumentException(\"'")
              .append(varName).append("' is required\");\n");
        }
        if (c.minLength() != null) {
            sb.append("        if (").append(varName).append(" != null && ").append(varName)
              .append(".length() < ").append(c.minLength())
              .append(") throw new IllegalArgumentException(\"'").append(varName)
              .append("' must be at least ").append(c.minLength()).append(" characters\");\n");
        }
        if (c.maxLength() != null) {
            sb.append("        if (").append(varName).append(" != null && ").append(varName)
              .append(".length() > ").append(c.maxLength())
              .append(") throw new IllegalArgumentException(\"'").append(varName)
              .append("' must be at most ").append(c.maxLength()).append(" characters\");\n");
        }
        if (c.min() != null) {
            sb.append("        if (").append(varName).append(" != null && ((Number)").append(varName)
              .append(").doubleValue() < ").append(c.min())
              .append(") throw new IllegalArgumentException(\"'").append(varName)
              .append("' must be >= ").append(c.min()).append("\");\n");
        }
        if (c.max() != null) {
            sb.append("        if (").append(varName).append(" != null && ((Number)").append(varName)
              .append(").doubleValue() > ").append(c.max())
              .append(") throw new IllegalArgumentException(\"'").append(varName)
              .append("' must be <= ").append(c.max()).append("\");\n");
        }
        if (c.pattern() != null) {
            sb.append("        if (").append(varName).append(" != null && !").append(varName)
              .append(".matches(\"").append(c.pattern().replace("\\", "\\\\"))
              .append("\")) throw new IllegalArgumentException(\"'").append(varName)
              .append("' does not match required pattern\");\n");
        }
    }

    private void appendInvariants(StringBuilder sb, java.util.List<InvariantSpec> invariants) {
        if (invariants == null || invariants.isEmpty()) return;
        sb.append("\n    // ── Invariants ──────────────────────────────────────────────────────────\n");
        for (var inv : invariants) {
            sb.append("\n    /**\n");
            if (inv.when() != null) sb.append("     * When: ").append(inv.when()).append("\n");
            sb.append("     * Expression: ").append(inv.expression()).append("\n");
            if (inv.message() != null) sb.append("     * Message: ").append(inv.message()).append("\n");
            sb.append("     */\n");
            sb.append("    // TODO: enforce invariant '").append(inv.name()).append("'\n");
        }
        sb.append("\n");
    }
}
