package akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.price.GetPriceRequest;

import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class AkkaRunner {

    private static final String ACTOR_SYSTEM_NAME = "ACTOR_SYSTEM";
    private static final String SERVER_ACTOR_NAME = "SERVER";
    private static final int NUMBER_OF_CLIENTS = 10;

    private static final String PROGRAM_USAGE_HELP = "PROGRAM USAGE\n"
            + " t [n] - tests exemplary clients with n [n] attempts\n"
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

        final HttpServer httpServer = new HttpServer(actorSystem);
        httpServer.startHttpServer(server);

        System.out.println(PROGRAM_USAGE_HELP);
        handleUserInput(clients);
    }

    private static void handleUserInput(final CopyOnWriteArrayList<ActorRef> clients) {
        final Scanner scanner = new Scanner(System.in);

        while (true) {
            final String line = scanner.nextLine();

            if (line.startsWith("t")) {
                final String[] splittedLine = line.split(" ");
                final int numberOfAttempts = Integer.parseInt(splittedLine[1]);
                testClients(clients, numberOfAttempts);
            } else if (line.startsWith("c")) {
                final String[] splittedLine = line.split(" ");
                final int clientNum = Integer.parseInt(splittedLine[1]);
                final String objectName = splittedLine[2];

                clients.get(clientNum).tell(new GetPriceRequest(objectName), null);
            } else if (line.startsWith("exit")) {
                System.out.println("Closing the application...");
                System.exit(0);
            } else {
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
