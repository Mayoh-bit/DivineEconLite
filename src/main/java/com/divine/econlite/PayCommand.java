package com.divine.econlite;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class PayCommand implements CommandExecutor {
    private final DivineEconLitePlugin plugin;
    private final EconService econ;

    public PayCommand(DivineEconLitePlugin plugin, EconService econ) {
        this.plugin = plugin;
        this.econ = econ;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("divineecon.pay")) {
            p.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.no-permission", "")));
            return true;
        }
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            p.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + "&c玩家不在线。"));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        amount = Math.max(0.0, amount);
        if (amount <= 0.0) return false;
        if (!econ.withdrawWallet(p.getUniqueId(), amount)) {
            String insufficient = plugin.getConfig().getString("messages.insufficient", "");
            insufficient = insufficient.replace("{need}", format(amount)).replace("{symbol}", econ.symbol());
            p.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + insufficient));
            return true;
        }
        econ.depositWallet(target.getUniqueId(), amount);

        String ok = plugin.getConfig().getString("messages.pay-ok", "");
        ok = ok.replace("{target}", target.getName()).replace("{amount}", format(amount)).replace("{symbol}", econ.symbol());
        p.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + ok));

        String recv = plugin.getConfig().getString("messages.pay-received", "");
        recv = recv.replace("{from}", p.getName()).replace("{amount}", format(amount)).replace("{symbol}", econ.symbol());
        target.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + recv));
        return true;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String format(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return String.format(Locale.US, "%.2f", v);
    }
}
