package akka.database;


import akka.actor.ActorSystem;
import akka.actor.Props;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

public class Database {

    private static volatile Database instance;
    private static final Object mutex = new Object();

    private final Connection connection;

    private final Map<String, LongAdder> cacheForQueries;
    private final ExecutorService executorService;

    @SneakyThrows
    private Database() {
        cacheForQueries = new ConcurrentHashMap<>();
        executorService = Executors.newFixedThreadPool(1);

        Class.forName("org.sqlite.JDBC");

        connection = DriverManager.getConnection("jdbc:sqlite:akka.db");
        createDefaultStatement().executeUpdate(QueryProvider.getCreateQuery());
    }

    public static synchronized Database getInstance() {
        Database result = instance;

        if (result == null) {
            synchronized (mutex) {
                result = instance;
                if (result == null)
                    instance = result = new Database();
            }
        }

        return result;
    }

    @SneakyThrows
    public synchronized int getAndIncrementQueriesCounter(final ActorSystem system,
                                                          final String objectName) {
        if (cacheForQueries.containsKey(objectName)) {
            cacheForQueries.get(objectName).increment();
            handleQueriesCounterUsingActor(system, objectName);
            return cacheForQueries.get(objectName).intValue();
        }

        synchronized (connection) {
            final ResultSet rs = createDefaultStatement()
                    .executeQuery(QueryProvider.getSelectQuery(objectName));

            if (rs.next()) {
                int counter = rs.getInt(QueryProvider.COUNTER_COLUMN_NAME);
                cacheForQueries.put(objectName, getLongAdder(counter + 1));
                handleQueriesCounterUsingActor(system, objectName);
                return counter + 1;
            } else {
                handleQueriesCounterUsingActor(system, objectName);
                cacheForQueries.put(objectName, getLongAdder(1));
                return 1;
            }
        }
    }

    public synchronized void handleQueriesCounterUsingActor(final ActorSystem system,
                                                            final String objectName) {

        system
                .actorOf(Props.create(DatabaseActor.class))
                .tell(objectName, null);
    }

    @SneakyThrows
    public synchronized void executeQueryCounterUpdate(final String objectName) {
        synchronized (connection) {
            createDefaultStatement()
                    .execute(QueryProvider.getInsertOnConflictQuery(objectName));
        }
    }

    private LongAdder getLongAdder(int num) {
        final LongAdder longAdder = new LongAdder();
        longAdder.add(num);
        return longAdder;
    }

    @SneakyThrows
    private synchronized Statement createDefaultStatement() {
        return connection.createStatement();
    }

    static final class QueryProvider {

        public static final String COUNTER_COLUMN_NAME = "counter";

        public static String getCreateQuery() {
            return "CREATE TABLE IF NOT EXISTS query" +
                    " (id string PRIMARY KEY NOT NULL, counter int)";
        }

        public static String getSelectQuery(final String objectName) {
            return String.format(
                    "SELECT * FROM query q" +
                            " WHERE q.id = '%s'", objectName);
        }

        public static String getInsertOnConflictQuery(final String objectName) {
            return String.format(
                    "INSERT INTO query (id, counter)"
                            + " VALUES ('%s', '%d')"
                            + " ON CONFLICT(id) DO UPDATE SET counter = counter + 1;",
                    objectName, 1);
        }
    }

}