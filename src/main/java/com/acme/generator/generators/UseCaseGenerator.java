package com.acme.generator.generators;

import com.acme.generator.model.Models.*;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Generates use cases (application layer) from aggregate commands.
 *
 * <p>For each command in each aggregate, generates:
 * <ul>
 *   <li>{@code generated/application/usecase/AbstractXxxUseCase.java} — input record + abstract base (always overwritten)</li>
 *   <li>{@code application/usecase/XxxUseCase.java} — manual implementation stub (only created if absent)</li>
 * </ul>
 *
 * <p>Each use case follows the pattern:
 * <pre>
 *   Input  — an immutable record with the command parameters
 *   Output — void or a typed result (e.g. the aggregate id for factory commands)
 *   execute(Input) — the single public method
 * </pre>
 */
public class UseCaseGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var aggregates = ctx.spec().aggregates();
        if (aggregates == null || aggregates.isEmpty()) return;

        // Also generate the UseCase marker interface
        generateMarkerInterface(ctx);

        int total = aggregates.values().stream()
                .mapToInt(a -> a.commands() == null ? 0 : a.commands().size())
                .sum();

        if (total == 0) return;

        System.out.println("[UseCaseGenerator] Generating " + total + " use case(s)...");

        for (var aggEntry : aggregates.entrySet()) {
            String aggregateName = TypeMapper.capitalise(aggEntry.getKey());
            AggregateSpec aggSpec = aggEntry.getValue();

            if (aggSpec.commands() == null || aggSpec.commands().isEmpty()) continue;

            for (var cmdEntry : aggSpec.commands().entrySet()) {
                String commandName = TypeMapper.capitalise(cmdEntry.getKey());
                CommandSpec cmd = cmdEntry.getValue();
                generateUseCase(ctx, aggregateName, commandName, cmd, aggSpec);
            }
        }
    }

    // ── Marker interface ──────────────────────────────────────────────────────

    private void generateMarkerInterface(GeneratorContext ctx) throws Exception {
        String pkg = ctx.generatedPackage() + ".application.usecase";
        String content =
                "package " + pkg + ";\n\n" +
                "// AUTO-GENERATED — do not edit.\n\n" +
                "/**\n" +
                " * Marker interface for all use cases.\n" +
                " *\n" +
                " * @param <I> input type (command)\n" +
                " * @param <O> output type (void → {@link Void})\n" +
                " */\n" +
                "public interface UseCase<I, O> {\n" +
                "    O execute(I input);\n" +
                "}\n";

        writer.writeGenerated(ctx.generatedDir().resolve("application/usecase"), "UseCase", content);
    }

    // ── Use case per command ──────────────────────────────────────────────────

    private void generateUseCase(GeneratorContext ctx, String aggregateName, String commandName,
                                  CommandSpec cmd, AggregateSpec aggSpec) throws Exception {
        String useCaseName   = commandName + "UseCase";
        String abstractName  = "Abstract" + useCaseName;
        String inputName     = commandName + "Input";

        String genPkg = ctx.generatedPackage() + ".application.usecase";
        String manPkg = ctx.manualPackage()    + ".application.usecase";
        String repoPkg = ctx.generatedPackage() + ".application.port";
        String basePkg = ctx.basePackage();

        // Resolve output type: factory commands return the aggregate id type; others return Void
        String outputType = resolveOutputType(cmd, aggSpec, basePkg);

        String generated = buildAbstractUseCase(
                abstractName, genPkg, useCaseName, manPkg,
                inputName, aggregateName, commandName, cmd,
                repoPkg, basePkg, outputType, aggSpec);

        writer.writeGenerated(ctx.generatedDir().resolve("application/usecase"), abstractName, generated);

        String manual = buildManualUseCase(
                useCaseName, manPkg, abstractName, genPkg,
                inputName, outputType, aggregateName, commandName, cmd, basePkg);

        writer.writeManualStub(ctx.manualDir().resolve("application/usecase"), useCaseName, manual);
    }

    // ── Abstract use case ─────────────────────────────────────────────────────

    private String buildAbstractUseCase(String abstractName, String pkg,
                                         String concreteName, String manPkg,
                                         String inputName, String aggregateName,
                                         String commandName, CommandSpec cmd,
                                         String repoPkg, String basePkg,
                                         String outputType, AggregateSpec aggSpec) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// AUTO-GENERATED — do not edit. Implement ").append(concreteName).append(" instead.\n\n");

        if (cmd.description() != null) {
            sb.append("/**\n * ").append(cmd.description()).append("\n */\n");
        }

        sb.append("public abstract class ").append(abstractName)
          .append(" implements UseCase<").append(abstractName).append(".").append(inputName)
          .append(", ").append(outputType).append("> {\n\n");

        // ── Input record ──────────────────────────────────────────────────────
        sb.append("    /**\n     * Input record for the '").append(commandName).append("' command.\n     */\n");
        sb.append("    public record ").append(inputName).append("(\n");
        var fields = new StringJoiner(",\n        ");
        if (cmd.parameters() != null && !cmd.parameters().isEmpty()) {
            for (var p : cmd.parameters().entrySet()) {
                fields.add(TypeMapper.toJava(p.getValue(), basePkg) + " " + p.getKey());
            }
        } else {
            // Factory commands without explicit params still need the aggregate id if not a create
            if (!cmd.factory()) {
                String idType = aggSpec.id() != null
                        ? TypeMapper.toJava(aggSpec.id().type(), basePkg) : "Object";
                String idName = aggSpec.id() != null ? aggSpec.id().name() : "id";
                fields.add(idType + " " + idName);
            }
        }
        sb.append("        ").append(fields).append("\n    ) {}\n\n");

        // ── Repository field ──────────────────────────────────────────────────
        sb.append("    protected final ").append(aggregateName).append("Repository repository;\n\n");

        sb.append("    protected ").append(abstractName)
          .append("(").append(aggregateName).append("Repository repository) {\n");
        sb.append("        this.repository = repository;\n");
        sb.append("    }\n\n");

        // ── execute — abstract ────────────────────────────────────────────────
        sb.append("    @Override\n");
        sb.append("    public abstract ").append(outputType).append(" execute(").append(inputName).append(" input);\n\n");

        // ── Precondition helpers ───────────────────────────────────────────────
        if (cmd.preconditions() != null && !cmd.preconditions().isEmpty()) {
            sb.append("    // ── Preconditions ────────────────────────────────────────────────────────\n\n");
            for (var pre : cmd.preconditions()) {
                sb.append("    /** Precondition: ").append(pre).append(" */\n");
                sb.append("    protected void check_").append(sanitise(pre))
                  .append("(").append(inputName).append(" input) {\n");
                sb.append("        // TODO: enforce precondition\n");
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ── Manual use case ───────────────────────────────────────────────────────

    private String buildManualUseCase(String useCaseName, String manPkg,
                                       String abstractName, String genPkg,
                                       String inputName, String outputType,
                                       String aggregateName, String commandName,
                                       CommandSpec cmd, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(manPkg).append(";\n\n");
        sb.append("import ").append(genPkg).append(".").append(abstractName).append(";\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.springframework.transaction.annotation.Transactional;\n\n");

        if (cmd.description() != null) {
            sb.append("/** ").append(cmd.description()).append(" */\n");
        }

        sb.append("@Service\n");
        sb.append("public class ").append(useCaseName)
          .append(" extends ").append(abstractName).append(" {\n\n");

        sb.append("    public ").append(useCaseName)
          .append("(").append(aggregateName).append("Repository repository) {\n");
        sb.append("        super(repository);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    @Transactional\n");
        sb.append("    public ").append(outputType).append(" execute(").append(inputName).append(" input) {\n");

        if (cmd.factory()) {
            // Factory: create and persist aggregate
            sb.append("        // 1. Validate input\n");
            sb.append("        // 2. Create aggregate\n");
            sb.append("        // var aggregate = ").append(aggregateName).append(".").append(toLowerFirst(commandName)).append("(...);\n");
            sb.append("        // 3. Persist\n");
            sb.append("        // repository.save(aggregate);\n");
            sb.append("        // 4. Return id\n");
            if (!"Void".equals(outputType)) {
                sb.append("        throw new UnsupportedOperationException(\"Not yet implemented\");\n");
            }
        } else {
            // Regular command: load → execute → save
            String idName = "input.id()";
            sb.append("        // 1. Load aggregate\n");
            sb.append("        var aggregate = repository.findById(").append(idName).append(")\n");
            sb.append("                .orElseThrow(() -> new IllegalArgumentException(\"")
              .append(aggregateName).append(" not found\"));\n\n");
            sb.append("        // 2. Execute command\n");
            sb.append("        aggregate.").append(toLowerFirst(commandName)).append("(");
            if (cmd.parameters() != null && !cmd.parameters().isEmpty()) {
                sb.append(cmd.parameters().keySet().stream()
                        .map(p -> "input." + p + "()")
                        .reduce((a, b) -> a + ", " + b).orElse(""));
            }
            sb.append(");\n\n");
            sb.append("        // 3. Persist\n");
            sb.append("        repository.save(aggregate);\n");

            if (cmd.emits() != null && !cmd.emits().isEmpty()) {
                sb.append("\n        // 4. Domain events are persisted via Outbox pattern\n");
                sb.append("        //    aggregate.pullDomainEvents() → outbox handler\n");
            }

            if (!"Void".equals(outputType)) {
                sb.append("\n        throw new UnsupportedOperationException(\"Return value not yet implemented\");\n");
            }
        }

        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Factory commands return the aggregate id type.
     * Regular commands return Void.
     */
    private String resolveOutputType(CommandSpec cmd, AggregateSpec aggSpec, String basePkg) {
        if (cmd.factory() && aggSpec.id() != null) {
            return TypeMapper.toJava(aggSpec.id().type(), basePkg);
        }
        return "Void";
    }

    private String sanitise(String s) {
        return s.replaceAll("[^A-Za-z0-9]", "_").toLowerCase();
    }

    private String toLowerFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
