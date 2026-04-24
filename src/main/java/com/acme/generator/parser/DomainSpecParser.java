package com.acme.generator.parser;

import com.acme.generator.model.*;
import com.acme.generator.model.Models.*;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses a domain DSL YAML file into a {@link DomainSpec}.
 */
@SuppressWarnings("unchecked")
public class DomainSpecParser {

    public DomainSpec parse(Path yamlFile) throws Exception {
        try (InputStream is = Files.newInputStream(yamlFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            return parseRoot(root);
        }
    }

    // ── Root ──────────────────────────────────────────────────────────────────

    private DomainSpec parseRoot(Map<String, Object> root) {
        return new DomainSpec(
                parseDomain(map(root, "domain")),
                list(root, "imports"),
                parseValueObjects(map(root, "valueObjects")),
                parseEnums(map(root, "enums")),
                parseEntities(map(root, "entities")),
                parseAggregates(map(root, "aggregates")),
                parseEvents(map(root, "events")),
                parseRepositories(map(root, "repositories")),
                parseDomainServices(map(root, "domainServices")),
                parseTests(map(root, "tests")),
                parseGeneration(map(root, "generation"))
        );
    }

    // ── Domain ────────────────────────────────────────────────────────────────

    private DomainInfo parseDomain(Map<String, Object> m) {
        if (m == null) throw new IllegalArgumentException("'domain' section is required");
        Map<String, Object> bc = map(m, "boundedContext");
        return new DomainInfo(
                str(m, "name"),
                str(m, "package"),
                str(m, "version"),
                str(m, "description"),
                bc == null ? null : new BoundedContextInfo(str(bc, "name"), str(bc, "description"))
        );
    }

    // ── Value Objects ─────────────────────────────────────────────────────────

    private Map<String, ValueObjectSpec> parseValueObjects(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> parseValueObject((Map<String, Object>) e.getValue())
        ));
    }

    private ValueObjectSpec parseValueObject(Map<String, Object> m) {
        return new ValueObjectSpec(
                str(m, "type"),
                str(m, "description"),
                boolOr(m, "immutable", true),
                str(m, "generation"),
                parseFields(map(m, "fields")),
                parseConstraintsList(m),
                parseInvariants(list(m, "invariants"))
        );
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    private Map<String, EnumSpec> parseEnums(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Map<String, Object> em = (Map<String, Object>) e.getValue();
                    return new EnumSpec(str(em, "description"), list(em, "values"));
                }
        ));
    }

    // ── Entities ──────────────────────────────────────────────────────────────

    private Map<String, EntitySpec> parseEntities(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> parseEntity((Map<String, Object>) e.getValue())
        ));
    }

    private EntitySpec parseEntity(Map<String, Object> m) {
        return new EntitySpec(
                str(m, "belongsTo"),
                str(m, "description"),
                parseNamedType(map(m, "identity")),
                parseFields(map(m, "fields")),
                parseInvariants(list(m, "invariants"))
        );
    }

    // ── Aggregates ────────────────────────────────────────────────────────────

    private Map<String, AggregateSpec> parseAggregates(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> parseAggregate((Map<String, Object>) e.getValue())
        ));
    }

    private AggregateSpec parseAggregate(Map<String, Object> m) {
        return new AggregateSpec(
                boolOr(m, "root", true),
                str(m, "description"),
                parseNamedType(map(m, "id")),
                parseFields(map(m, "fields")),
                parseInvariants(list(m, "invariants")),
                parseCommands(map(m, "commands")),
                parseStateMachine(map(m, "stateMachine")),
                parseHooks(map(m, "hooks"))
        );
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private Map<String, FieldSpec> parseFields(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> parseField((Map<String, Object>) e.getValue())
        ));
    }

    private FieldSpec parseField(Map<String, Object> m) {
        return new FieldSpec(
                str(m, "type"),
                str(m, "itemType"),
                str(m, "description"),
                boolOr(m, "nullable", false),
                m.get("default"),
                str(m, "generation"),
                parseConstraints(map(m, "constraints"))
        );
    }

    // ── Constraints ───────────────────────────────────────────────────────────

    private List<ConstraintSpec> parseConstraintsList(Map<String, Object> m) {
        // constraints at valueObject level is an object (same shape), wrap in list
        Map<String, Object> cm = map(m, "constraints");
        if (cm == null) return List.of();
        return List.of(parseConstraints(cm));
    }

    private ConstraintSpec parseConstraints(Map<String, Object> m) {
        if (m == null) return null;
        return new ConstraintSpec(
                (Boolean) m.get("required"),
                (Number) m.get("min"),
                (Number) m.get("max"),
                (Integer) m.get("minLength"),
                (Integer) m.get("maxLength"),
                (Integer) m.get("minSize"),
                (Integer) m.get("maxSize"),
                str(m, "format"),
                str(m, "pattern"),
                (List<Object>) m.get("enum")
        );
    }

    // ── Invariants ────────────────────────────────────────────────────────────

    private List<InvariantSpec> parseInvariants(List<Object> list) {
        if (list == null) return List.of();
        return list.stream().map(o -> {
            Map<String, Object> m = (Map<String, Object>) o;
            return new InvariantSpec(str(m, "name"), str(m, "when"), str(m, "expression"), str(m, "message"));
        }).toList();
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    private Map<String, CommandSpec> parseCommands(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> parseCommand((Map<String, Object>) e.getValue())
        ));
    }

    private CommandSpec parseCommand(Map<String, Object> m) {
        return new CommandSpec(
                boolOr(m, "factory", false),
                str(m, "description"),
                (Map<String, String>) m.get("parameters"),
                list(m, "preconditions"),
                (Map<String, Object>) m.get("sets"),
                list(m, "emits"),
                list(m, "hooks")
        );
    }

    // ── State Machine ─────────────────────────────────────────────────────────

    private StateMachineSpec parseStateMachine(Map<String, Object> m) {
        if (m == null) return null;
        List<Object> tList = list(m, "transitions");
        List<TransitionSpec> transitions = tList == null ? List.of() : tList.stream().map(o -> {
            Map<String, Object> t = (Map<String, Object>) o;
            return new TransitionSpec(str(t, "from"), str(t, "to"), str(t, "command"));
        }).toList();
        return new StateMachineSpec(str(m, "field"), str(m, "initial"), list(m, "terminal"), transitions);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    private Map<String, EventSpec> parseEvents(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Map<String, Object> em = (Map<String, Object>) e.getValue();
                    return new EventSpec(
                            str(em, "aggregate"),
                            intOr(em, "version", 1),
                            str(em, "description"),
                            (Map<String, String>) em.get("fields")
                    );
                }
        ));
    }

    // ── Repositories ──────────────────────────────────────────────────────────

    private Map<String, RepositorySpec> parseRepositories(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Map<String, Object> rm = (Map<String, Object>) e.getValue();
                    Map<String, Object> methods = map(rm, "methods");
                    Map<String, RepositoryMethodSpec> parsedMethods = methods == null ? Map.of() :
                            methods.entrySet().stream().collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    me -> {
                                        Map<String, Object> mm = (Map<String, Object>) me.getValue();
                                        return new RepositoryMethodSpec(
                                                str(mm, "by"),
                                                str(mm, "parameter"),
                                                str(mm, "returns"),
                                                boolOr(mm, "required", false)
                                        );
                                    }
                            ));
                    return new RepositorySpec(str(rm, "aggregate"), parsedMethods);
                }
        ));
    }

    // ── Domain Services ───────────────────────────────────────────────────────

    private Map<String, DomainServiceSpec> parseDomainServices(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Map<String, Object> sm = (Map<String, Object>) e.getValue();
                    return new DomainServiceSpec(
                            str(sm, "description"),
                            (Map<String, String>) sm.get("inputs"),
                            str(sm, "output"),
                            str(sm, "implementation")
                    );
                }
        ));
    }

    // ── Hooks ─────────────────────────────────────────────────────────────────

    private Map<String, HookSpec> parseHooks(Map<String, Object> m) {
        if (m == null) return Map.of();
        return m.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    Map<String, Object> hm = (Map<String, Object>) e.getValue();
                    return new HookSpec(str(hm, "type"), str(hm, "method"));
                }
        ));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    private TestsSpec parseTests(Map<String, Object> m) {
        if (m == null) return null;
        Map<String, Object> gen = map(m, "generate");
        if (gen == null) return new TestsSpec(null, (Map<String, Object>) m.get("fixtures"));

        Map<String, Object> dom = map(gen, "domain");
        Map<String, Object> rep = map(gen, "repositories");
        Map<String, Object> ser = map(gen, "serialization");

        return new TestsSpec(
                new TestGenerationSpec(
                        dom == null ? null : new DomainTestSpec(
                                boolOr(dom, "valueObjects", false),
                                boolOr(dom, "invariants", false),
                                boolOr(dom, "stateMachine", false),
                                boolOr(dom, "commands", false),
                                boolOr(dom, "events", false)
                        ),
                        rep == null ? null : new RepositoryTestSpec(boolOr(rep, "contractTests", false)),
                        ser == null ? null : new SerializationTestSpec(boolOr(ser, "events", false))
                ),
                (Map<String, Object>) m.get("fixtures")
        );
    }

    // ── Generation ────────────────────────────────────────────────────────────

    private GenerationSpec parseGeneration(Map<String, Object> m) {
        if (m == null) return null;
        Map<String, Object> ow = map(m, "overwrite");
        Map<String, Object> tr = map(m, "traceability");
        return new GenerationSpec(
                ow == null ? null : new OverwriteSpec(boolOr(ow, "generated", true), boolOr(ow, "custom", false)),
                boolOr(m, "failOnManualChangesInGeneratedCode", false),
                tr == null ? null : new TraceabilitySpec(boolOr(tr, "includeSpecReferenceInGeneratedCode", false))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NamedTypeSpec parseNamedType(Map<String, Object> m) {
        if (m == null) return null;
        return new NamedTypeSpec(str(m, "name"), str(m, "type"));
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private boolean boolOr(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    private int intOr(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v instanceof Map<?, ?> ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v instanceof List<?> ? (List<T>) v : null;
    }
}
