package net.foster.testing.orbital.managers;

import org.bukkit.OfflinePlayer;

import java.util.HashMap;

/**
 * An economy manager used to handle all players balances
 */
public class Economy {
    private final HashMap<OfflinePlayer, Double> balances = new HashMap<>();

    /**
     * Get the balance for a player
     * @param player Player to get balance from
     * @return A double containing the balance
     */
    public double getBalance(OfflinePlayer player) {
        return balances.computeIfAbsent(player, k -> 0.0);
    }

    /**
     * Withdraws given amount from players balance
     * @param player Player to withdraw from
     * @param amount Amount to withdraw as a double
     */
    public void withdrawPlayer(OfflinePlayer player, double amount) {
        balances.put(player, getBalance(player) - amount);
    }

    /**
     * Deposits given amount to players balance
     * @param player Player to deposit money to
     * @param amount Amount to deposit as a double
     */
    public void depositPlayer(OfflinePlayer player, double amount) {
        balances.put(player, getBalance(player) + amount);
    }

    /**
     * @return A Map containing all players balances
     */
    public HashMap<OfflinePlayer, Double> getBalances() {
        return balances;
    }
}
