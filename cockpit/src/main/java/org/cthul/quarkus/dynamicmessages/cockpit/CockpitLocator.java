package org.cthul.quarkus.dynamicmessages.cockpit;

import io.quarkus.logging.Log;
import io.quarkus.qute.TemplateLocator;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.cthul.quarkus.dynamicmessages.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class CockpitLocator implements DynamicTemplateLocator {

    @Inject
    CockpitConfig config;

    @Inject
    @RestClient
    CockpitClient client;

    @Inject
    DynamicTemplates dynamicTemplates;

    private MessageBackup backup = null;
    private final KeyRename bundles = new KeyRename();
    private final KeyRename templates = new KeyRename('_', '.');
    private final Set<String> projects = new HashSet<>();
    private final Map<String, Map<String, Map<String, String>>> projectLocalizations = new HashMap<>();
    private long lastRefresh = 0;

    @PostConstruct
    void init() {
        config.backupFile().ifPresent(file -> {
            backup = new MessageBackup(Path.of(file));
            projectLocalizations.putAll(backup.read());
        });
        config.mappings().forEach((name, mapping) -> {
            var project = mapping.project().orElse(name);
            if (project.equals(CockpitConfig.DEFAULT_MAPPING)) return;
            projects.add(project);
            var prefix = mapping.prefix().orElse("");
            var bundle = mapping.bundle().orElse(name);
            if (!bundle.equals(CockpitConfig.DEFAULT_MAPPING)) bundles.addBundlePrefix(bundle, project, prefix);
            var path = mapping.path().orElse(name);
            if (!path.equals(CockpitConfig.DEFAULT_MAPPING)) templates.addBundlePrefix(path, project, prefix);
        });
        autoRefresh();
    }

    @Override
    public void initialize(Set<String> messageIds) {
        if (autoRefresh()) return;
        var matches = messageIds.stream()
                .filter(id -> locate(bundles.mapMessage(id)) != null)
                .collect(Collectors.toSet());
        dynamicTemplates.resetAll(matches);
    }

    public boolean autoRefresh() {
        if (System.currentTimeMillis() - lastRefresh > 15 * 60 * 1000) {
            return refresh();
        }
        return true;
    }

    public boolean refresh() {
        try {
            var reset = new HashSet<String>();
            projects.forEach(project -> {
                var data = client.lokalize(project, config.token().orElse(""));
                refresh(project, data.getTranslations(), reset);
            });
            Log.debugf("Refreshed localizations: %s -> %s", projects, reset);
            lastRefresh = System.currentTimeMillis();
            if (reset.isEmpty()) return false;
            if (backup != null) {
                backup.write(projectLocalizations);
            }
            dynamicTemplates.resetAll(reset);
            return true;
        } catch (Exception e) {
            Log.error("Failed to refresh localizations", e);
            return false;
        }
    }

    private synchronized void refresh(String project, Map<String, Map<String, String>> data, Set<String> reset) {
        var existing = projectLocalizations.computeIfAbsent(project, p -> new HashMap<>());
        data.forEach((lang, translations) -> {
            var existingLang = existing.computeIfAbsent(lang, l -> new HashMap<>());
            translations.forEach((key, value) -> {
                var existingValue = existingLang.put(key, value);
                if (!Objects.equals(existingValue, value)) {
                    var bundleId = bundles.mapKey(lang, project, key);
                    if (bundleId != null) {
                        reset.add(bundleId.messageId());
                        reset.add(bundleId.messageId("en"));
                    }
                    var templateId = templates.mapKey(lang, project, key);
                    if (templateId != null) {
                        reset.add(templateId.messageId());
                        reset.add(templateId.messageId("en"));
                    }
                }
            });
        });
    }

    @Override
    public Optional<TemplateLocator.TemplateLocation> locate(String id) {
        return Optional.ofNullable(templates.mapMessage(id))
                .or(() -> Optional.ofNullable(bundles.mapMessage(id)))
                .map(this::locate)
                .map(StringTemplateLocation::new);
    }

    private String locate(KeyRename.MappedKey mapped) {
        if (mapped == null) return null;
        return get(mapped.namespace(), mapped.locale(), mapped.key());
    }

    public synchronized String get(String project, String locale, String key) {
        autoRefresh();
        if (locale.isBlank()) locale = "default";
        var projectData = projectLocalizations.get(project);
        if (projectData == null) return null;
        var result = get(projectData, locale, key);
        if (result != null) {
            Log.debugf("Found localization: %s:%s:%s", project, locale, key);
            return result;
        }
        if (!locale.equals("default") && get(projectData, "default", key) != null) {
            Log.debugf("Found default localization: %s:%s:%s", project, locale, key);
            var ref = templates.mapKey("default", project, key);
            return "{#include %s}".formatted(ref.messageId("default"));
        }
        Log.debugf("Localization not found: %s:%s:%s", project, locale, key);
        return null;
    }

    private static String get(Map<String, Map<String, String>> projectData, String locale, String key) {
        var translations = projectData.get(locale);
        if (translations != null) {
            var result = translations.get(key);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        return null;
    }
}
