package org.cthul.quarkus.dynamicmessages;

import java.util.ArrayList;
import java.util.List;

public class KeyRename {

    private final List<BundlePrefix> bundlePrefixes = new ArrayList<>();
    private final List<String> locales = new ArrayList<>(List.of("default", "test", "en", "de", "fr", "nl"));
    private char nameSeparator = '-';
    private char keySeparator = '.';

    public KeyRename() {
    }

    public KeyRename(char nameSeparator, char keySeparator) {
        this.nameSeparator = nameSeparator;
        this.keySeparator = keySeparator;
    }

    public void addBundlePrefix(String bundle, String namespace, String prefix) {
        bundlePrefixes.add(new BundlePrefix(bundle, namespace, prefix));
    }

    public void setSeparators(char nameSeparator, char keySeparator) {
        this.nameSeparator = nameSeparator;
        this.keySeparator = keySeparator;
    }

    public MappedKey mapKey(String locale, String namespace, String key) {
        for (var bp: bundlePrefixes) {
            if (bp.namespace.equals(namespace) && startsWith(key, bp.prefix, keySeparator)) {
                var n = bp.prefix.length();
                if (n > 0 && n < key.length()) n++;
                var name = key.substring(n).replace(keySeparator, nameSeparator);
                return new MappedKey(bp.bundle, bp.namespace, locale, name, key);
            }
        }
        return null;
    }

    public MappedKey mapMessage(String messageId) {
        for (var bp: bundlePrefixes) {
            if (startsWith(messageId, bp.bundle, '_')) {
                var n = bp.bundle.length() + 1;
                var nextSep = messageId.indexOf("_", n) + 1;
                var locale = "";
                if (nextSep > n) {
                    locale = messageId.substring(n, nextSep - 1);
                    if (!locales.contains(locale)) {
                        nextSep = n;
                        locale = "";
                    }
                } else {
                    nextSep = n;
                }
                var name = messageId.substring(nextSep);
                var key = (bp.prefix.isBlank() ? "" : bp.prefix + keySeparator) + name.replace(nameSeparator, keySeparator);
                return new MappedKey(bp.bundle, bp.namespace, locale, name, key);
            }
        }
        return null;
    }

    private boolean startsWith(String string, String prefix, char sep) {
        if (!string.startsWith(prefix)) return false;
        if (string.length() == prefix.length() || prefix.isBlank()) return true;
        return string.charAt(prefix.length()) == sep;
    }

    public record BundlePrefix(String bundle, String namespace, String prefix) {
    }

    public record MappedKey(String bundle, String namespace, String locale, String name, String key) {

        public String messageId() {
            return messageId("");
        }

        public String messageId(String defaultLocale) {
            return bundle + "_" + (isLocale(locale) && !locale.equals(defaultLocale) ? locale + "_" : "") + name;
        }
    }

    private static boolean isLocale(String locale) {
        return locale != null && !locale.isBlank() && !"_".equals(locale) && !"default".equals(locale);
    }
}
