package com.acme.generator.generators;

import com.acme.generator.model.Models.*;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

/**
 * Generates test skeletons for domain objects (value objects, invariants, commands, state machine, events).
 * Tests go to src/test/java following the same generated/manual split.
 */
public class TestGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        TestsSpec tests = ctx.spec().tests();
        if (tests == null || tests.generate() == null) return;

        TestGenerationSpec gen = tests.generate();

        DomainTestSpec dom = gen.domain();
        if (dom != null) {
            if (dom.valueObjects()) generateValueObjectTests(ctx);
            if (dom.commands() || dom.invariants()) generateAggregateTests(ctx);
            if (dom.stateMachine()) generateStateMachineTests(ctx);
            if (dom.events()) generateEventTests(ctx);
        }

        RepositoryTestSpec rep = gen.repositories();
        if (rep != null && rep.contractTests()) generateRepositoryContractTests(ctx);

        SerializationTestSpec ser = gen.serialization();
        if (ser != null && ser.events()) generateEventSerializationTests(ctx);
    }

    // ── Value Object Tests ────────────────────────────────────────────────────

    private void generateValueObjectTests(GeneratorContext ctx) throws Exception {
        var vos = ctx.spec().valueObjects();
        if (vos == null || vos.isEmpty()) return;

        System.out.println("[TestGenerator] Generating value object tests...");

        for (var entry : vos.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            ValueObjectSpec spec = entry.getValue();
            String pkg = ctx.generatedPackage() + ".domain.vo";
            String testPkg = ctx.generatedPackage() + ".domain.vo";

            String content = buildVOTest(name, testPkg, pkg, spec, ctx.basePackage());
            writer.writeGenerated(ctx.testGeneratedDir().resolve("domain/vo"), name + "Test", content);
        }
    }

    private String buildVOTest(String name, String testPkg, String srcPkg, ValueObjectSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(testPkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");
        sb.append("@DisplayName(\"").append(name).append(" value object\")\n");
        sb.append("class ").append(name).append("Test {\n\n");

        if ("object".equals(spec.type())) {
            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"should create valid ").append(name).append("\")\n");
            sb.append("    void shouldCreateValid() {\n");
            sb.append("        // TODO: create a valid ").append(name).append(" and assert fields\n");
            sb.append("    }\n\n");
        } else {
            String javaType = TypeMapper.toJava(spec.type(), basePkg);
            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"should wrap a valid value\")\n");
            sb.append("    void shouldWrapValidValue() {\n");
            sb.append("        // TODO: provide a valid ").append(javaType).append(" value\n");
            sb.append("        // var vo = new ").append(name).append("(...);\n");
            sb.append("        // assertNotNull(vo.value());\n");
            sb.append("    }\n\n");

            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"should reject null value\")\n");
            sb.append("    void shouldRejectNull() {\n");
            sb.append("        assertThrows(IllegalArgumentException.class, () -> new ").append(name).append("(null));\n");
            sb.append("    }\n\n");

            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"should be equal when values match\")\n");
            sb.append("    void shouldHaveValueEquality() {\n");
            sb.append("        // TODO: provide two identical values\n");
            sb.append("        // var a = new ").append(name).append("(value);\n");
            sb.append("        // var b = new ").append(name).append("(value);\n");
            sb.append("        // assertEquals(a, b);\n");
            sb.append("    }\n");
        }

        // Constraint tests
        if (spec.constraints() != null && !spec.constraints().isEmpty()) {
            var c = spec.constraints().get(0);
            appendConstraintTests(sb, name, c);
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void appendConstraintTests(StringBuilder sb, String name, ConstraintSpec c) {
        if (c.minLength() != null) {
            sb.append("\n    @Test\n");
            sb.append("    @DisplayName(\"should reject value shorter than ").append(c.minLength()).append("\")\n");
            sb.append("    void shouldRejectTooShort() {\n");
            sb.append("        assertThrows(IllegalArgumentException.class,\n");
            sb.append("            () -> new ").append(name).append("(\"x\".repeat(").append(c.minLength() - 1).append(")));\n");
            sb.append("    }\n");
        }
        if (c.maxLength() != null) {
            sb.append("\n    @Test\n");
            sb.append("    @DisplayName(\"should reject value longer than ").append(c.maxLength()).append("\")\n");
            sb.append("    void shouldRejectTooLong() {\n");
            sb.append("        assertThrows(IllegalArgumentException.class,\n");
            sb.append("            () -> new ").append(name).append("(\"x\".repeat(").append(c.maxLength() + 1).append(")));\n");
            sb.append("    }\n");
        }
    }

    // ── Aggregate / Command / Invariant Tests ─────────────────────────────────

    private void generateAggregateTests(GeneratorContext ctx) throws Exception {
        var aggregates = ctx.spec().aggregates();
        if (aggregates == null || aggregates.isEmpty()) return;

        System.out.println("[TestGenerator] Generating aggregate command/invariant tests...");

        for (var entry : aggregates.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            AggregateSpec spec = entry.getValue();
            String pkg = ctx.generatedPackage() + ".domain.aggregate";

            String content = buildAggregateTest(name, pkg, spec, ctx.basePackage());
            writer.writeGenerated(ctx.testGeneratedDir().resolve("domain/aggregate"), name + "Test", content);
        }
    }

    private String buildAggregateTest(String name, String pkg, AggregateSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");
        sb.append("@DisplayName(\"").append(name).append(" aggregate\")\n");
        sb.append("class ").append(name).append("Test {\n\n");

        // Command tests
        if (spec.commands() != null) {
            for (var cmdEntry : spec.commands().entrySet()) {
                String cmdName = cmdEntry.getKey();
                CommandSpec cmd = cmdEntry.getValue();
                sb.append("    @Test\n");
                sb.append("    @DisplayName(\"should execute command '").append(cmdName).append("'\")\n");
                sb.append("    void shouldExecute").append(TypeMapper.capitalise(cmdName)).append("() {\n");
                sb.append("        // TODO: set up aggregate, call ").append(cmdName).append("(), assert state\n");
                if (cmd.emits() != null && !cmd.emits().isEmpty()) {
                    sb.append("        // Expected events: ").append(String.join(", ", cmd.emits())).append("\n");
                    sb.append("        // var events = aggregate.pullDomainEvents();\n");
                    sb.append("        // assertEquals(1, events.size());\n");
                }
                sb.append("    }\n\n");
            }
        }

        // Invariant tests
        if (spec.invariants() != null) {
            for (var inv : spec.invariants()) {
                sb.append("    @Test\n");
                sb.append("    @DisplayName(\"invariant: ").append(inv.name()).append("\")\n");
                sb.append("    void invariant").append(TypeMapper.capitalise(inv.name())).append("() {\n");
                sb.append("        // Expression: ").append(inv.expression()).append("\n");
                if (inv.message() != null) sb.append("        // Expected message: ").append(inv.message()).append("\n");
                sb.append("        // TODO: assert invariant is enforced\n");
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ── State Machine Tests ───────────────────────────────────────────────────

    private void generateStateMachineTests(GeneratorContext ctx) throws Exception {
        var aggregates = ctx.spec().aggregates();
        if (aggregates == null) return;

        for (var entry : aggregates.entrySet()) {
            if (entry.getValue().stateMachine() == null) continue;

            String name = TypeMapper.capitalise(entry.getKey());
            StateMachineSpec sm = entry.getValue().stateMachine();
            String pkg = ctx.generatedPackage() + ".domain.aggregate";

            String content = buildStateMachineTest(name, pkg, sm);
            writer.writeGenerated(ctx.testGeneratedDir().resolve("domain/aggregate"), name + "StateMachineTest", content);
        }
    }

    private String buildStateMachineTest(String name, String pkg, StateMachineSpec sm) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");
        sb.append("@DisplayName(\"").append(name).append(" state machine\")\n");
        sb.append("class ").append(name).append("StateMachineTest {\n\n");
        sb.append("    // Initial state: ").append(sm.initial()).append("\n\n");

        for (var t : sm.transitions()) {
            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"").append(t.from()).append(" --[").append(t.command())
              .append("]--> ").append(t.to()).append("\")\n");
            sb.append("    void transition_").append(t.from()).append("_to_").append(t.to()).append("() {\n");
            sb.append("        // TODO: set up aggregate in '").append(t.from()).append("' state\n");
            sb.append("        // aggregate.").append(t.command()).append("(...);\n");
            sb.append("        // assertEquals(\"").append(t.to()).append("\", aggregate.").append(sm.field()).append("());\n");
            sb.append("    }\n\n");
        }

        if (sm.terminal() != null && !sm.terminal().isEmpty()) {
            for (var terminal : sm.terminal()) {
                sb.append("    @Test\n");
                sb.append("    @DisplayName(\"should reject commands in terminal state ").append(terminal).append("\")\n");
                sb.append("    void shouldRejectCommandsInTerminalState_").append(terminal).append("() {\n");
                sb.append("        // TODO: put aggregate in '").append(terminal).append("' state and assert any command throws\n");
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ── Event Tests ───────────────────────────────────────────────────────────

    private void generateEventTests(GeneratorContext ctx) throws Exception {
        var events = ctx.spec().events();
        if (events == null || events.isEmpty()) return;

        System.out.println("[TestGenerator] Generating event tests...");

        for (var entry : events.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            EventSpec spec = entry.getValue();
            String pkg = ctx.generatedPackage() + ".domain.event";

            String content = buildEventTest(name, pkg, spec, ctx.basePackage());
            writer.writeGenerated(ctx.testGeneratedDir().resolve("domain/event"), name + "Test", content);
        }
    }

    private String buildEventTest(String name, String pkg, EventSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");
        sb.append("@DisplayName(\"").append(name).append(" event\")\n");
        sb.append("class ").append(name).append("Test {\n\n");
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"should create event with generated id and timestamp\")\n");
        sb.append("    void shouldCreateWithMetadata() {\n");
        sb.append("        // var event = ").append(name).append(".of(/* fields */);\n");
        sb.append("        // assertNotNull(event.eventId());\n");
        sb.append("        // assertNotNull(event.occurredOn());\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ── Repository Contract Tests ─────────────────────────────────────────────

    private void generateRepositoryContractTests(GeneratorContext ctx) throws Exception {
        var repos = ctx.spec().repositories();
        if (repos == null || repos.isEmpty()) return;

        System.out.println("[TestGenerator] Generating repository contract tests...");

        for (var entry : repos.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            RepositorySpec spec = entry.getValue();
            String pkg = ctx.generatedPackage() + ".application.port";

            String content = buildRepoContractTest(name, pkg, spec);
            writer.writeGenerated(ctx.testGeneratedDir().resolve("application/port"), name + "RepositoryContractTest", content);
        }
    }

    private String buildRepoContractTest(String name, String pkg, RepositorySpec spec) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n");
        sb.append("// Implement this abstract class with a concrete repository for integration tests.\n\n");
        sb.append("@DisplayName(\"").append(name).append("Repository contract\")\n");
        sb.append("public abstract class ").append(name).append("RepositoryContractTest {\n\n");
        sb.append("    protected abstract ").append(name).append("Repository repository();\n\n");
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"should save and find by id\")\n");
        sb.append("    void shouldSaveAndFindById() {\n");
        sb.append("        // TODO: create aggregate, save, findById, assert equal\n");
        sb.append("    }\n\n");
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"should return empty when not found\")\n");
        sb.append("    void shouldReturnEmptyWhenNotFound() {\n");
        sb.append("        // TODO: assert findById returns Optional.empty() for unknown id\n");
        sb.append("    }\n\n");
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"should delete aggregate\")\n");
        sb.append("    void shouldDelete() {\n");
        sb.append("        // TODO: save, delete, assert not found\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ── Event Serialization Tests ─────────────────────────────────────────────

    private void generateEventSerializationTests(GeneratorContext ctx) throws Exception {
        var events = ctx.spec().events();
        if (events == null || events.isEmpty()) return;

        System.out.println("[TestGenerator] Generating event serialization tests...");

        for (var entry : events.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            String pkg = ctx.generatedPackage() + ".domain.event";

            String content = buildSerializationTest(name, pkg, ctx.basePackage());
            writer.writeGenerated(ctx.testGeneratedDir().resolve("domain/event"), name + "SerializationTest", content);
        }
    }

    private String buildSerializationTest(String name, String pkg, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");
        sb.append("@DisplayName(\"").append(name).append(" serialization\")\n");
        sb.append("class ").append(name).append("SerializationTest {\n\n");
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"should serialize and deserialize without data loss\")\n");
        sb.append("    void shouldRoundTrip() {\n");
        sb.append("        // TODO: serialize event to JSON/Avro, deserialize, assertEquals\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
