package com.acme.generator.generators;

import com.acme.generator.model.DomainSpec;

import java.nio.file.Path;

/**
 * Shared context available to every generator.
 */
public record GeneratorContext(
        DomainSpec spec,
        Path outputRoot,    // e.g. src/main/java
        Path testOutputRoot // e.g. src/test/java
) {
    /** Base package from the domain spec, e.g. "com.acme.booking" */
    public String basePackage() {
        return spec.basePackage();
    }

    /** Package path for generated sources: basePackage + ".generated" */
    public String generatedPackage() {
        return basePackage() + ".generated";
    }

    /** Package path for manual (extension) sources: basePackage */
    public String manualPackage() {
        return basePackage();
    }

    public Path generatedDir() {
        return packageToPath(outputRoot, generatedPackage());
    }

    public Path manualDir() {
        return packageToPath(outputRoot, manualPackage());
    }

    public Path testGeneratedDir() {
        return packageToPath(testOutputRoot, generatedPackage());
    }

    public Path testManualDir() {
        return packageToPath(testOutputRoot, manualPackage());
    }

    private static Path packageToPath(Path root, String pkg) {
        return root.resolve(pkg.replace('.', '/'));
    }
}
