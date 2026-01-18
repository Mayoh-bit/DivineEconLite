package com.divine.econlite;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class AdminCommand implements CommandExecutor {
    private final DivineEconLitePlugin plugin;
    private final EconService econ;
    private final BalanceStore store;

    public AdminCommand(DivineEconLitePlugin plugin, EconService econ, BalanceStore store) {
        this.plugin = plugin;
        this.econ = econ;
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;
        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            if (!sender.hasPermission("divineecon.reload")) {
                sender.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.reload-ok", "")));
            return true;
        }

        if (!hasAdminPermission(sender, sub)) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.no-permission", "")));
            return true;
        }

        if (args.length < 4) return false;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage("Player not found.");
            return true;
        }
        String which = args[2].toLowerCase();
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (amount < 0.0) return false;
        UUID uuid = target.getUniqueId();

        if (sub.equals("set")) {
            if (which.equals("wallet")) econ.setWallet(uuid, amount);
            else if (which.equals("bank")) econ.setBank(uuid, amount);
            else return false;
            store.save();
            sender.sendMessage("OK");
            return true;
        }

        if (sub.equals("give") || sub.equals("add")) {
            if (which.equals("wallet")) econ.depositWallet(uuid, amount);
            else if (which.equals("bank")) econ.setBank(uuid, econ.bank(uuid) + amount);
            else return false;
            store.save();
            sender.sendMessage("OK");
            return true;
        }

        if (sub.equals("take") || sub.equals("remove")) {
            if (which.equals("wallet")) econ.withdrawWallet(uuid, amount);
            else if (which.equals("bank")) econ.setBank(uuid, Math.max(0.0, econ.bank(uuid) - amount));
            else return false;
            store.save();
            sender.sendMessage("OK");
            return true;
        }

        return false;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private boolean hasAdminPermission(CommandSender sender, String sub) {
        if (sender.hasPermission("divineecon.admin")) return true;
        return switch (sub) {
            case "set" -> sender.hasPermission("divineecon.admin.set");
            case "give" -> sender.hasPermission("divineecon.admin.give");
            case "take" -> sender.hasPermission("divineecon.admin.take");
            case "add" -> sender.hasPermission("divineecon.admin.add");
            case "remove" -> sender.hasPermission("divineecon.admin.remove");
            default -> false;
        };
    }
}
