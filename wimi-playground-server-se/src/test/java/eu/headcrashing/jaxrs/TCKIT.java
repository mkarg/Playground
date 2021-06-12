package eu.headcrashing.jaxrs;

import static jakarta.ws.rs.RuntimeType.CLIENT;
import static jakarta.ws.rs.RuntimeType.SERVER;
import static jakarta.ws.rs.SeBootstrap.Configuration.FREE_PORT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
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
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.core.UriBuilder;
import java.lang.reflect.Type;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.Produces;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

@Timeout(value = 1, unit = HOURS)
public final class TCKIT {

	private static final Logger rootLogger() {
		Logger logger = Logger.getGlobal();
		while (logger.getParent() != null)
			logger = logger.getParent();
		return logger;
	}

	private static final Handler consoleHandler() {
		final Handler[] handlers = rootLogger().getHandlers();
		for (final Handler handler : handlers)
			if (handler instanceof ConsoleHandler)
				return handler;
		return new ConsoleHandler();
	}

	@Test
	public final void shouldNotFail() throws Throwable {
		rootLogger().setLevel(Level.INFO);
		consoleHandler().setLevel(Level.ALL);

		// given
		final Application application = new EchoApplication();
		final UriBuilder baseUri = UriBuilder.newInstance().scheme("http").host("localhost").port(FREE_PORT);
		final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri.build(), ResourceConfig.forApplication(application));
		final int actualPort = httpServer.getListener("grizzly").getPort();
		final UriBuilder effectiveUri = baseUri.port(actualPort).path("echo");
		Thread.sleep(1000);

		try (final Client client = ClientBuilder.newBuilder().register(new CustomJsonbProvider(CLIENT)).build()) {
			// when
			final String origin = String.format("Origin(%d)", mockInt());
			final POJO requestPojo = new POJO();
			requestPojo.setSeenBy(origin);
			final POJO responsePojo = client.target(effectiveUri).request(APPLICATION_JSON_TYPE).buildPost(Entity.entity(requestPojo, APPLICATION_JSON_TYPE))
					.invoke(POJO.class);

			// then
			final String expectedWaypoints = String.join(",", origin, "CustomSerializer(CLIENT)", "CustomDeserializer(SERVER)", "EchoResource",
					"CustomSerializer(SERVER)", "CustomDeserializer(CLIENT)");
			assertThat(responsePojo.getSeenBy(), is(expectedWaypoints));
		}

		Thread.sleep(5000);

		// httpServer.shutdown(1, SECONDS);
	}

	public static final class CustomJsonbProvider implements ContextResolver<Jsonb> {

		private final RuntimeType runtimeType;

		private CustomJsonbProvider(final RuntimeType runtimeType) {
			this.runtimeType = runtimeType;
		}

		public final Jsonb getContext(final Class<?> type) {
			if (!POJO.class.isAssignableFrom(type))
				return null;

			return JsonbBuilder.create(new JsonbConfig().withSerializers(new CustomSerializer()).withDeserializers(new CustomDeserializer()));
		}

		private final class CustomSerializer implements JsonbSerializer<POJO> {
			@Override
			public final void serialize(final POJO pojo, final JsonGenerator generator, final SerializationContext ctx) {
				generator.writeStartObject();
				generator.write("seenBy", String.format("%s,CustomSerializer(%s)", pojo.getSeenBy(), runtimeType));
				generator.writeEnd();
			}
		}

		private final class CustomDeserializer implements JsonbDeserializer<POJO> {
			@Override
			public final POJO deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
				final POJO pojo = new POJO();
				pojo.setSeenBy(String.format("%s,CustomDeserializer(%s)", parser.getObject().getString("seenBy"), runtimeType));
				return pojo;
			}
		}
	}

	public static final class POJO {

		private String seenBy;

		public final String getSeenBy() {
			return this.seenBy;
		}

		public final void setSeenBy(final String seenBy) {
			this.seenBy = seenBy;
		}
	}

	private static final class EchoApplication extends Application {

		@Override
		public final Set<Class<?>> getClasses() {
			return Collections.singleton(EchoResource.class);
		}

		@Override
		public final Set<Object> getSingletons() {
			return Collections.singleton(new CustomJsonbProvider(SERVER));
		}

		@Path("echo")
		public static class EchoResource {

			@POST
			@Consumes(APPLICATION_JSON)
			@Produces(APPLICATION_JSON)
			public POJO echo(final POJO pojo) {
				pojo.setSeenBy(String.join(",", pojo.getSeenBy(), "EchoResource"));
				return pojo;
			}
		}
	}

	private static final int mockInt() {
		return (int) Math.round(Integer.MAX_VALUE * Math.random());
	}

}
