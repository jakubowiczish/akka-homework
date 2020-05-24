package akka.database;


import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public synchronized int getAndIncrementQueriesCounter(final String objectName) {
        if (cacheForQueries.containsKey(objectName)) {
            cacheForQueries.get(objectName).increment();
            updateQueriesCounter(objectName);
            return cacheForQueries.get(objectName).intValue();
        }

        synchronized (connection) {
            final ResultSet rs = createDefaultStatement()
                    .executeQuery(QueryProvider.getSelectQuery(objectName));

            if (rs.next()) {
                int count = rs.getInt("counter");
                cacheForQueries.put(objectName, getLongAdder(count + 1));
                updateQueriesCounter(objectName);
                return count + 1;
            } else {
                insertQueriesCounter(objectName);
                cacheForQueries.put(objectName, getLongAdder(1));
                return 1;
            }
        }
    }

    private synchronized void insertQueriesCounter(final String objectName) {
        executorService.submit(() -> {
            synchronized (connection) {
                try {
                    createDefaultStatement()
                            .executeUpdate(QueryProvider.getInsertQuery(objectName));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        });
    }

    private synchronized void updateQueriesCounter(final String objectName) {
        executorService.submit(() -> {
            synchronized (connection) {
                final int counter = cacheForQueries.get(objectName).intValue();

                try {
                    createDefaultStatement()
                            .executeUpdate(QueryProvider.getUpdateQuery(counter, objectName));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

            }
        });
    }

    private synchronized LongAdder getLongAdder(int num) {
        final LongAdder longAdder = new LongAdder();
        longAdder.add(num);
        return longAdder;
    }

    @SneakyThrows
    private synchronized Statement createDefaultStatement() {
        return connection.createStatement();
    }

    static final class QueryProvider {

        public static String getCreateQuery() {
            return "CREATE TABLE IF NOT EXISTS query (id string, counter int)";
        }

        public static String getSelectQuery(final String objectName) {
            return String.format("SELECT * FROM query q WHERE q.id = '%s'", objectName);
        }

        public static String getInsertQuery(final String objectName) {
            return String.format("INSERT INTO query VALUES('%s', %d)", objectName, 1);
        }

        public static String getUpdateQuery(final int counter, final String objectName) {
            return String.format("UPDATE query SET counter = %d WHERE id = '%s'", counter, objectName);
        }
    }

}