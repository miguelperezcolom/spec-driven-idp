package com.acme.generator.model;

import java.util.List;
import java.util.Map;

/**
 * Root model parsed from the domain DSL YAML spec.
 */
public record DomainSpec(
        DomainInfo domain,
        List<String> imports,
        Map<String, ValueObjectSpec> valueObjects,
        Map<String, EnumSpec> enums,
        Map<String, EntitySpec> entities,
        Map<String, AggregateSpec> aggregates,
        Map<String, EventSpec> events,
        Map<String, RepositorySpec> repositories,
        Map<String, DomainServiceSpec> domainServices,
        TestsSpec tests,
        GenerationSpec generation
) {
    public String basePackage() {
        return domain.pkg();
    }
}
