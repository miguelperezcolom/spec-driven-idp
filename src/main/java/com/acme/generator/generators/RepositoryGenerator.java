package com.acme.generator.generators;

import com.acme.generator.model.Models.RepositoryMethodSpec;
import com.acme.generator.model.Models.RepositorySpec;
import com.acme.generator.util.TypeMapper;
import com.acme.generator.writer.SourceFileWriter;

import java.util.Map;

/**
 * Generates repository interfaces in the application layer.
 *
 * <p>Pattern:
 * <ul>
 *   <li>{@code generated/application/port/XxxRepository.java} — generated interface (always overwritten)</li>
 *   <li>Implementation stub in infrastructure layer is left to the developer</li>
 * </ul>
 */
public class RepositoryGenerator implements CodeGenerator {

    private final SourceFileWriter writer = new SourceFileWriter();

    @Override
    public void generate(GeneratorContext ctx) throws Exception {
        var repos = ctx.spec().repositories();
        if (repos == null || repos.isEmpty()) return;

        System.out.println("[RepositoryGenerator] Generating " + repos.size() + " repository interface(s)...");

        for (var entry : repos.entrySet()) {
            String name = TypeMapper.capitalise(entry.getKey());
            RepositorySpec spec = entry.getValue();
            generateRepository(ctx, name, spec);
        }
    }

    private void generateRepository(GeneratorContext ctx, String name, RepositorySpec spec) throws Exception {
        String pkg = ctx.generatedPackage() + ".application.port";
        String basePkg = ctx.basePackage();
        String aggregateName = spec.aggregate();

        String content = buildInterface(name + "Repository", pkg, aggregateName, spec, basePkg);
        writer.writeGenerated(ctx.generatedDir().resolve("application/port"), name + "Repository", content);
    }

    private String buildInterface(String ifaceName, String pkg, String aggregateName,
                                   RepositorySpec spec, String basePkg) {
        var sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import java.util.Optional;\n");
        sb.append("import java.util.List;\n\n");
        sb.append("// AUTO-GENERATED — do not edit.\n\n");
        sb.append("/**\n * Repository interface for {@link ").append(aggregateName).append("}.\n */\n");
        sb.append("public interface ").append(ifaceName).append(" {\n\n");

        // Standard CRUD operations
        sb.append("    void save(").append(aggregateName).append(" aggregate);\n\n");
        sb.append("    Optional<").append(aggregateName).append("> findById(Object id);\n\n");
        sb.append("    void delete(").append(aggregateName).append(" aggregate);\n\n");

        // Custom methods from spec
        if (spec.methods() != null && !spec.methods().isEmpty()) {
            sb.append("    // ── Custom query methods ────────────────────────────────────────────────\n\n");
            for (var methodEntry : spec.methods().entrySet()) {
                appendMethod(sb, methodEntry.getKey(), methodEntry.getValue(), aggregateName, basePkg);
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void appendMethod(StringBuilder sb, String methodName, RepositoryMethodSpec method,
                               String aggregateName, String basePkg) {
        String returnType = resolveReturnType(method, aggregateName, basePkg);
        String paramType = method.parameter() != null
                ? TypeMapper.toJava(method.parameter(), basePkg)
                : "Object";
        String paramName = method.by() != null ? method.by() : "param";

        sb.append("    ").append(returnType).append(" ").append(methodName)
          .append("(").append(paramType).append(" ").append(paramName).append(");\n\n");
    }

    private String resolveReturnType(RepositoryMethodSpec method, String aggregateName, String basePkg) {
        if (method.returns() != null) {
            return TypeMapper.toJava(method.returns(), basePkg);
        }
        // Default: Optional unless required=true
        return method.required()
                ? aggregateName
                : "Optional<" + aggregateName + ">";
    }
}
