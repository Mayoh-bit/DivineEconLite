package com.divine.econlite;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class MoneyCommand implements CommandExecutor {
    private final DivineEconLitePlugin plugin;
    private final EconService econ;

    public MoneyCommand(DivineEconLitePlugin plugin, EconService econ) {
        this.plugin = plugin;
        this.econ = econ;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("divineecon.use")) {
            p.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.no-permission", "")));
            return true;
        }
        String msg = plugin.getConfig().getString("messages.balance", "");
        msg = msg.replace("{wallet}", format(econ.wallet(p.getUniqueId())))
                .replace("{bank}", format(econ.bank(p.getUniqueId())))
                .replace("{symbol}", econ.symbol());
        p.sendMessage(color(plugin.getConfig().getString("messages.prefix", "") + msg));
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
