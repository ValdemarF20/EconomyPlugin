package net.foster.testing.orbital.data;

import net.foster.testing.orbital.OrbitalTesting;
import net.foster.testing.orbital.lambda.SafeConsumer;
import net.foster.testing.orbital.lambda.SafeFunction;
import net.foster.testing.orbital.managers.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.foster.testing.orbital.OrbitalTesting.TABLE_NAME;

/**
 * Database manager that handles data for a local SQLite database
 */
public class Database {
    private final OrbitalTesting orbitalTesting;
    private final Economy econ;
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    public Database(OrbitalTesting orbitalTesting, Economy econ) {
        this.orbitalTesting = orbitalTesting;
        this.econ = econ;
    }

    /**
     * Initializes the database and creates the file if missing
     * @return A {@link CompletableFuture<Void>} to handle exceptions
     */
    public CompletableFuture<Void> initialize() {
        TABLE_NAME = ConfigManager.getString("database.table-name");
        if(TABLE_NAME == null) {
            LOGGER.error("database.table-name not found in config.yml");
            return null;
        }
        if (!Files.exists(Paths.get(orbitalTesting.DATABASE_PATH))) {
            return openConnection().thenAcceptAsync(connection -> {
                try(PreparedStatement statement = connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME +
                                "` (`uuid` VARCHAR(36), `balance` DOUBLE)")) {
                    statement.execute();
                    LOGGER.info("SQLite file created");
                } catch (Exception e) {
                    LOGGER.error("Something went wrong while creating the SQLite file", e);
                }
            }, EXECUTOR);
        }

        autoSave();
        return null;
    }

    /**
     * Opens a connection to the database
     * For SQLite a connection can be opened for every query without a performance decrease
     * Because the database is local and not remote
     * @return A {@link CompletableFuture<Connection>} containing the {@link Connection}
     */
    public static CompletableFuture<Connection> openConnection() {
        CompletableFuture<Connection> connection = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                connection.complete(DriverManager.getConnection("jdbc:sqlite:" + OrbitalTesting.DATABASE_PATH));
            } catch(Exception e) {
                LOGGER.error("Connection to database could not open", e);
            }
        }, EXECUTOR);
        return connection;
    }

    /**
     * Executes a query to database
     * @param query A String query to send to the database
     * @param initializer A {@link Consumer} that can throw a checked exception.
     * @param process A {@link Function} that can throw a checked exception.
     * @return A {@link CompletableFuture<T>} for handling exceptions
     * @param <T> The type of the result for {@link SafeFunction}
     */
    public <T> CompletableFuture<T> query(String query, @NotNull SafeConsumer<PreparedStatement> initializer, @NotNull SafeFunction<ResultSet, T> process) {
        return CompletableFuture.supplyAsync(() -> {
            try(Connection connection = Database.openConnection().get();
                PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
                initializer.consume(statement);
                return process.apply(resultSet);
            } catch(Exception e) {
                LOGGER.error("Error when preparing statement for query: " + query, e);
                throw new RuntimeException();
            }
        }, EXECUTOR);
    }

    /**
     * Updates query to database
     * Must be a SQL Data Manipulation Language (DML) statement, such as INSERT, UPDATE or DELETE
     * @param query A String query to send to the database
     * @param initializer A {@link Consumer} that can throw a checked exception.
     * @return A {@link CompletableFuture<Void>} for handling exceptions
     */
    public CompletableFuture<Void> update(String query, @NotNull SafeConsumer<PreparedStatement> initializer) {
        return CompletableFuture.runAsync(() -> {
            try(Connection connection = Database.openConnection().get();
                PreparedStatement statement = connection.prepareStatement(query)) {
                initializer.consume(statement);
                statement.executeUpdate();
            } catch(Exception e) {
                LOGGER.error("Error when preparing statement for query: " + query, e);
                throw new RuntimeException();
            }
        }, EXECUTOR);
    }

    /**
     * Creates default data for a player, which is inserted into the database
     * @param uuid UUID for player to create default data for
     */
    public void createDefaultData(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if(player == null) {
            LOGGER.error("Player was null for uuid in createDefaultData: " + uuid);
            return;
        }


        this.update("INSERT OR IGNORE INTO " + TABLE_NAME + " (uuid, balance) VALUES (?, ?)",
                statement -> {
                    statement.setString(1, uuid.toString());
                    statement.setInt(2, ConfigManager.getInt("start-money"));
                });
    }

    /**
     * Loads the balance from database into balances map in Economy manager
     * @param uuid UUID for player to load
     */
    public void deserializePlayerData(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if(player == null) {
            LOGGER.error("Player was null for uuid in loadPlayerData: " + uuid);
            return;
        }

        /* Create default data if player is not in database */
        this.query("SELECT COUNT(*) FROM `" + TABLE_NAME + "` WHERE uuid = '" + uuid + "';",
                statement -> {},
                resultSet -> {
                    resultSet.next();
                    if(resultSet.getInt(1) == 0) {
                        // Data doesn't exist
                        createDefaultData(uuid);
                    }
                    return null;
                });


        /* Update player data */
        this.query("SELECT * FROM " + TABLE_NAME + " WHERE uuid = '" + uuid + "';",
                statement -> {},
                resultSet -> {
                    resultSet.next();
                    double balance = resultSet.getDouble("balance");

                    econ.getBalances().put(player, balance);
                    return null;
                }
        );
    }

    /**
     * Saves balance from Economy manager into database
     * @param uuid UUID for player to save
     */
    public void serializePlayerData(UUID uuid){
        Player player = Bukkit.getPlayer(uuid);
        if(player == null) {
            LOGGER.error("Player was null for uuid in serializePlayerData: " + uuid);
            return;
        }

        this.update("UPDATE " + TABLE_NAME +
                        " SET balance = ?" +
                        " WHERE uuid = ?",
                statement -> {
                    statement.setString(2, player.getUniqueId().toString());
                    statement.setDouble(1, econ.getBalance(player));
                });
    }

    /**
     * Saves the data for all players
     */
    public void serializePlayerDatas() {
        for (OfflinePlayer player : econ.getBalances().keySet()) {
            serializePlayerData(player.getUniqueId());
        }
    }

    /**
     * Makes sure that every task is stopped before closing the JVM
     */
    public void shutdown() {
        EXECUTOR.shutdown(); // Stops new tasks from being scheduled to the executor.

        try {
            if (!EXECUTOR.awaitTermination(30, TimeUnit.SECONDS)) { // Wait for existing tasks to terminate.
                EXECUTOR.shutdownNow(); // Cancel currently executing tasks that didn't finish in the time.

                if (!EXECUTOR.awaitTermination(30, TimeUnit.SECONDS)) { // Wait for tasks to respond to cancellation.
                    LOGGER.error("Pool failed to terminate");
                }
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow(); // Cancel currently executing tasks if interrupted.
            Thread.currentThread().interrupt(); // Preserve interrupt status.
        }
    }

    /**
     * Serializes the data synchronized to avoid running task after server has stopped
     * @param shutdown Whether the current tasks running should stop or not
     */
    public void save(boolean shutdown) {
        serializePlayerDatas();

        if(shutdown) {
            shutdown();
        }
    }

    /**
     * Runs at given interval to autosave the data into database
     */
    private void autoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                save(false);
            }
        }.runTaskTimerAsynchronously(orbitalTesting, 1, ConfigManager.getDataSaveInterval());
    }
}
