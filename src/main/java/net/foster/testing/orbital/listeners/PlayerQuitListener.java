package net.foster.testing.orbital.listeners;

import net.foster.testing.orbital.data.Database;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final Database database;
    public PlayerQuitListener(Database database) {
        this.database = database;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        database.serializePlayerData(e.getPlayer().getUniqueId());
    }
}
