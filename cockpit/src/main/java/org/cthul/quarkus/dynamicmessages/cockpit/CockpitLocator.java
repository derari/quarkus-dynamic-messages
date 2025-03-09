package org.cthul.quarkus.dynamicmessages.cockpit;

import com.fasterxml.jackson.annotation.JsonValue;
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
    private final CockpitData data = new CockpitData();
    private long lastRefresh = 0;

    @PostConstruct
    void init() {
        config.backupFile().ifPresent(file -> {
            backup = new MessageBackup(Path.of(file));
            data.putAll(backup.read());
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
            lastRefresh = System.currentTimeMillis();
            var reset = new HashSet<String>();
            projects.forEach(project -> {
                var data = client.lokalize(project, config.token().orElse(""));
                refresh(project, data.getTranslations(), reset);
            });
            Log.debugf("Refreshed localizations: %s -> %s", projects, reset);
            if (reset.isEmpty()) return false;
            if (backup != null) {
                backup.write(data.asMap());
            }
            dynamicTemplates.resetAll(reset);
            return true;
        } catch (Exception e) {
            Log.error("Failed to refresh localizations", e);
            return false;
        }
    }

    private synchronized void refresh(String project, Map<String, Map<String, String>> data, Set<String> reset) {
        var existing = this.data.project(project);
        data.forEach((lang, translations) -> {
            var existingLang = existing.language(lang);
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
        var projectData = data.peekProject(project);
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

    private static String get(Localizations projectData, String locale, String key) {
        var translations = projectData.language(locale);
        var result = translations.get(key);
        if (result != null && !result.isEmpty()) {
            return result;
        }
        return null;
    }

    public record CockpitData(ProjectLocalizations localizations, Collections collections) {

        public CockpitData() {
            this(new ProjectLocalizations(), new Collections());
        }

        public Localizations project(String key) {
            return localizations.project(key);
        }

        public Localizations peekProject(String key) {
            return localizations.peekProject(key);
        }

        public Collection collection(String key) {
            return collections.collection(key);
        }

        public void putAll(Map<String, Object> data) {
            data.forEach((project, value) -> {
                if (value instanceof Map<?, ?> map) {
                    if (project.equals("collections")) {
                        collections.putAll(map);
                    } else {
                        localizations.project(project).putAll(map);
                    }
                }
            });
        }

        public Map<String, Object> asMap() {
            var map = new HashMap<String, Object>(localizations.asMap());
            map.put("collections", collections.asMap());
            return map;
        }
    }

    public record ProjectLocalizations(@JsonValue Map<String, Localizations> projects) {

        public ProjectLocalizations() {
            this(new HashMap<>());
        }

        public Localizations project(String key) {
            return projects.computeIfAbsent(key, k -> new Localizations());
        }

        public Localizations peekProject(String key) {
            return projects.get(key);
        }

        public Map<String, Map<String, Map<String, String>>> asMap() {
            return projects.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asMap()));
        }
    }

    public record Localizations(@JsonValue Map<String, KeyValues> translations) {

        public Localizations() {
            this(new HashMap<>());
        }

        public KeyValues language(String key) {
            return translations.computeIfAbsent(key, k -> new KeyValues());
        }

        public void putAll(Map<?, ?> data) {
            data.forEach((key, value) -> {
                if (key instanceof String lang
                        && value instanceof Map<?, ?> map) {
                    language(lang).putAll(map);
                }
            });
        }

        public Map<String, Map<String, String>> asMap() {
            return translations.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().values));
        }
    }

    public record Collections(Map<String, Collection> collections) {

        public Collections() {
            this(new HashMap<>());
        }

        public Collection collection(String key) {
            return collections.computeIfAbsent(key, k -> new Collection());
        }

        public void putAll(Map<?, ?> data) {
            data.forEach((key, value) -> {
                if (key instanceof String c
                        && value instanceof Map<?, ?> map) {
                    collection(c).putAll(map);
                }
            });
        }

        public Map<String, Map<String, List<Map<String, String>>>> asMap() {
            return collections.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asMap()));
        }
    }

    public record Collection(Map<String, LocalizedCollection> translations) {

        public Collection() {
            this(new HashMap<>());
        }

        public LocalizedCollection language(String key) {
            return translations.computeIfAbsent(key, k -> new LocalizedCollection());
        }

        public void putAll(Map<?, ?> data) {
            data.forEach((key, value) -> {
                if (key instanceof String lang
                        && value instanceof List<?> map) {
                    language(lang).addAll(map);
                }
            });
        }

        public Map<String, List<Map<String, String>>> asMap() {
            return translations.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asList()));
        }
    }

    public record LocalizedCollection(List<KeyValues> items) {

        public LocalizedCollection() {
            this(new ArrayList<>());
        }

        public void addAll(List<?> data) {
            data.forEach(item -> {
                if (item instanceof Map<?, ?> map) {
                    add(map);
                }
            });
        }

        public void add(Map<?, ?> data) {
            var kv = new KeyValues();
            kv.putAll(data);
            items.add(kv);
        }

        public List<Map<String, String>> asList() {
            return items.stream().map(KeyValues::values).collect(Collectors.toList());
        }
    }

    public record KeyValues(@JsonValue Map<String, String> values) {

        public KeyValues() {
            this(new HashMap<>());
        }

        public String get(String key) {
            return values.get(key);
        }

        public void putAll(Map<?, ?> data) {
            data.forEach((key, value) -> {
                if (key instanceof String k
                        && value instanceof String v) {
                    values.put(k, v);
                }
            });
        }

        public String put(String key, String value) {
            return values.put(key, value);
        }
    }
}
