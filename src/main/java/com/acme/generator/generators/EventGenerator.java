package com.acme.generator.generators;

import com.acme.generator.model.Models.EventSpec;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

import java.util.StringJoiner;

/**
 * Generates domain event records.
 * Events are immutable data — Java records are a perfect fit.
 * All events are generated (no manual extension needed).
 */
public class EventGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var events = ctx.spec().events();
        if (events == null || events.isEmpty()) return;

        System.out.println("[EventGenerator] Generating " + events.size() + " event(s)...");

        for (var entry : events.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            EventSpec spec = entry.getValue();
            String pkg = ctx.generatedPackage() + ".domain.event";
            String basePkg = ctx.basePackage();

            String content = buildEvent(name, pkg, spec, basePkg);
            writer.writeGenerated(ctx.generatedDir().resolve("domain/event"), name, content);
        }
    }

    private String buildEvent(String name, String pkg, EventSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");

        if (spec.description() != null) sb.append("/** ").append(spec.description()).append(" */\n");

        sb.append("/** Version: ").append(spec.version()).append(" | Aggregate: ").append(spec.aggregate()).append(" */\n");

        // Build record components
        var components = new StringJoiner(",\n        ");
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                String javaType = TypeMapper.toJava(f.getValue(), basePkg);
                components.add(javaType + " " + f.getKey());
            }
        }

        // Add metadata fields always present in domain events
        components.add("java.time.Instant occurredOn");
        components.add("String eventId");

        sb.append("public record ").append(name).append("(\n        ")
          .append(components)
          .append("\n) implements DomainEvent {\n\n");

        // Convenience factory
        sb.append("    /** Creates a new event with a generated ID and current timestamp. */\n");
        sb.append("    public static ").append(name).append(" of(");
        var params = new StringJoiner(", ");
        if (spec.fields() != null) {
            for (var f : spec.fields().entrySet()) {
                params.add(TypeMapper.toJava(f.getValue(), basePkg) + " " + f.getKey());
            }
        }
        sb.append(params).append(") {\n");
        sb.append("        return new ").append(name).append("(");
        var args = new StringJoiner(", ");
        if (spec.fields() != null) spec.fields().keySet().forEach(args::add);
        args.add("java.time.Instant.now()");
        args.add("java.util.UUID.randomUUID().toString()");
        sb.append(args).append(");\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }
}
