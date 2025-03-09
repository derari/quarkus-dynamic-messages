package org.cthul.quarkus.dynamicmessages.cockpit;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "cockpit")
public interface CockpitConfig {

    String DEFAULT_MAPPING = "<default>";

    @WithDefault("")
    Optional<String> token();

    @WithDefault("")
    Optional<String> backupFile();

    @WithParentName
    @WithUnnamedKey(DEFAULT_MAPPING)
    @ConfigDocMapKey("mapping-name")
    Map<String, ProjectConfig> mappings();

    @WithParentName
    @WithUnnamedKey(DEFAULT_MAPPING)
    @ConfigDocMapKey("mapping-name")
    Map<String, CollectionConfig> collections();

    interface ProjectConfig {

        @WithDefault("")
        Optional<String> project();

        @WithDefault("")
        Optional<String> prefix();

        @WithDefault("")
        Optional<String> bundle();

        @WithDefault("")
        Optional<String> path();
    }

    interface CollectionConfig {

        @WithDefault("")
        Optional<String> project();

    }
}
