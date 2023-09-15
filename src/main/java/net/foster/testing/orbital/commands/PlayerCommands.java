package net.foster.testing.orbital.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.foster.testing.orbital.OrbitalTesting;
import net.foster.testing.orbital.data.ConfigManager;
import net.foster.testing.orbital.managers.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Syntax;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class PlayerCommands extends BaseCommand {
    /* Instances */
    private final OrbitalTesting orbitalTesting;
    private final Economy econ;

    /* Variables */
    private final ConfigurationSection commandsSection;

    /* Utilities */
    private final NumberFormat numberFormatUS = NumberFormat.getCurrencyInstance(Locale.US);
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerCommands.class);
    public PlayerCommands(OrbitalTesting orbitalTesting, Economy econ) {
        this.orbitalTesting = orbitalTesting;
        this.econ = econ;

        commandsSection = ConfigManager.getConfigurationSection("commands");
        if(commandsSection == null) {
            LOGGER.error("\"commands\" section not found in config.yml");
            throw new NullPointerException();
        }
    }

    @CommandAlias("balance|bal")
    @Syntax("[player] &e - Check balance of player (default is sender)")
    public void onBal(CommandSender sender, @Optional OnlinePlayer targetOnlinePlayer) {
        if(targetOnlinePlayer == null) {
            if(sender instanceof Player) {
                String balance = numberFormatUS.format(econ.getBalance((Player) sender));

                sender.sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                                Objects.requireNonNull(commandsSection.getString("balance.no-target"))
                                        .replace("%balance%", balance)));
            }
            return;
        }

        Player target = targetOnlinePlayer.getPlayer();
        String balance = numberFormatUS.format(econ.getBalance(target));
        sender.sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Objects.requireNonNull(commandsSection.getString("balance.target"))
                                .replace("%target_player%", target.getName())
                                .replace("%balance%", balance)));
    }

    @CommandAlias("give")
    @Subcommand("target amount")
    @Syntax("<target> <amount> &e - Give target player money")
    public void onGive(Player sender, OnlinePlayer targetOnlinePlayer, String[] args) {
        if(targetOnlinePlayer == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("give.target-not-specified"))));
            return;
        }
        Player target = targetOnlinePlayer.getPlayer();

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch(NumberFormatException ignored) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("give.incorrect-amount-input"))));
            return;
        }

        if(sender.equals(target)) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&',
                            Objects.requireNonNull(commandsSection.getString("give.self-usage"))));
            return;
        }

        if(econ.getBalance(sender) < amount) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("give.cannot-afford"))));
        } else {
            String amountFormatted = numberFormatUS.format(amount);

            econ.withdrawPlayer(sender, amount);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("give.given"))
                            .replace("%target_player%", target.getName())
                            .replace("%amount%", amountFormatted)));

            econ.depositPlayer(target, amount);
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("give.received"))
                            .replace("%amount%", amountFormatted)
                            .replace("%sender_player%", sender.getName())));
        }
    }

    @CommandAlias("setbalance|setbal")
    @Subcommand("target amount")
    @Syntax("<target> <amount> &e - Give target player money")
    @CommandPermission("orbital.operator")
    public void onSetBal(CommandSender sender, OnlinePlayer targetOnlinePlayer, String[] args) {
        if(targetOnlinePlayer == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("set-balance.target-not-specified"))));
            return;
        }
        Player target = targetOnlinePlayer.getPlayer();

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
            // Amount must be positive
            if(amount < 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        Objects.requireNonNull(commandsSection.getString("set-balance.positive-amount"))));
                return;
            }
        } catch(NumberFormatException ignored) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(commandsSection.getString("set-balance.incorrect-amount-input"))));
            return;
        }

        double balanceChange = amount - econ.getBalance(target);
        econ.depositPlayer(target, balanceChange);
        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(commandsSection.getString("set-balance.completed"))
                        .replace("%target_player%", target.getName())
                        .replace("%new_amount%", String.valueOf(numberFormatUS.format(amount)))));
    }

    @CommandAlias("earn")
    public void onEarn(Player sender) {
        HashMap<UUID, Integer> activeCountdowns = orbitalTesting.getActiveCooldowns();
        if (activeCountdowns.containsKey(sender.getUniqueId())) {
            sender.sendMessage(
                    ChatColor.translateAlternateColorCodes('&',
                            Objects.requireNonNull(commandsSection.getString("earn.cooldown"))
                                    .replace("%time%", activeCountdowns.get(sender.getUniqueId()).toString())));
            return;
        }

        final double random = (Math.random() * (5 - 1)) + 1; // Between 1 and 5
        econ.depositPlayer(sender, random);

        sender.sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Objects.requireNonNull(commandsSection.getString("earn.money-given"))
                                .replace("%money%", String.valueOf(numberFormatUS.format(random)))));

        activeCountdowns.put(sender.getUniqueId(), 60);
    }
}
