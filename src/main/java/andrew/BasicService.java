package andrew;

import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.*;
import com.linecorp.armeria.server.logging.LoggingService;

import java.util.concurrent.CompletableFuture;

public class BasicService {

    public static void main(String[] args) {
        ServerBuilder sb = new ServerBuilder();
        sb.http(8080);

        // Add a simple 'Hello, world!' service.
        sb.service("/", (ctx, res) -> HttpResponse.of("Hello, world!"));

        // Using path variables:
        sb.service("/greet/{name}", new AbstractHttpService() {
            @Override
            protected HttpResponse doGet(ServiceRequestContext ctx, HttpRequest req) throws Exception {
                String name = ctx.pathParam("name");
                return HttpResponse.of("Hello, %s!", name);
            }
        }.decorate(LoggingService.newDecorator())); // Enable logging

        // Using an annotated service object:
        sb.annotatedService(new Object() {
            @Get("/greet2/:name") // `:name` style is also available
            public HttpResponse greet(@Param("name") String name) {
                return HttpResponse.of("Hello, %s!", name);
            }
        });

        // Just in case your name contains a slash:
        sb.serviceUnder("/greet3", (ctx, req) -> {
            String path = ctx.mappedPath();  // Get the path without the prefix ('/greet3')
            String name = path.substring(1); // Strip the leading slash ('/')
            return HttpResponse.of("Hello, %s!", name);
        });

        // Using an annotated service object:
        sb.annotatedService(new Object() {
            @Get("regex:^/greet4/(?<name>.*)$")
            public HttpResponse greet(@Param("name") String name) {
                return HttpResponse.of("Hello, %s!", name);
            }
        });

        // Using a query parameter (e.g. /greet5?name=alice) on an annotated service object:
        sb.annotatedService(new Object() {
            @Get("/greet5")
            public HttpResponse greet(@Param("name") String name,
                                      @Param("title") @Default("Mr.") String title) {
                // "Mr." is used by default if there is no title parameter in the request.
                return HttpResponse.of("Hello, %s %s!", title, name);
            }
        });

        // Getting a map of query parameters on an annotated service object:
        sb.annotatedService(new Object() {
            @Get("/greet6")
            public HttpResponse greet(HttpParameters parameters) {
                return HttpResponse.of("Hello, %s!", parameters.get("name"));
            }
        });

        // Using media type negotiation:
        sb.annotatedService(new Object() {
            @Get("/greet7")
            @Produces("application/json;charset=UTF-8")
            public HttpResponse greetGet(@Param("name") String name) {
                return HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, "{\"name\":\"%s\"}", name);
            }

            @Post("/greet7")
            @Consumes("application/x-www-form-urlencoded")
            public HttpResponse greetPost(@Param("name") String name) {
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        Server server = sb.build();
        CompletableFuture<Void> future = server.start();
        future.join();
    }

}
