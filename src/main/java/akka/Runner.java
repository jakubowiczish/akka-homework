package akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.price.GetPriceRequest;

import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Runner {

    private static final String ACTOR_SYSTEM_NAME = "ACTOR_SYSTEM";
    private static final String SERVER_ACTOR_NAME = "SERVER";
    private static final int NUMBER_OF_CLIENTS = 10;
    private static final int NUMBER_OF_TEST_ATTEMPTS = 10;

    private static final String PROGRAM_USAGE_HELP = " t - tests exemplary clients\n"
            + " c [id] [object-name] - sends a request with clients' id [id] for specific objects' price with name [object-name]\n"
            + " exit - sends a request to close the application";

    public static void main(String[] args) {
        final ActorSystem actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME);

        final ActorRef server = actorSystem.actorOf(Props.create(Server.class), SERVER_ACTOR_NAME);

        final CopyOnWriteArrayList<ActorRef> clients = new CopyOnWriteArrayList<>();
        for (long i = 0; i < NUMBER_OF_CLIENTS; i++) {
            final ActorRef client = actorSystem.actorOf(Props.create(Client.class, i, server), "CLIENT_" + i);
            clients.add(client);
        }

        handleUserInput(clients);
    }

    private static void handleUserInput(final CopyOnWriteArrayList<ActorRef> clients) {
        final Scanner scanner = new Scanner(System.in);

        while (true) {
            final String line = scanner.nextLine();

            if (line.startsWith("t")) {
                testClients(clients);
            } else if (line.startsWith("c")) {
                String[] splittedLine = line.split(" ");
                int clientNum = Integer.parseInt(splittedLine[1]);
                String objectName = splittedLine[2];
                clients.get(clientNum).tell(new GetPriceRequest(objectName), null);
            } else if (line.startsWith("exit")) {
                System.out.println("Closing the application...");
                System.exit(0);
            } else {
                throw new IllegalArgumentException("Trying to invoke unknown command...");
            }
        }
    }

    private static void testClients(final CopyOnWriteArrayList<ActorRef> clients) {
        IntStream.range(0, NUMBER_OF_TEST_ATTEMPTS)
                .mapToObj(i -> clients.stream())
                .flatMap(Function.identity())
                .forEach(client -> client.tell(new GetPriceRequest("example-object"), null));
    }


}
