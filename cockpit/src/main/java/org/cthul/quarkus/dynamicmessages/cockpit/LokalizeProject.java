package org.cthul.quarkus.dynamicmessages.cockpit;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.HashMap;
import java.util.Map;

@RegisterForReflection
public class LokalizeProject {

    private Map<String, Key> keys;
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    public void setKey(Map<String, Key> keys) {
        this.keys = keys;
    }

    @JsonProperty("key")
    public Map<String, Key> getKey() {
        return keys;
    }

    @JsonAnySetter
    public void setTranslations(String language, Map<String, String> translations) {
        this.translations.put(language, translations);
    }

    @JsonAnyGetter
    public Map<String, Map<String, String>> getTranslations() {
        return translations;
    }

    public record Key(String value) { }
}
