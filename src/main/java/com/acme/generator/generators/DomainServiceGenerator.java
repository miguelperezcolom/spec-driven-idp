package com.acme.generator.generators;

import com.acme.generator.model.Models.DomainServiceSpec;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

import java.util.StringJoiner;

/**
 * Generates domain service interfaces and stubs.
 *
 * <p>Pattern:
 * <ul>
 *   <li>{@code generated/.../AbstractXxxService.java} — generated abstract class</li>
 *   <li>{@code .../XxxService.java} — manual implementation stub</li>
 * </ul>
 */
public class DomainServiceGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var services = ctx.spec().domainServices();
        if (services == null || services.isEmpty()) return;

        System.out.println("[DomainServiceGenerator] Generating " + services.size() + " domain service(s)...");

        for (var entry : services.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey()) + "Service";
            DomainServiceSpec spec = entry.getValue();
            generateService(ctx, name, spec);
        }
    }

    private void generateService(GeneratorContext ctx, String name, DomainServiceSpec spec) throws Exception {
        String genPkg = ctx.generatedPackage() + ".domain.service";
        String manPkg = ctx.manualPackage() + ".domain.service";
        String basePkg = ctx.basePackage();

        String abstractName = "Abstract" + name;
        String generated = buildAbstract(abstractName, genPkg, name, spec, basePkg);
        writer.writeGenerated(ctx.generatedDir().resolve("domain/service"), abstractName, generated);

        String manual = buildManual(name, manPkg, abstractName, genPkg, spec, basePkg);
        writer.writeManualStub(ctx.manualDir().resolve("domain/service"), name, manual);
    }

    private String buildAbstract(String abstractName, String pkg, String concreteName,
                                  DomainServiceSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("// AUTO-GENERATED — do not edit. Implement ").append(concreteName).append(" instead.\n\n");

        if (spec.description() != null) sb.append("/** ").append(spec.description()).append(" */\n");

        sb.append("public abstract class ").append(abstractName).append(" {\n\n");

        String outputType = spec.output() != null ? TypeMapper.toJava(spec.output(), basePkg) : "void";

        sb.append("    public abstract ").append(outputType).append(" execute(");
        var params = new StringJoiner(", ");
        if (spec.inputs() != null) {
            for (var input : spec.inputs().entrySet()) {
                params.add(TypeMapper.toJava(input.getValue(), basePkg) + " " + input.getKey());
            }
        }
        sb.append(params).append(");\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildManual(String name, String manPkg, String abstractName,
                                String genPkg, DomainServiceSpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(manPkg).append(";\n\n");
        sb.append("import ").append(genPkg).append(".").append(abstractName).append(";\n\n");

        if (spec.description() != null) sb.append("/** ").append(spec.description()).append(" */\n");

        sb.append("public class ").append(name).append(" extends ").append(abstractName).append(" {\n\n");

        String outputType = spec.output() != null ? TypeMapper.toJava(spec.output(), basePkg) : "void";

        sb.append("    @Override\n");
        sb.append("    public ").append(outputType).append(" execute(");
        var params = new StringJoiner(", ");
        if (spec.inputs() != null) {
            for (var input : spec.inputs().entrySet()) {
                params.add(TypeMapper.toJava(input.getValue(), basePkg) + " " + input.getKey());
            }
        }
        sb.append(params).append(") {\n");
        sb.append("        // TODO: implement domain service logic\n");
        if (!"void".equals(outputType)) sb.append("        throw new UnsupportedOperationException(\"Not yet implemented\");\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
