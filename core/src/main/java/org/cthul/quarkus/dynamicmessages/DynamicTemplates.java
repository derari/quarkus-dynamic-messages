package org.cthul.quarkus.dynamicmessages;

import io.quarkus.arc.All;
import io.quarkus.logging.Log;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.runtime.MessageBundleRecorder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.*;

@ApplicationScoped
public class DynamicTemplates implements TemplateLocator {

    @Inject
    Engine qute;

    @Inject
    MessageBundleRecorder.BundleContext ctx;

    @Inject
    Instance<DynamicTemplateLocator> locators;

    void init(@Observes Startup ev) {
        var messageKeys = new HashSet<>(ctx.getMessageTemplates().keySet());
        messageKeys.forEach(k -> Log.infof("Found key %s", k));
        locators.stream().forEach(locator -> locator.initialize(messageKeys));
    }

    public void reset(String key) {
        qute.removeTemplates(key::equals);
    }

    public void resetAll(Set<String> key) {
        qute.removeTemplates(key::contains);
    }

    public void reset(String bundle, String locale, String key) {
        var id = bundle + "_" + (isLocale(locale) ? "" : locale + "_") + key;
        qute.removeTemplates(id::equals);
    }

    private boolean isLocale(String locale) {
        return locale != null && !locale.isBlank() && !"_".equals(locale);
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public Optional<TemplateLocation> locate(String id) {
        return locators.stream().toList().stream()
                .map(locator -> locator.locate(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

}
