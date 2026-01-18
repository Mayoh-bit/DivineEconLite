package com.divine.econlite;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class DivineEconLitePlugin extends JavaPlugin {

    private BalanceStore store;
    private EconService econ;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.store = new BalanceStore(this);
        this.store.load();

        this.econ = new EconService(this, store);

        Bukkit.getPluginManager().registerEvents(new EconomyListeners(this, econ, store), this);

        PluginCommand money = getCommand("money");
        if (money != null) money.setExecutor(new MoneyCommand(this, econ));
        PluginCommand pay = getCommand("pay");
        if (pay != null) pay.setExecutor(new PayCommand(this, econ));
        PluginCommand bank = getCommand("bank");
        if (bank != null) bank.setExecutor(new BankCommand(this, econ));
        PluginCommand admin = getCommand("divineecon");
        if (admin != null) admin.setExecutor(new AdminCommand(this, econ, store));

        // Optional Vault hook
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultEconomyBridge(this, econ), this, ServicePriority.Highest);
                getLogger().info("Vault detected: Economy service registered.");
            } catch (Throwable t) {
                getLogger().warning("Vault detected but Economy bridge failed to register: " + t.getMessage());
            }
        }

        getLogger().info("DivineEconLite enabled.");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
        getLogger().info("DivineEconLite disabled.");
    }
}
