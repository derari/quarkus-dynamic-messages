package org.cthul.quarkus.dynamicmessages;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DynamicTemplatesTest {

    @Inject
    DynamicTemplates instance;

    @Test
    void reset() {
        instance.reset("msg", null, "bob");
    }
}
