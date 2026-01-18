package com.divine.econlite;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.LocalDate;
import java.util.UUID;

public final class EconService {
    private final DivineEconLitePlugin plugin;
    private final BalanceStore store;

    public EconService(DivineEconLitePlugin plugin, BalanceStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public String symbol() {
        return plugin.getConfig().getString("currency.symbol", "Coins");
    }

    public double wallet(UUID uuid) {
        return store.get(uuid).wallet();
    }

    public double bank(UUID uuid) {
        return store.get(uuid).bank();
    }

    public void setWallet(UUID uuid, double v) {
        var b = store.get(uuid);
        store.set(uuid, new BalanceStore.Balances(Math.max(0.0, v), b.bank()));
    }

    public void setBank(UUID uuid, double v) {
        var b = store.get(uuid);
        store.set(uuid, new BalanceStore.Balances(b.wallet(), Math.max(0.0, v)));
    }

    public boolean withdrawWallet(UUID uuid, double amount) {
        amount = Math.max(0.0, amount);
        double cur = wallet(uuid);
        if (cur + 1e-9 < amount) return false;
        setWallet(uuid, cur - amount);
        return true;
    }

    public void depositWallet(UUID uuid, double amount) {
        amount = Math.max(0.0, amount);
        setWallet(uuid, wallet(uuid) + amount);
    }

    public boolean moveWalletToBank(UUID uuid, double amount) {
        if (!withdrawWallet(uuid, amount)) return false;
        setBank(uuid, bank(uuid) + amount);
        return true;
    }

    public boolean moveBankToWallet(UUID uuid, double amount) {
        amount = Math.max(0.0, amount);
        double cur = bank(uuid);
        if (cur + 1e-9 < amount) return false;
        setBank(uuid, cur - amount);
        setWallet(uuid, wallet(uuid) + amount);
        return true;
    }

    public double applyKillRewardWithDailyCap(UUID uuid, double reward) {
        reward = Math.max(0.0, reward);
        double cap = plugin.getConfig().getDouble("kill-rewards.daily-cap", 0.0);
        if (cap <= 0.0) {
            depositWallet(uuid, reward);
            return reward;
        }

        String today = LocalDate.now().toString();
        var dc = store.getDaily(uuid);
        double used = dc.amount();
        if (!today.equals(dc.date())) {
            used = 0.0;
        }
        double remaining = Math.max(0.0, cap - used);
        double applied = Math.min(remaining, reward);
        if (applied > 0.0) {
            depositWallet(uuid, applied);
            store.setDaily(uuid, new BalanceStore.DailyCounter(today, used + applied));
        }
        return applied;
    }

    public OfflinePlayer offline(String name) {
        return Bukkit.getOfflinePlayer(name);
    }
}
