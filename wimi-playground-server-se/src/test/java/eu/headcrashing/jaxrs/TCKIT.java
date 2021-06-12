package eu.headcrashing.jaxrs;

import static jakarta.ws.rs.SeBootstrap.Configuration.FREE_PORT;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Timeout(value = 1, unit = HOURS)
public final class TCKIT {

    @Test
	public final void shouldNotFail() {
        // given
        final int expectedResponse = mockInt();
        final Application application = new StaticApplication(expectedResponse);
        final UriBuilder baseUri = UriBuilder.newInstance().scheme("http").host("localhost").port(FREE_PORT);

		final ResourceConfig resourceConfigFromClasses = ResourceConfig.forApplication(application);
		final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri.build(), resourceConfigFromClasses);
		final int actualPort = httpServer.getListener("grizzly").getPort();

		final UriBuilder effectiveUri = baseUri.port(actualPort).path("resource");
		try (final Client client = ClientBuilder.newClient()) {
			// when
			final int actualResponse = client.target(effectiveUri).request().get(int.class);
			
			// then
	        assertThat(actualResponse, is(expectedResponse));			
		}

		httpServer.shutdown(1, SECONDS);
	}

    public static final class StaticApplication extends Application {

        private final StaticResource staticResource;

        private StaticApplication(final long staticResponse) {
            this.staticResource = new StaticResource(staticResponse);
        }

        @Override
        public final Set<Object> getSingletons() {
            return Collections.<Object>singleton(this.staticResource);
        }

        @Path("resource")
        public static final class StaticResource {

            private final long staticResponse;

            private StaticResource(final long staticResponse) {
                this.staticResponse = staticResponse;
            }

            @GET
            public final long staticResponse() {
                return this.staticResponse;
            }
        }
    };

    private static final int mockInt() {
        return (int) Math.round(Integer.MAX_VALUE * Math.random());
    }

}
