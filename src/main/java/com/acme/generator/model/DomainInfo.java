package com.acme.generator.model;

import java.util.List;
import java.util.Map;

public record DomainInfo(
        String name,
        String pkg,           // mapped from "package" (reserved keyword)
        String version,
        String description,
        BoundedContextInfo boundedContext
) {}
