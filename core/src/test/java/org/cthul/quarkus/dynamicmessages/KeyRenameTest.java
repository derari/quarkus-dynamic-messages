package org.cthul.quarkus.dynamicmessages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyRenameTest {

    KeyRename instance = new KeyRename();

    @Test
    void mapMessage() {
        instance.setSeparators('_', '.');
        instance.addBundlePrefix("bundle", "ns", "prefix");
        var mapped = instance.mapMessage("bundle_de_foo_bar");

        assertEquals("bundle", mapped.bundle());
        assertEquals("de", mapped.locale());
        assertEquals("prefix.foo.bar", mapped.key());
        assertEquals("bundle_de_foo_bar", mapped.messageId("en"));
        assertEquals("bundle_foo_bar", mapped.messageId("de"));
    }

    @Test
    void mapMessage_noPrefix() {
        instance.setSeparators('_', '.');
        instance.addBundlePrefix("bundle", "ns", "");
        var mapped = instance.mapMessage("bundle_de_foo_bar");

        assertEquals("bundle", mapped.bundle());
        assertEquals("de", mapped.locale());
        assertEquals("foo.bar", mapped.key());
        assertEquals("bundle_de_foo_bar", mapped.messageId("en"));
        assertEquals("bundle_foo_bar", mapped.messageId("de"));
    }

    @Test
    void mapMessage_noLocale() {
        instance.setSeparators('_', '.');
        instance.addBundlePrefix("bundle", "ns", "prefix");
        var mapped = instance.mapMessage("bundle_foo_bar");

        assertEquals("bundle", mapped.bundle());
        assertEquals("", mapped.locale());
        assertEquals("prefix.foo.bar", mapped.key());
        assertEquals("bundle_foo_bar", mapped.messageId());
    }

    @Test
    void mapKey() {
        instance.setSeparators('_', '.');
        instance.addBundlePrefix("bundle", "ns", "prefix");
        var mapped = instance.mapKey("de", "ns", "prefix.foo.bar");

        assertEquals("bundle", mapped.bundle());
        assertEquals("de", mapped.locale());
        assertEquals("prefix.foo.bar", mapped.key());
        assertEquals("bundle_de_foo_bar", mapped.messageId("en"));
        assertEquals("bundle_foo_bar", mapped.messageId("de"));
    }

    @Test
    void mapKey_noPrefix() {
        instance.setSeparators('_', '.');
        instance.addBundlePrefix("bundle", "ns", "");
        var mapped = instance.mapKey("de", "ns", "foo.bar");

        assertEquals("bundle", mapped.bundle());
        assertEquals("de", mapped.locale());
        assertEquals("foo.bar", mapped.key());
        assertEquals("bundle_de_foo_bar", mapped.messageId("en"));
        assertEquals("bundle_foo_bar", mapped.messageId("de"));
    }
}