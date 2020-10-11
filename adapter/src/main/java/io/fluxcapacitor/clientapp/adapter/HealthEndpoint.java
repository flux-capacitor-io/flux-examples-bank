package io.fluxcapacitor.clientapp.adapter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("")
@Produces(MediaType.TEXT_HTML)
public class HealthEndpoint {

    @GET
    @Path("/health")
    public String healthCheck() {
        return "healthy";
    }
}
