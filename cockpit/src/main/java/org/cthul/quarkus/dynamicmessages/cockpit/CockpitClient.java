package org.cthul.quarkus.dynamicmessages.cockpit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;

@RegisterRestClient(configKey = "cockpit-client")
public interface CockpitClient {

    @GET
    @Path("api/lokalize/project/{project}")
    LokalizeProject lokalize(@RestPath String project, @RestHeader("api-key") String token);
}
