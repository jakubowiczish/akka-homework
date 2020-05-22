package akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import akka.price.GetPriceRequest;
import akka.price.GetPriceResponse;
import lombok.AllArgsConstructor;

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

    private void requestApply(GetPriceRequest getPriceRequest) {
        log.info("Client with id: " + id
                + " is asking for a price of: " + getPriceRequest.getObjectName());
        server.tell(getPriceRequest, getSelf());
    }

    private void responseApply(GetPriceResponse getPriceResponse) {
        if (getPriceResponse.isPriceValid()) {
            log.info("Client with id: " + id
                    + " Query number: " + getPriceResponse.getQueriesCounter()
                    + " Price for " + getPriceResponse.getObjectName()
                    + ": " + getPriceResponse.getPrice());
        } else {
            log.info("Client with id: " + id
                    + " Query number: " + getPriceResponse.getQueriesCounter()
                    + " No price found for " + getPriceResponse.getObjectName());
        }
    }
}
