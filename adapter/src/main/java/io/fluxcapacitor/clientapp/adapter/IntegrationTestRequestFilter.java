package io.fluxcapacitor.clientapp.adapter;

import io.fluxcapacitor.javaclient.FluxCapacitor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Priority(0)
@Provider
@Slf4j
public class IntegrationTestRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final Map<String, FluxCapacitor> fluxCapacitorByRequestId = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        FluxCapacitor.instance.set(fluxCapacitorByRequestId.remove(requestContext.getHeaderString("requestId")));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        FluxCapacitor.instance.remove();
    }
}
