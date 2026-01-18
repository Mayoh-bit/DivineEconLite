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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EconomyListeners implements Listener {
    private final DivineEconLitePlugin plugin;
    private final EconService econ;
    private final BalanceStore store;
    private final Map<UUID, PendingCommand> pendingFees = new HashMap<>();

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

        double reward = rollKillReward(e.getEntityType());
        if (reward <= 0.0) return;

        econ.applyKillRewardWithDailyCap(killer.getUniqueId(), reward);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("death-penalty.enabled", true)) return;
        Player p = e.getEntity();
        if (p.hasPermission("divineecon.death.bypass")) return;
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
        if (p.hasPermission("divineecon.fees.bypass") || p.hasPermission("divineecon.fee.bypass")) return;

        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) return;
        String base = msg.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (!base.startsWith("/")) return;
        String commandName = base.substring(1);
        if (commandName.isEmpty()) return;

        for (String ex : plugin.getConfig().getStringList("command-fees.exempt")) {
            if (ex == null || ex.isEmpty()) continue;
            String trimmed = ex.trim().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
            if (commandName.equals(trimmed)) return;
        }

        double fee = commandFee(commandName, base);
        if (fee <= 0.0) return;

        if (!hasFunds(p.getUniqueId(), fee)) {
            String insufficient = plugin.getConfig().getString("messages.insufficient", "&cInsufficient");
            insufficient = insufficient.replace("{need}", format(fee)).replace("{symbol}", econ.symbol());
            send(p, insufficient);
            e.setCancelled(true);
            return;
        }

        if (plugin.getConfig().getBoolean("command-fees.confirm", false)) {
            long now = System.currentTimeMillis();
            int timeoutSeconds = plugin.getConfig().getInt("command-fees.confirm-timeout-seconds", 10);
            PendingCommand pending = pendingFees.get(p.getUniqueId());
            if (pending != null && pending.command().equals(commandName) && now - pending.createdAtMs() <= timeoutSeconds * 1000L) {
                pendingFees.remove(p.getUniqueId());
            } else {
                pendingFees.put(p.getUniqueId(), new PendingCommand(commandName, now));
                String confirmMsg = plugin.getConfig().getString("command-fees.confirm-message",
                        "&e再次输入指令可确认扣费 {cost} {symbol}。");
                confirmMsg = confirmMsg.replace("{cost}", format(fee))
                        .replace("{symbol}", econ.symbol())
                        .replace("{cmd}", commandName)
                        .replace("{seconds}", String.valueOf(timeoutSeconds));
                send(p, confirmMsg);
                e.setCancelled(true);
                return;
            }
        }

        if (!econ.withdrawWallet(p.getUniqueId(), fee)) {
            String insufficient = plugin.getConfig().getString("messages.insufficient", "&cInsufficient");
            insufficient = insufficient.replace("{need}", format(fee)).replace("{symbol}", econ.symbol());
            send(p, insufficient);
            e.setCancelled(true);
            return;
        }

        String paid = plugin.getConfig().getString("command-fees.message", "");
        paid = paid.replace("{cost}", format(fee)).replace("{symbol}", econ.symbol()).replace("{cmd}", commandName);
        send(p, paid);
    }

    private boolean hasFunds(UUID uuid, double amount) {
        return econ.wallet(uuid) + 1e-9 >= amount;
    }

    private double commandFee(String commandName, String baseCommand) {
        double fee = plugin.getConfig().getDouble("command-fees.list." + commandName, 0.0);
        if (fee <= 0.0) {
            fee = plugin.getConfig().getDouble("command-fees.fees." + baseCommand, 0.0);
        }
        if (fee <= 0.0) {
            fee = plugin.getConfig().getDouble("command-fees.fees./" + commandName, 0.0);
        }
        return fee;
    }

    private double rollKillReward(EntityType type) {
        String basePath = "kill-rewards.by-entity." + type.name();
        double defaultMin = plugin.getConfig().getDouble("kill-rewards.default.min",
                plugin.getConfig().getDouble("kill-rewards.default", 0.0));
        double defaultMax = plugin.getConfig().getDouble("kill-rewards.default.max",
                plugin.getConfig().getDouble("kill-rewards.default", defaultMin));
        double min = plugin.getConfig().getDouble(basePath + ".min",
                plugin.getConfig().getDouble("kill-rewards.per-mob." + type.name(), defaultMin));
        double max = plugin.getConfig().getDouble(basePath + ".max",
                plugin.getConfig().getDouble("kill-rewards.per-mob." + type.name(), defaultMax));
        if (min <= 0.0 && max <= 0.0) return 0.0;
        if (max < min) max = min;
        if (Math.abs(max - min) < 1e-9) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
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

    private record PendingCommand(String command, long createdAtMs) {}
}
