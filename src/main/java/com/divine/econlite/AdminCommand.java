package com.divine.econlite;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        if (!sender.hasPermission("divineecon.admin")) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.no-permission", "")));
            return true;
        }

        if (args.length < 4) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player offline.");
            return true;
        }
        String which = args[2].toLowerCase();
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException ex) {
            return false;
        }

        if (sub.equals("set")) {
            if (which.equals("wallet")) econ.setWallet(target.getUniqueId(), amount);
            else if (which.equals("bank")) econ.setBank(target.getUniqueId(), amount);
            else return false;
            store.save();
            sender.sendMessage("OK");
            return true;
        }

        if (sub.equals("give")) {
            if (which.equals("wallet")) econ.depositWallet(target.getUniqueId(), amount);
            else if (which.equals("bank")) econ.setBank(target.getUniqueId(), econ.bank(target.getUniqueId()) + Math.max(0.0, amount));
            else return false;
            store.save();
            sender.sendMessage("OK");
            return true;
        }

        if (sub.equals("take")) {
            if (which.equals("wallet")) econ.withdrawWallet(target.getUniqueId(), Math.max(0.0, amount));
            else if (which.equals("bank")) econ.setBank(target.getUniqueId(), Math.max(0.0, econ.bank(target.getUniqueId()) - Math.max(0.0, amount)));
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
}
