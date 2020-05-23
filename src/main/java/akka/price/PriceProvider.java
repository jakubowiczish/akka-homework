package akka.price;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.event.LoggingAdapter;
import lombok.SneakyThrows;

import java.util.Random;

import static akka.event.Logging.getLogger;
import static java.lang.Thread.sleep;

public class PriceProvider extends AbstractActor {

    private static final Random random = new Random();
    private final LoggingAdapter log = getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetPriceRequest.class, this::requestApply)
                .matchAny(e -> log.info("Unknown message received"))
                .build();
    }

    private void requestApply(GetPriceRequest getPriceRequest) {
        final long price = getRandomPrice();
        final GetPriceResponse getPriceResponse = GetPriceResponse.builder()
                .objectName(getPriceRequest.getObjectName())
                .price(price)
                .build();

        getSender().tell(getPriceResponse, getSelf());
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    @SneakyThrows
    private long getRandomPrice() {
        sleep(100 + random.nextInt(400));
        return (1 + random.nextInt(9));
    }

}
