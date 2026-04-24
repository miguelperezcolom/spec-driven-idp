package com.acme.generator.generators;

/**
 * Contract for all code generators.
 */
public interface CodeGenerator {
    /**
     * Generates all files for this generator's concern.
     */
    void generate(GeneratorContext ctx) throws Exception;
}
