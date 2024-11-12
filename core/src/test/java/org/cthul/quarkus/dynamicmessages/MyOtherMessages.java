package org.cthul.quarkus.dynamicmessages;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle(value = "other", defaultKey = Message.UNDERSCORED_ELEMENT_NAME)
public interface MyOtherMessages {

    @Message("Hello {name}")
    String helloName(String name);

    @Message("{!!}Okkk!")
    String ok();
}
