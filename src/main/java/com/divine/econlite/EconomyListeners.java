package com.divine.econlite;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class EconomyListeners implements Listener {
    private final DivineEconLitePlugin plugin;
    private final EconService econ;
    private final BalanceStore store;

    public EconomyListeners(DivineEconLitePlugin plugin, EconService econ, BalanceStore store) {
        this.plugin = plugin;
        this.econ = econ;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent e) {
        if (!plugin.getConfig().getBoolean("kill-rewards.enabled", true)) return;
        Player killer = e.getEntity().getKiller();
        if (plugin.getConfig().getBoolean("kill-rewards.require-player-kill", true) && killer == null) return;
        if (killer == null) return;

        EntityType t = e.getEntityType();
        double def = plugin.getConfig().getDouble("kill-rewards.default", 0.0);
        double reward = plugin.getConfig().getDouble("kill-rewards.per-mob." + t.name(), def);
        if (reward <= 0.0) return;

        econ.applyKillRewardWithDailyCap(killer.getUniqueId(), reward);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("death-penalty.enabled", true)) return;
        Player p = e.getEntity();
        double percent = plugin.getConfig().getDouble("death-penalty.wallet-loss-percent", 0.0);
        percent = Math.max(0.0, Math.min(100.0, percent));
        if (percent <= 0.0) return;
        double wallet = econ.wallet(p.getUniqueId());
        double loss = wallet * (percent / 100.0);
        if (loss <= 0.0) return;
        econ.withdrawWallet(p.getUniqueId(), loss);

        String msg = plugin.getConfig().getString("messages.death-loss", "");
        msg = msg.replace("{loss}", format(loss)).replace("{symbol}", econ.symbol());
        send(p, msg);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("command-fees.enabled", true)) return;
        Player p = e.getPlayer();
        if (p.hasPermission("divineecon.fee.bypass")) return;

        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) return;
        String base = msg.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (!base.startsWith("/")) return;

        for (String ex : plugin.getConfig().getStringList("command-fees.exempt")) {
            if (ex != null && !ex.isEmpty() && base.equalsIgnoreCase(ex.trim())) return;
        }

        double fee = plugin.getConfig().getDouble("command-fees.fees." + base, 0.0);
        if (fee <= 0.0) return;

        if (!econ.withdrawWallet(p.getUniqueId(), fee)) {
            String insufficient = plugin.getConfig().getString("messages.insufficient", "&cInsufficient");
            insufficient = insufficient.replace("{need}", format(fee)).replace("{symbol}", econ.symbol());
            send(p, insufficient);
            e.setCancelled(true);
            return;
        }

        String paid = plugin.getConfig().getString("command-fees.message", "");
        paid = paid.replace("{cost}", format(fee)).replace("{symbol}", econ.symbol()).replace("{cmd}", base);
        send(p, paid);
    }

    private void send(Player p, String raw) {
        if (raw == null || raw.isEmpty()) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        p.sendMessage(color(prefix + raw));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String format(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return String.format(Locale.US, "%.2f", v);
    }
}
