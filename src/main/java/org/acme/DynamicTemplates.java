package org.acme;

import io.quarkus.logging.Log;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class DynamicTemplates implements TemplateLocator {

    @Inject
    Engine qute;

    private final Map<String, String> messages = new HashMap<>();

    void init(@Observes Startup ev) {
        qute.removeTemplates(name -> {
            if (name.startsWith("msg_")) Log.infof("Found %s", name);
            return false;
        });
    }

    public void update(String locale, String key, String value) {
        var id = "msg_" + (locale.equals("_") ? "" : locale + "_") + key;
        messages.put(id, value);
        qute.removeTemplates(id::equals);
        Log.infof("Updated template: %s", id);
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public Optional<TemplateLocation> locate(String id) {
        var value = messages.get(id);
        if (value == null) return Optional.empty();
        Log.infof("Loading template: %s", id);
        return Optional.of(new StringTemplate(value));
    }

    record StringTemplate(String value) implements TemplateLocation {

        @Override
        public Reader read() {
            return new StringReader(value);
        }

        @Override
        public Optional<Variant> getVariant() {
            return Optional.empty();
        }
    }
}
