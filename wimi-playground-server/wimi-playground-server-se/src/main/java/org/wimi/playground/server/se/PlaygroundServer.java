package org.wimi.playground.server.se;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.wimi.playground.server.core.resources.HelloWorld;

public final class PlaygroundServer {

    public static void main(final String[] args) throws InterruptedException {
        final URI baseUri = UriBuilder.fromUri("").scheme("http").host("localhost").port(9091).build();
        final ResourceConfig resourceConfigFromClasses = new ResourceConfig(HelloWorld.class);

        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfigFromClasses);
        final int actualPort = httpServer.getListener("grizzly").getPort();

        System.out.printf("Playground Server running on port %d - Send SIGKILL to shutdown.%n", actualPort);

        Thread.currentThread().join();
    }

}
