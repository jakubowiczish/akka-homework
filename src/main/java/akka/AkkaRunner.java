package akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.client.Client;
import akka.price.GetPriceRequest;
import akka.server.HttpServer;
import akka.server.Server;

import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toCollection;

public class AkkaRunner {

    private static final String ACTOR_SYSTEM_NAME = "ACTOR_SYSTEM";
    private static final String SERVER_ACTOR_NAME = "SERVER";
    private static final int NUMBER_OF_CLIENTS = 10;

    private static final String PROGRAM_USAGE_HELP = "PROGRAM USAGE\n"
            + " t [n] - tests exemplary clients with n [n] attempts\n"
            + " c [id] [object-name] - sends a request with clients' id [id] for specific objects' price with name [object-name]\n"
            + " exit - sends a request to close the application";

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        final ActorSystem actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME);

        final ActorRef server = actorSystem.actorOf(Props.create(Server.class), SERVER_ACTOR_NAME);

        final CopyOnWriteArrayList<ActorRef> clients = LongStream.range(0, NUMBER_OF_CLIENTS)
                .mapToObj(i -> actorSystem.actorOf(Props.create(Client.class, i, server), "CLIENT_" + i))
                .collect(toCollection(CopyOnWriteArrayList::new));

        final HttpServer httpServer = new HttpServer(actorSystem);
        httpServer.startHttpServer(server, HOST, PORT);

        System.out.println(PROGRAM_USAGE_HELP);
        handleUserInput(clients);
    }

    private static void handleUserInput(final CopyOnWriteArrayList<ActorRef> clients) {
        final Scanner scanner = new Scanner(System.in);

        while (true) {
            final String line = scanner.nextLine();

            switch (line.charAt(0)) {
                case 't': {
                    final String[] splittedLine = line.split(" ");
                    final int numberOfAttempts = Integer.parseInt(splittedLine[1]);

                    testClients(clients, numberOfAttempts);
                    break;
                }
                case 'c': {
                    final String[] splittedLine = line.split(" ");
                    final int clientNum = Integer.parseInt(splittedLine[1]);
                    final String objectName = splittedLine[2];

                    clients.get(clientNum).tell(new GetPriceRequest(objectName), null);
                    break;
                }
                case 'e':
                    System.out.println("Closing the application...");
                    System.exit(0);
                default:
                    throw new IllegalArgumentException("Trying to invoke unknown command...");
            }
        }
    }

    private static void testClients(final CopyOnWriteArrayList<ActorRef> clients,
                                    final int numberOfAttempts) {

        for (int i = 0; i < numberOfAttempts; i++) {
            for (ActorRef client : clients) {
                client.tell(new GetPriceRequest("example-object"), null);
            }
        }
    }

}
