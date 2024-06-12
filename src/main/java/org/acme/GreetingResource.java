package org.acme;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.MessageBundles;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestPath;

@Path("/hello")
public class GreetingResource {

    @Inject
    Template page;

    @Inject
    DynamicTemplates dynamicTemplates;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{locale}/{name}")
    public TemplateInstance hello(@RestPath String locale, @RestPath String name) {
        return page.data("name", name).setLocale(locale);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{locale}")
    public String ok(@RestPath String locale) {
        try {
            return MessageBundles.get(MyMessages.class, Localized.Literal.of(locale)).ok();
        } catch (IllegalStateException ex) {
            return MessageBundles.get(MyMessages.class).ok();
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("set/{locale}/{key}/{value}")
    public String set(@RestPath String locale, @RestPath String key, @RestPath String value) {
        dynamicTemplates.update(locale, key, value);
        return value;
    }
}
