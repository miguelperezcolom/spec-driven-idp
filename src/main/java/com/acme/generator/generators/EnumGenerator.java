package com.acme.generator.generators;

import com.acme.generator.model.Models.EnumSpec;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

/**
 * Generates Java enums from the DSL enum definitions.
 * Enums are always generated (no abstract/manual split needed).
 */
public class EnumGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var enums = ctx.spec().enums();
        if (enums == null || enums.isEmpty()) return;

        System.out.println("[EnumGenerator] Generating " + enums.size() + " enum(s)...");

        for (var entry : enums.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            EnumSpec spec = entry.getValue();
            String pkg = ctx.generatedPackage() + ".domain.enums";

            String content = buildEnum(name, pkg, spec);
            writer.writeGenerated(ctx.generatedDir().resolve("domain/enums"), name, content);
        }
    }

    private String buildEnum(String name, String pkg, EnumSpec spec) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");

        if (spec.description() != null) {
            sb.append("/** ").append(spec.description()).append(" */\n");
        }

        sb.append("public enum ").append(name).append(" {\n");

        var values = spec.values();
        for (int i = 0; i < values.size(); i++) {
            sb.append("    ").append(values.get(i));
            if (i < values.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
