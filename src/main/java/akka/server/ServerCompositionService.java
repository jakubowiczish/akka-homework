package akka.server;

import akka.actor.ActorSystem;
import akka.database.Database;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.price.GetPriceRequest;
import akka.price.GetPriceResponse;
import akka.price.PriceProvider;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import static akka.event.Logging.getLogger;

public final class ServerCompositionService extends AbstractActor {

    private static final int TIMEOUT_IN_MILLIS = 300;

    private final LoggingAdapter log = getLogger(getContext().getSystem(), this);
    private final ExecutionContextExecutor contextExecutor = getContext().getDispatcher();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetPriceRequest.class, this::requestApply)
                .matchAny(e -> log.info("Unknown message received"))
                .build();
    }

    private synchronized void requestApply(GetPriceRequest getPriceRequest) {
        final ActorRef sender = getSender();

        final ActorRef firstPriceProvider = context().actorOf(Props.create(PriceProvider.class));
        final ActorRef secondPriceProvider = context().actorOf(Props.create(PriceProvider.class));

        final Future<Object> firstPriceResponse
                = Patterns.ask(firstPriceProvider, getPriceRequest, TIMEOUT_IN_MILLIS);
        final Future<Object> secondPriceResponse
                = Patterns.ask(secondPriceProvider, getPriceRequest, TIMEOUT_IN_MILLIS);

        final ActorSystem system = context().system();

        firstPriceResponse
                .zipWith(secondPriceResponse,
                        GetPriceResponse::chooseLowerPriceResponse,
                        contextExecutor)
                .fallbackTo(firstPriceResponse
                        .map(GetPriceResponse::toGetPriceResponse, contextExecutor))
                .fallbackTo(secondPriceResponse
                        .map(GetPriceResponse::toGetPriceResponse, contextExecutor))
                .onComplete(optionalPriceResponse -> {
                    final long queriesCounter = Database
                            .getInstance()
                            .getAndIncrementQueriesCounter(system, getPriceRequest.getObjectName());

                    final GetPriceResponse getPriceResponse = optionalPriceResponse
                            .getOrElse(() -> GetPriceResponse.builder()
                                    .objectName(getPriceRequest.getObjectName())
                                    .price(GetPriceResponse.INVALID_PRICE)
                                    .build());

                    getPriceResponse.setQueriesCounter(queriesCounter);

                    sender.tell(getPriceResponse, getSelf());
                    getSelf().tell(PoisonPill.getInstance(), getSelf());
                    return getPriceResponse;
                }, contextExecutor);
    }

}
