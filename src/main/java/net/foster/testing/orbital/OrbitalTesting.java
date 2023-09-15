package net.foster.testing.orbital;

import co.aikar.commands.PaperCommandManager;
import net.foster.testing.orbital.commands.PlayerCommands;
import net.foster.testing.orbital.data.ConfigManager;
import net.foster.testing.orbital.data.Database;
import net.foster.testing.orbital.listeners.PlayerJoinListener;
import net.foster.testing.orbital.listeners.PlayerQuitListener;
import net.foster.testing.orbital.managers.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class OrbitalTesting extends JavaPlugin {

    private final HashMap<UUID, Integer> activeCountdowns = new HashMap<>();
    private static OrbitalTesting instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(OrbitalTesting.class);

    private static Economy econ;


    /* Database */
    private Database database;
    public static String TABLE_NAME;
    public static String DATABASE_PATH;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        /* Managers */
        // High priority (independent of all other classes)
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.enableUnstableAPI("help");
        ConfigManager.setup();
        econ = new Economy();

        // Dependent on only itself and high priority classes
        setupServer(); // Sets up database (Depends on ConfigManager)

        // Commands
        manager.registerCommand(new PlayerCommands(this, econ));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(database),  this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(database),  this);

        tickCountdown();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        database.save(false);
    }

    /**
     * Sets up the database for the server
     */
    private void setupServer() {
        /* Setup of Database management */
        DATABASE_PATH = getDataFolder().getAbsolutePath() + File.separator + ConfigManager.getString("database.path") + ".db";

        database = new Database(this, econ);
        CompletableFuture<Void> cf = database.initialize();
        if(cf == null || cf.isCancelled()) {
            LOGGER.error("Database initialization was cancelled");
        }
    }

    public static OrbitalTesting getInstance() {
        return instance;
    }

    /**
     * Ticks the countdown for "/earn" command
     * Runnable ticks every second
     */
    private void tickCountdown() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeCountdowns.keySet()) {
                    int time = activeCountdowns.get(uuid);
                    if(time <= 0) {
                        activeCountdowns.remove(uuid);
                        continue;
                    }

                    activeCountdowns.put(uuid, time - 1);
                }
            }
        }.runTaskTimer(this, 1, 20); // Runs every second
    }

    public HashMap<UUID, Integer> getActiveCooldowns() {
        return activeCountdowns;
    }
}
