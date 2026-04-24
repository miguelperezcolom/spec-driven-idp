package com.acme.generator.generators;

import com.acme.generator.writer.SourceFileWriter;

/**
 * Generates the DomainEvent marker interface that all events implement.
 */
public class DomainEventInterfaceGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        String pkg = ctx.generatedPackage() + ".domain.event";
        String content = buildInterface(pkg);
        writer.writeGenerated(ctx.generatedDir().resolve("domain/event"), "DomainEvent", content);
    }

    private String buildInterface(String pkg) {
        return "package " + pkg + ";\n\n" +
               "// AUTO-GENERATED — do not edit.\n\n" +
               "/**\n" +
               " * Marker interface for all domain events.\n" +
               " * Every event carries a stable eventId and the instant it occurred.\n" +
               " */\n" +
               "public interface DomainEvent {\n" +
               "    String eventId();\n" +
               "    java.time.Instant occurredOn();\n" +
               "}\n";
    }
}
