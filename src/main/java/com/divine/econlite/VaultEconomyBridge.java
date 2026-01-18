package com.divine.econlite;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public final class VaultEconomyBridge implements Economy {

    private final DivineEconLitePlugin plugin;
    private final EconService econ;

    public VaultEconomyBridge(DivineEconLitePlugin plugin, EconService econ) {
        this.plugin = plugin;
        this.econ = econ;
    }

    @Override public boolean isEnabled() { return plugin.isEnabled(); }
    @Override public String getName() { return "DivineEconLite"; }

    @Override public boolean hasBankSupport() { return true; }
    @Override public int fractionalDigits() { return 2; }
    @Override public String format(double amount) { return plugin.getConfig().getString("currency.format", "{amount} {symbol}")
            .replace("{amount}", String.format(java.util.Locale.US, "%.2f", amount))
            .replace("{symbol}", econ.symbol()); }
    @Override public String currencyNamePlural() { return econ.symbol(); }
    @Override public String currencyNameSingular() { return econ.symbol(); }

    @Override public boolean hasAccount(OfflinePlayer player) { return true; }
    @Override public boolean hasAccount(String playerName) { return true; }
    @Override public boolean hasAccount(String playerName, String worldName) { return true; }
    @Override public boolean hasAccount(OfflinePlayer player, String worldName) { return true; }

    @Override public double getBalance(OfflinePlayer player) { return econ.wallet(player.getUniqueId()); }
    @Override public double getBalance(String playerName) { return econ.wallet(Bukkit.getOfflinePlayer(playerName).getUniqueId()); }
    @Override public double getBalance(String playerName, String world) { return getBalance(playerName); }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) + 1e-9 >= amount; }
    @Override public boolean has(String playerName, double amount) { return getBalance(playerName) + 1e-9 >= amount; }
    @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        boolean ok = econ.withdrawWallet(player.getUniqueId(), amount);
        return new EconomyResponse(amount, getBalance(player), ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, ok ? "" : "Insufficient");
    }
    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }

    @Override public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        econ.depositWallet(player.getUniqueId(), amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, "");
    }
    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    // Bank uses plugin internal bank
    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use per-player bank"); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return createBank(name, player.getName()); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use per-player bank"); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use /bank"); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use /bank"); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use /bank"); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use /bank"); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use per-player bank"); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return isBankOwner(name, player.getName()); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Use per-player bank"); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return isBankMember(name, player.getName()); }
    @Override public List<String> getBanks() { return java.util.Collections.emptyList(); }

    @Override public boolean createPlayerAccount(OfflinePlayer player) { return true; }
    @Override public boolean createPlayerAccount(String playerName) { return true; }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return true; }
}
