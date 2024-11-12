package org.cthul.quarkus.dynamicmessages;

import io.quarkus.qute.TemplateLocator;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class MyDynamicLocator implements DynamicTemplateLocator {

    @Override
    public void initialize(Set<String> messageIds) {
    }

    @Override
    public Optional<TemplateLocator.TemplateLocation> locate(String id) {
        return Optional.of(new StringTemplateLocation("Hello {name}"));
    }
}
