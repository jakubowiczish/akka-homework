package akka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.price.GetPriceRequest;

import static akka.event.Logging.getLogger;

public final class Server extends AbstractActor {

    private final LoggingAdapter log = getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetPriceRequest.class, this::requestApply)
                .matchAny(e -> log.info("Unknown message received"))
                .build();
    }

    private void requestApply(GetPriceRequest getPriceRequest) {
        context()
                .actorOf(Props.create(ServerNode.class))
                .tell(getPriceRequest, getSender());
    }
}
