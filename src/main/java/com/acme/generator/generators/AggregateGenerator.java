package com.acme.generator.generators;

import com.acme.generator.model.Models.*;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Generates aggregate root classes.
 *
 * <p>Pattern:
 * <ul>
 *   <li>{@code generated/AbstractXxx.java} — generated abstract base (always overwritten)</li>
 *   <li>{@code Xxx.java} — manual extension stub (only created if absent)</li>
 * </ul>
 */
public class AggregateGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var aggregates = ctx.spec().aggregates();
        if (aggregates == null || aggregates.isEmpty()) return;

        System.out.println("[AggregateGenerator] Generating " + aggregates.size() + " aggregate(s)...");

        for (var entry : aggregates.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            AggregateSpec spec = entry.getValue();
            generateAggregate(ctx, name, spec);
        }
    }

    private void generateAggregate(GeneratorContext ctx, String name, AggregateSpec spec) throws Exception {
        String genPkg = ctx.generatedPackage() + ".domain.aggregate";
        String manPkg = ctx.manualPackage() + ".domain.aggregate";
        String basePkg = ctx.basePackage();

        String abstractName = "Abstract" + name;
        String generated = buildAbstractAggregate(abstractName, genPkg, name, manPkg, spec, basePkg, ctx);
        writer.writeGenerated(ctx.generatedDir().resolve("domain/aggregate"), abstractName, generated);

        String manual = buildManualAggregate(name, manPkg, abstractName, genPkg, spec, basePkg);
        writer.writeManualStub(ctx.manualDir().resolve("domain/aggregate"), name, manual);
    }

    // ── Abstract aggregate ────────────────────────────────────────────────────

    private String buildAbstractAggregate(String abstractName, String pkg, String concreteName,
                                           String manPkg, AggregateSpec spec, String basePkg,
                                           GeneratorContext ctx) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import java.util.ArrayList;\n");
        sb.append("import java.util.Collections;\n");
        sb.append("import java.util.List;\n\n");
        sb.append("// AUTO-GENERATED — do not edit. Extend ").append(concreteName).append(" instead.\n\n");

        if (spec.description() != null) sb.append("/** ").append(spec.description()).append(" */\n");

        sb.append("public abstract class ").append(abstractName).append(" {\n\n");

        // Domain events list
        sb.append("    private final List<Object> domainEvents = new ArrayList<>();\n\n");

        // ID field
        if (spec.id() != null) {
            String idType = TypeMapper.toJava(spec.id().type(), basePkg);
            sb.append("    private final ").append(idType).append(" ").append(spec.id().name()).append(";\n");
        }

        // Fields
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg);
                String modifier = "list".equals(f.getValue().type()) ? "" : "";
                sb.append("    protected ").append(javaType).append(" ").append(f.getKey()).append(";\n");
            }
        }
        sb.append("\n");

        // Constructor (protected, for factory commands / reconstitution)
        sb.append("    protected ").append(abstractName).append("(");
        var params = new StringJoiner(", ");
        if (spec.id() != null) {
            params.add(TypeMapper.toJava(spec.id().type(), basePkg) + " " + spec.id().name());
        }
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                params.add(TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg) + " " + f.getKey());
            }
        }
        sb.append(params).append(") {\n");
        if (spec.id() != null) {
            sb.append("        if (").append(spec.id().name()).append(" == null) ")
              .append("throw new IllegalArgumentException(\"id must not be null\");\n");
            sb.append("        this.").append(spec.id().name()).append(" = ").append(spec.id().name()).append(";\n");
        }
        if (spec.fields() != null) {
            for (var key : spec.fields().keySet()) {
                sb.append("        this.").append(key).append(" = ").append(key).append(";\n");
            }
        }
        sb.append("    }\n\n");

        // ID getter
        if (spec.id() != null) {
            String idType = TypeMapper.toJava(spec.id().type(), basePkg);
            sb.append("    public ").append(idType).append(" ").append(spec.id().name())
              .append("() { return ").append(spec.id().name()).append("; }\n\n");
        }

        // Field getters
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg);
                sb.append("    public ").append(javaType).append(" ").append(f.getKey())
                  .append("() { return ").append(f.getKey()).append("; }\n");
            }
            sb.append("\n");
        }

        // Domain events support
        sb.append("    protected void registerEvent(Object event) {\n");
        sb.append("        domainEvents.add(event);\n");
        sb.append("    }\n\n");
        sb.append("    public List<Object> pullDomainEvents() {\n");
        sb.append("        List<Object> events = new ArrayList<>(domainEvents);\n");
        sb.append("        domainEvents.clear();\n");
        sb.append("        return Collections.unmodifiableList(events);\n");
        sb.append("    }\n\n");

        // Command stubs
        if (spec.commands() != null && !spec.commands().isEmpty()) {
            sb.append("    // ── Commands ────────────────────────────────────────────────────────────\n\n");
            for (var cmdEntry : spec.commands().entrySet()) {
                appendCommandMethod(sb, cmdEntry.getKey(), cmdEntry.getValue(), spec, basePkg, abstractName);
            }
        }

        // Invariants
        if (spec.invariants() != null && !spec.invariants().isEmpty()) {
            sb.append("    // ── Invariants ──────────────────────────────────────────────────────────\n\n");
            for (var inv : spec.invariants()) {
                sb.append("    /**\n");
                if (inv.when() != null) sb.append("     * When: ").append(inv.when()).append("\n");
                sb.append("     * Expression: ").append(inv.expression()).append("\n");
                if (inv.message() != null) sb.append("     * Message: ").append(inv.message()).append("\n");
                sb.append("     */\n");
                sb.append("    // TODO: enforce invariant '").append(inv.name()).append("'\n\n");
            }
        }

        // State machine constants
        if (spec.stateMachine() != null) {
            appendStateMachineSupport(sb, spec.stateMachine());
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void appendCommandMethod(StringBuilder sb, String cmdName, CommandSpec cmd,
                                      AggregateSpec agg, String basePkg, String abstractName) {
        boolean isFactory = cmd.factory();

        if (cmd.description() != null) sb.append("    /** ").append(cmd.description()).append(" */\n");

        if (isFactory) {
            // Static factory method
            sb.append("    public static ").append(abstractName.replace("Abstract", "")).append(" ").append(cmdName).append("(");
        } else {
            sb.append("    public void ").append(cmdName).append("(");
        }

        var params = new StringJoiner(", ");
        if (cmd.parameters() != null) {
            for (var p : cmd.parameters().entrySet()) {
                params.add(TypeMapper.toJava(p.getValue(), basePkg) + " " + p.getKey());
            }
        }
        sb.append(params).append(") {\n");

        // Preconditions
        if (cmd.preconditions() != null) {
            for (var pre : cmd.preconditions()) {
                sb.append("        // Precondition: ").append(pre).append("\n");
            }
        }

        // Sets
        if (cmd.sets() != null) {
            for (var s : cmd.sets().entrySet()) {
                sb.append("        this.").append(s.getKey()).append(" = ").append(s.getValue()).append(";\n");
            }
        }

        // Emits
        if (cmd.emits() != null) {
            for (var event : cmd.emits()) {
                sb.append("        // TODO: registerEvent(new ").append(event).append("(...));\n");
            }
        }

        if (isFactory) {
            sb.append("        // TODO: implement factory and return new instance\n");
            sb.append("        throw new UnsupportedOperationException(\"Factory '").append(cmdName).append("' not yet implemented\");\n");
        }

        sb.append("    }\n\n");
    }

    private void appendStateMachineSupport(StringBuilder sb, StateMachineSpec sm) {
        sb.append("    // ── State Machine ────────────────────────────────────────────────────────\n\n");
        sb.append("    // Initial state: ").append(sm.initial()).append("\n");
        if (sm.terminal() != null && !sm.terminal().isEmpty()) {
            sb.append("    // Terminal states: ").append(String.join(", ", sm.terminal())).append("\n");
        }
        sb.append("    // Transitions:\n");
        for (var t : sm.transitions()) {
            sb.append("    //   ").append(t.from()).append(" --[").append(t.command())
              .append("]--> ").append(t.to()).append("\n");
        }
        sb.append("\n");
        sb.append("    protected void validateTransition(String currentState, String targetState) {\n");
        sb.append("        // TODO: enforce state machine transitions\n");
        sb.append("    }\n\n");
    }

    // ── Manual stub ───────────────────────────────────────────────────────────

    private String buildManualAggregate(String name, String manPkg, String abstractName,
                                         String genPkg, AggregateSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(manPkg).append(";\n\n");
        sb.append("import ").append(genPkg).append(".").append(abstractName).append(";\n\n");

        if (spec.description() != null) sb.append("/** ").append(spec.description()).append(" */\n");

        sb.append("public class ").append(name).append(" extends ").append(abstractName).append(" {\n\n");

        // Constructor forwarding
        sb.append("    public ").append(name).append("(");
        var params = new StringJoiner(", ");
        if (spec.id() != null) {
            params.add(TypeMapper.toJava(spec.id().type(), basePkg) + " " + spec.id().name());
        }
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                params.add(TypeMapper.toJava(f.getValue().type(), f.getValue().itemType(), basePkg) + " " + f.getKey());
            }
        }
        sb.append(params).append(") {\n");
        sb.append("        super(");
        var args = new StringJoiner(", ");
        if (spec.id() != null) args.add(spec.id().name());
        if (spec.fields() != null) spec.fields().keySet().forEach(args::add);
        sb.append(args).append(");\n");
        sb.append("    }\n\n");

        sb.append("    // Add domain behaviour, override command methods, enforce invariants here\n");
        sb.append("}\n");
        return sb.toString();
    }
}
