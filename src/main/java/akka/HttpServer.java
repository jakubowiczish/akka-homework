package akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.price.GetPriceRequest;
import akka.price.GetPriceResponse;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import static akka.http.javadsl.model.HttpRequest.create;
import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.PathMatchers.segment;
import static org.jsoup.Jsoup.parse;

public class HttpServer {

    private static final long TIMEOUT_LONG = 10000;
    private static final Timeout TIMEOUT = Timeout.create(Duration.ofMillis(TIMEOUT_LONG));

    private final ActorSystem system;
    private final Http http;
    private final Materializer materializer;

    public HttpServer(final ActorSystem system) {
        this.system = system;
        http = Http.get(system);
        materializer = Materializer.matFromSystem(system);
    }

    public void startHttpServer(final ActorRef server, final String host, final int port) {
        final Route route = createRouteForPriceAndReview(server);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = route.flow(system, materializer);
        final CompletionStage<ServerBinding> futureBinding =
                http.bindAndHandle(routeFlow, ConnectHttp.toHost(host, port), materializer);

        futureBinding.whenComplete((binding, exception) -> {
            if (binding != null) {
                InetSocketAddress address = binding.localAddress();
                system.log().info("Server online at http://{}:{}/",
                        address.getHostString(),
                        address.getPort());
            } else {
                system.log().error("Failed to bind HTTP endpoint, terminating system", exception);
                system.terminate();
            }
        });
    }

    private Route createRouteForPriceAndReview(final ActorRef server) {
        return concat(
                pathPrefix("price", () -> handlePriceRoute(server)),
                pathPrefix("review", this::handleReviewRoute)
        );
    }

    private Route handlePriceRoute(final ActorRef server) {
        return path(segment(), (objectName) ->
                get(() -> {
                    final GetPriceRequest getPriceRequest = GetPriceRequest.builder()
                            .objectName(objectName)
                            .build();

                    final Future<Object> futureResponse = Patterns.ask(server, getPriceRequest, TIMEOUT);

                    final GetPriceResponse getPriceResponse;
                    try {
                        getPriceResponse = (GetPriceResponse) Await.result(futureResponse, TIMEOUT.duration());
                    } catch (InterruptedException | TimeoutException e) {
                        e.printStackTrace();
                        return complete("Problem with receiving price of the object: " + objectName);
                    }

                    return getPriceResponse.isPriceValid()
                            ? complete(
                            getMessageOnValidResponse(
                                    getPriceResponse.getQueriesCounter(),
                                    getPriceResponse.getPrice(),
                                    getPriceResponse.getObjectName()))
                            : complete(
                            getMessageOnInvalidResponse(
                                    getPriceResponse.getQueriesCounter(),
                                    getPriceResponse.getObjectName()));
                }));
    }

    private String getMessageOnValidResponse(final long queryCounter, final long price, final String objectName) {
        return String.format("QUERY {%d} - RECEIVED VALID PRICE: %d FOR: %s",
                queryCounter, price, objectName);
    }

    private String getMessageOnInvalidResponse(final long queryCounter, final String objectName) {
        return String.format("QUERY {%d} - RECEIVED NO PRICE FOR: %s",
                queryCounter, objectName);
    }

    private Route handleReviewRoute() {
        return path(segment(), (objectName) ->
                get(() -> onComplete(
                        http.singleRequest(create(getUrlForListOfProductsWithSpecificName(objectName)))
                                .thenCompose(this::toStrict)
                                .thenApply(this::toUtf8String)
                                .thenApply(this::getFirstProductHrefAttributeAsString)
                                .thenApply(this::getAdvantagesUrl)
                                .thenCompose(link -> http.singleRequest(create(link)))
                                .thenCompose(this::toStrict)
                                .thenApply(this::toUtf8String)
                                .thenApply(this::getPros),
                        (result) -> complete(result.get()))));
    }

    private String getUrlForListOfProductsWithSpecificName(final String objectName) {
        return String.format("https://www.opineo.pl/?szukaj=%s&s=2", objectName.replaceAll(" ", "+"));
    }

    private CompletionStage<HttpEntity.Strict> toStrict(final HttpResponse response) {
        return response.entity().toStrict(TIMEOUT_LONG, materializer);
    }

    private String toUtf8String(final HttpEntity.Strict entity) {
        return entity.getData().utf8String();
    }

    private String getFirstProductHrefAttributeAsString(final String html) {
        return parse(html)
                .select("#screen > div.pls > div:nth-child(1) > h2 > a")
                .attr("href");
    }

    private String getAdvantagesUrl(final String link) {
        return String.format("https://www.opineo.pl%s", link);
    }

    private String getPros(final String html) {
        return parse(html)
                .select("#screen > div.pane > div > div.shl > div.ph_asset.ph_pros")
                .text();
    }

}
