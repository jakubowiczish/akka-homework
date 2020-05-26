package akka.database;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.event.LoggingAdapter;

import static akka.event.Logging.getLogger;

public class DatabaseActor extends AbstractActor {

    private final LoggingAdapter log = getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, objectName -> {
                    Database.getInstance()
                            .executeQueryCounterUpdate(objectName);

                    getSelf().tell(PoisonPill.getInstance(), getSelf());
                })
                .matchAny(o -> log.info("Unknown message received"))
                .build();
    }
}
