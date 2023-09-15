package net.foster.testing.orbital.data;

import net.foster.testing.orbital.OrbitalTesting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ConfigManager {
    private static JavaPlugin main;
    public FileConfiguration config;
    private static ConfigManager instance;

    private ConfigManager() {
        main = OrbitalTesting.getInstance();
        config = main.getConfig();

        instantiate();
    }

    /**
     * Method used on server startup
     * starts an autosaving task of the config.yml file
     */
    private void instantiate() {
        setupConfig();

        new BukkitRunnable() {
            @Override
            public void run() {
                saveConfig();
            }
        }.runTaskTimerAsynchronously(main, getInt("config-save-interval") * 20L * 60L, getInt("config-save-interval") * 20L * 60L);
    }

    /**
     * Sets up all required config files for plugin
     */
    public void setupConfig() {
        config.options().copyDefaults(true);
        main.saveDefaultConfig();
    }

    /**
     * Saves all config files for plugin
     */
    public void saveConfig() {
        config.options().copyDefaults();
        main.saveConfig();
    }

    /**
     * Use method to get an instance of singleton class
     * @return Instance of the class
     */
    public static ConfigManager setup() {
        if(instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public static String getString(String path) {
        return main.getConfig().getString(path);
    }

    public static String getString(String path, String defaultOption) {
        return main.getConfig().getString(path, defaultOption);
    }

    public static int getInt(String path) {
        return main.getConfig().getInt(path);
    }

    public static boolean getBoolean(String path) { return main.getConfig().getBoolean(path); }

    public static double getDouble(String path) {
        return main.getConfig().getDouble(path);
    }

    public static List<String> getList(String path) {
        return main.getConfig().getStringList(path);
    }

    public static ConfigurationSection getConfigurationSection(String path) {
        return main.getConfig().getConfigurationSection(path);
    }

    /* Plugin specific methods */

    /**
     * Gets the database save interval in minutes
     * @return Minute amount for interval
     */
    public static long getDataSaveInterval() {
        return main.getConfig().getLong("database.save-interval") * 20 /* Seconds */ * 60 /* Minutes */;
    }
}
