package org.cthul.quarkus.dynamicmessages;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface MyMessages {

    @Message("Hello {name}")
    String hello_name(String name);

    @Message("{!!}Okkk!")
    String ok();
}
