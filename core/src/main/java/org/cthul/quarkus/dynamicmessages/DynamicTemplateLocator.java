package org.cthul.quarkus.dynamicmessages;

import io.quarkus.qute.TemplateLocator;

import java.util.Optional;
import java.util.Set;

public interface DynamicTemplateLocator {

    void initialize(Set<String> messageIds);

    Optional<TemplateLocator.TemplateLocation> locate(String id);
}
