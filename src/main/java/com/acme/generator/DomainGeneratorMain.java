package com.acme.generator;

import com.acme.generator.generators.*;
import com.acme.generator.parser.DomainSpecParser;
import com.acme.generator.model.DomainSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entry point for the domain DSL code generator.
 *
 * <p>Usage:
 * <pre>
 *   java DomainGeneratorMain &lt;specsDir&gt; &lt;srcOutputDir&gt; [testOutputDir]
 * </pre>
 *
 * <p>Arguments:
 * <ul>
 *   <li>{@code specsDir}       — directory containing *.yaml spec files</li>
 *   <li>{@code srcOutputDir}   — root of src/main/java</li>
 *   <li>{@code testOutputDir}  — root of src/test/java (optional, defaults to srcOutputDir/../test/java)</li>
 * </ul>
 */
public class DomainGeneratorMain {

    private static final List<CodeGenerator> GENERATORS = List.of(
            new DomainEventInterfaceGenerator(),
            new EnumGenerator(),
            new ValueObjectGenerator(),
            new AggregateGenerator(),
            new EventGenerator(),
            new RepositoryGenerator(),
            new DomainServiceGenerator(),
            new UseCaseGenerator(),
            new TestGenerator()
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: DomainGeneratorMain <specsDir> <srcOutputDir> [testOutputDir]");
            System.exit(1);
        }

        Path specsDir   = Path.of(args[0]);
        Path srcOut     = Path.of(args[1]);
        Path testOut    = args.length > 2 ? Path.of(args[2]) : srcOut.resolveSibling("test/java");

        if (!Files.isDirectory(specsDir)) {
            System.err.println("Specs directory not found: " + specsDir);
            System.exit(1);
        }

        DomainSpecParser parser = new DomainSpecParser();

        try (Stream<Path> yamlFiles = Files.walk(specsDir)
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .filter(p -> !p.getFileName().toString().startsWith("schema"))) {

            yamlFiles.forEach(yamlFile -> {
                try {
                    System.out.println("\n══════════════════════════════════════════════════");
                    System.out.println("Processing spec: " + yamlFile);
                    System.out.println("══════════════════════════════════════════════════");

                    DomainSpec spec = parser.parse(yamlFile);
                    GeneratorContext ctx = new GeneratorContext(spec, srcOut, testOut);

                    System.out.println("Domain   : " + spec.domain().name());
                    System.out.println("Package  : " + spec.basePackage());
                    System.out.println("Generated: " + ctx.generatedPackage());
                    System.out.println("──────────────────────────────────────────────────");

                    for (CodeGenerator generator : GENERATORS) {
                        generator.generate(ctx);
                    }

                    System.out.println("──────────────────────────────────────────────────");
                    System.out.println("Done: " + yamlFile.getFileName());

                } catch (Exception e) {
                    System.err.println("ERROR processing " + yamlFile + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }

        System.out.println("\n✓ Generation complete.");
    }
}
