package org.cthul.quarkus.dynamicmessages.cockpit;

import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@ConnectWireMock
class CockpitLocatorTest {

    @Inject
    MyMessages messages;

    @Localized("de")
    MyMessages deMessages;

    @Test
    void defaultLang() {
        var string = messages.theKey("Alice");
        assertEquals("Hi Alice", string);
    }

    @Test
    void de() {
        var string = deMessages.theKey("Alice");
        assertEquals("Guten Tag", string);
    }
}