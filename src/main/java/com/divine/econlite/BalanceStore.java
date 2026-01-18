package com.divine.econlite;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BalanceStore {
    private final DivineEconLitePlugin plugin;
    private final File file;
    private FileConfiguration yaml;

    private final Map<UUID, Balances> balances = new HashMap<>();
    private final Map<UUID, DailyCounter> daily = new HashMap<>();

    public BalanceStore(DivineEconLitePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            yaml = new YamlConfiguration();
            save();
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        balances.clear();
        var section = yaml.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double wallet = section.getDouble(key + ".wallet", 0.0);
                    double bank = section.getDouble(key + ".bank", 0.0);
                    balances.put(uuid, new Balances(wallet, bank));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // load daily caps
        var d = yaml.getConfigurationSection("daily");
        if (d != null) {
            for (String key : d.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String date = d.getString(key + ".date", "");
                    double amount = d.getDouble(key + ".amount", 0.0);
                    daily.put(uuid, new DailyCounter(date, amount));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // ensure online players exist
        for (Player p : Bukkit.getOnlinePlayers()) {
            ensure(p.getUniqueId());
        }
    }

    public void save() {
        if (yaml == null) yaml = new YamlConfiguration();
        yaml.set("players", null);
        for (var e : balances.entrySet()) {
            String path = "players." + e.getKey();
            yaml.set(path + ".wallet", e.getValue().wallet());
            yaml.set(path + ".bank", e.getValue().bank());
        }
        yaml.set("daily", null);
        for (var e : daily.entrySet()) {
            String path = "daily." + e.getKey();
            yaml.set(path + ".date", e.getValue().date());
            yaml.set(path + ".amount", e.getValue().amount());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save data.yml: " + ex.getMessage());
        }
    }

    public Balances ensure(UUID uuid) {
        return balances.computeIfAbsent(uuid, u -> {
            double sw = plugin.getConfig().getDouble("starting-balance.wallet", 0.0);
            double sb = plugin.getConfig().getDouble("starting-balance.bank", 0.0);
            return new Balances(sw, sb);
        });
    }

    public Balances get(UUID uuid) {
        return ensure(uuid);
    }

    public void set(UUID uuid, Balances b) {
        balances.put(uuid, b);
    }

    public DailyCounter getDaily(UUID uuid) {
        return daily.computeIfAbsent(uuid, u -> new DailyCounter("", 0.0));
    }

    public void setDaily(UUID uuid, DailyCounter c) {
        daily.put(uuid, c);
    }

    public record Balances(double wallet, double bank) {}
    public record DailyCounter(String date, double amount) {}
}
