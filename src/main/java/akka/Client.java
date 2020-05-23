package akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import akka.price.GetPriceRequest;
import akka.price.GetPriceResponse;
import lombok.AllArgsConstructor;

import static akka.Client.LoggingMessageProvider.*;
import static akka.event.Logging.getLogger;

@AllArgsConstructor
public final class Client extends AbstractActor {

    private final LoggingAdapter log = getLogger(getContext().getSystem(), this);

    private final long id;
    private final ActorRef server;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetPriceRequest.class, this::requestApply)
                .match(GetPriceResponse.class, this::responseApply)
                .matchAny(e -> log.info("Unknown message received"))
                .build();
    }

    private synchronized void requestApply(final GetPriceRequest getPriceRequest) {
        log.info(getLogMessageOnRequest(this.id, getPriceRequest.getObjectName()));
        server.tell(getPriceRequest, getSelf());
    }

    private synchronized void responseApply(final GetPriceResponse getPriceResponse) {
        if (getPriceResponse.isPriceValid()) {
            log.info(getLogMessageOnValidResponse(
                    getPriceResponse.getQueriesCounter(),
                    this.id,
                    getPriceResponse.getPrice(),
                    getPriceResponse.getObjectName()
            ));
        } else {
            log.info(getLogMessageOnInvalidResponse(
                    getPriceResponse.getQueriesCounter(),
                    this.id,
                    getPriceResponse.getObjectName()));
        }
    }

    static final class LoggingMessageProvider {

        public static String getLogMessageOnRequest(final long clientId,
                                                    final String objectName) {

            return String.format("CLIENT [%d] REQUESTING FOR PRICE OF: %s",
                    clientId, objectName);
        }

        public static String getLogMessageOnValidResponse(final long queryCounter,
                                                          final long clientId,
                                                          final long price,
                                                          final String objectName) {

            return String.format("QUERY {%d} - CLIENT [%d] RECEIVED VALID PRICE: %d FOR: %s",
                    queryCounter, clientId, price, objectName);
        }

        public static String getLogMessageOnInvalidResponse(final long queryCounter,
                                                            final long clientId,
                                                            final String objectName) {

            return String.format("QUERY {%d} - CLIENT [%d] RECEIVED NO PRICE FOR: %s",
                    queryCounter, clientId, objectName);
        }
    }
}
