package org.cthul.quarkus.dynamicmessages;

import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MessageBackup {

    private final Path path;

    public MessageBackup(Path path) {
        this.path = path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void write(Map<String, ?> messages) {
        var json = new JsonObject((Map) messages);
        try {
            Files.writeString(path, json.encodePrettily());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Map<String, T> read() {
        try {
            var json = new JsonObject(Files.readString(path));
            return (Map) json.getMap();
        } catch (IOException e) {
            Log.infof(e, "Failed to read messages from %s", path);
            return Map.of();
        }
    }
}
