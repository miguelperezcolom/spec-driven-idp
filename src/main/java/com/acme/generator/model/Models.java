package com.acme.generator.model;

import java.util.List;
import java.util.Map;

public final class Models {
    private Models() {}

    public record BoundedContextInfo(String name, String description) {}

    public record ValueObjectSpec(
            String type,         // "object" | scalar
            String description,
            boolean immutable,
            String generation,
            Map<String, FieldSpec> fields,
            List<ConstraintSpec> constraints,
            List<InvariantSpec> invariants
    ) {}

    public record EnumSpec(
            String description,
            List<String> values
    ) {}

    public record EntitySpec(
            String belongsTo,
            String description,
            NamedTypeSpec identity,
            Map<String, FieldSpec> fields,
            List<InvariantSpec> invariants
    ) {}

    public record AggregateSpec(
            boolean root,
            String description,
            NamedTypeSpec id,
            Map<String, FieldSpec> fields,
            List<InvariantSpec> invariants,
            Map<String, CommandSpec> commands,
            StateMachineSpec stateMachine,
            Map<String, HookSpec> hooks
    ) {}

    public record FieldSpec(
            String type,
            String itemType,
            String description,
            boolean nullable,
            Object defaultValue,
            String generation,
            ConstraintSpec constraints
    ) {}

    public record ConstraintSpec(
            Boolean required,
            Number min,
            Number max,
            Integer minLength,
            Integer maxLength,
            Integer minSize,
            Integer maxSize,
            String format,
            String pattern,
            List<Object> enumValues
    ) {}

    public record InvariantSpec(
            String name,
            String when,
            String expression,
            String message
    ) {}

    public record CommandSpec(
            boolean factory,
            String description,
            Map<String, String> parameters,
            List<String> preconditions,
            Map<String, Object> sets,
            List<String> emits,
            List<String> hooks
    ) {}

    public record StateMachineSpec(
            String field,
            String initial,
            List<String> terminal,
            List<TransitionSpec> transitions
    ) {}

    public record TransitionSpec(String from, String to, String command) {}

    public record EventSpec(
            String aggregate,
            int version,
            String description,
            Map<String, String> fields
    ) {}

    public record RepositorySpec(
            String aggregate,
            Map<String, RepositoryMethodSpec> methods
    ) {}

    public record RepositoryMethodSpec(
            String by,
            String parameter,
            String returns,
            boolean required
    ) {}

    public record DomainServiceSpec(
            String description,
            Map<String, String> inputs,
            String output,
            String implementation
    ) {}

    public record HookSpec(String type, String method) {}

    public record NamedTypeSpec(String name, String type) {}

    public record TestsSpec(TestGenerationSpec generate, Map<String, Object> fixtures) {}

    public record TestGenerationSpec(
            DomainTestSpec domain,
            RepositoryTestSpec repositories,
            SerializationTestSpec serialization
    ) {}

    public record DomainTestSpec(
            boolean valueObjects,
            boolean invariants,
            boolean stateMachine,
            boolean commands,
            boolean events
    ) {}

    public record RepositoryTestSpec(boolean contractTests) {}

    public record SerializationTestSpec(boolean events) {}

    public record GenerationSpec(
            OverwriteSpec overwrite,
            boolean failOnManualChangesInGeneratedCode,
            TraceabilitySpec traceability
    ) {}

    public record OverwriteSpec(boolean generated, boolean custom) {}

    public record TraceabilitySpec(boolean includeSpecReferenceInGeneratedCode) {}
}
