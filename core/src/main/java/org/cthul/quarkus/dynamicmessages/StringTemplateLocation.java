package org.cthul.quarkus.dynamicmessages;

import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;

import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

public record StringTemplateLocation(String value, Variant varian) implements TemplateLocator.TemplateLocation {

    public StringTemplateLocation(String value) {
        this(value, null);
    }

    @Override
    public Reader read() {
        return new StringReader(value);
    }

    @Override
    public Optional<Variant> getVariant() {
        return Optional.ofNullable(varian);
    }
}
