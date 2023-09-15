package net.foster.testing.orbital.listeners;

import net.foster.testing.orbital.data.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final Database database;
    public PlayerJoinListener(Database database) {
        this.database = database;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        database.deserializePlayerData(e.getPlayer().getUniqueId());
    }
}
