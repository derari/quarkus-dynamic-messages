package org.cthul.quarkus.dynamicmessages.cockpit;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle(value = "my_items", defaultKey = Message.HYPHENATED_ELEMENT_NAME, locale = "default")
public interface MyItems {
}
