/*
 * Copyright (C) filoghost and contributors SPDX-License-Identifier:
 * GPL-3.0-or-later
 */
package me.filoghost.chestcommands;

import me.filoghost.chestcommands.api.internal.BackendAPI;
import me.filoghost.chestcommands.command.CommandHandler;
import me.filoghost.chestcommands.config.ConfigManager;
import me.filoghost.chestcommands.config.CustomPlaceholders;
import me.filoghost.chestcommands.config.Settings;
import me.filoghost.chestcommands.hook.BarAPIHook;
import me.filoghost.chestcommands.hook.BungeeCordHook;
import me.filoghost.chestcommands.hook.PlaceholderAPIHook;
import me.filoghost.chestcommands.hook.VaultEconomyHook;
import me.filoghost.chestcommands.legacy.UpgradeExecutorException;
import me.filoghost.chestcommands.legacy.UpgradesExecutor;
import me.filoghost.chestcommands.listener.CommandListener;
import me.filoghost.chestcommands.listener.InventoryListener;
import me.filoghost.chestcommands.listener.JoinListener;
import me.filoghost.chestcommands.listener.SignListener;
import me.filoghost.chestcommands.logging.Errors;
import me.filoghost.chestcommands.logging.PrintableErrorCollector;
import me.filoghost.chestcommands.menu.MenuManager;
import me.filoghost.chestcommands.parsing.menu.LoadedMenu;
import me.filoghost.chestcommands.placeholder.PlaceholderManager;
import me.filoghost.chestcommands.task.TickingTask;
import me.filoghost.fcommons.FCommonsPlugin;
import me.filoghost.fcommons.config.ConfigLoader;
import me.filoghost.fcommons.logging.ErrorCollector;
import me.filoghost.fcommons.logging.Log;
import me.filoghost.fcommons.reflection.ReflectUtils;
import me.filoghost.updatechecker.UpdateChecker;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ChestCommands extends FCommonsPlugin {

    public static final String CHAT_PREFIX = ChatColor.DARK_GREEN + "[" + ChatColor.GREEN + "ChestCommands" + ChatColor.DARK_GREEN + "] "
            + ChatColor.GREEN;

    private static Plugin pluginInstance;
    private static Path dataFolderPath;

    private static ConfigManager configManager;
    private static CustomPlaceholders placeholders;

    private static ErrorCollector lastLoadErrors;
    private static String newVersion;

    @Override
    protected void onCheckedEnable() throws PluginEnableException {
        if (!ReflectUtils.isClassLoaded("org.bukkit.inventory.ItemFlag")) { // ItemFlag was added in 1.8
            if (Bukkit.getVersion().contains("(MC: 1.8)")) {
                throw new PluginEnableException("ChestCommands requires a more recent version of Bukkit 1.8 to run.");
            } else {
                throw new PluginEnableException("ChestCommands requires at least Bukkit 1.8 to run.");
            }
        }

        if (ChestCommands.pluginInstance != null || System.getProperty("ChestCommandsLoaded") != null) {
            throw new PluginEnableException("""
                    External plugin reloading is not supported:\
                     avoid using /reload or plugin reloaders, and use the command "/cc reload" instead.\
                     Fully restart the server to enable ChestCommands again.""");
        }

        System.setProperty("ChestCommandsLoaded", "true");

        ChestCommands.pluginInstance = this;
        ChestCommands.dataFolderPath = this.getDataFolder().toPath();
        ChestCommands.configManager = new ConfigManager(ChestCommands.getDataFolderPath());
        ChestCommands.placeholders = new CustomPlaceholders();

        BackendAPI.setImplementation(new DefaultBackendAPI());

        VaultEconomyHook.INSTANCE.setup();
        BarAPIHook.INSTANCE.setup();
        PlaceholderAPIHook.INSTANCE.setup();
        BungeeCordHook.INSTANCE.setup();

        if (VaultEconomyHook.INSTANCE.isEnabled()) {
            Log.info("Hooked Vault");
        } else {
            Log.warning("Couldn't find Vault and a compatible economy plugin! Money-related features will not work.");
        }

        if (BarAPIHook.INSTANCE.isEnabled()) {
            Log.info("Hooked BarAPI");
        }

        if (PlaceholderAPIHook.INSTANCE.isEnabled()) {
            Log.info("Hooked PlaceholderAPI");
        }

        Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new SignListener(), this);

        new CommandHandler("chestcommands").register(this);

        final ErrorCollector errorCollector = ChestCommands.load();

        if (errorCollector.hasErrors()) {
            errorCollector.logToConsole();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getConsoleSender()
                        .sendMessage(ChestCommands.CHAT_PREFIX + ChatColor.RED + "Encountered " + errorCollector.getErrorsCount()
                                + " error(s) on load. "
                                + "Check previous console logs or run \"/chestcommands errors\" to see them again.");
            }, 10L);
        }

        if (Settings.get().update_notifications) {
            UpdateChecker.run(this, 56919, (final String newVersion) -> {
                ChestCommands.newVersion = newVersion;

                Log.info("Found a new version: " + newVersion + " (yours: v" + this.getDescription().getVersion() + ")");
                Log.info("Download the update on Bukkit Dev:");
                Log.info("https://dev.bukkit.org/projects/chest-commands");
            });
        }

        // Start bStats metrics
        final int pluginID = 3658;
        new MetricsLite(this, pluginID);

        Bukkit.getScheduler().runTaskTimer(this, new TickingTask(), 1L, 1L);
    }

    @Override
    public void onDisable() { MenuManager.closeAllOpenMenuViews(); }

    public static ErrorCollector load() {
        final ErrorCollector errorCollector = new PrintableErrorCollector();
        MenuManager.reset();
        final boolean isFreshInstall = !Files.isDirectory(ChestCommands.configManager.getRootDataFolder());
        try {
            Files.createDirectories(ChestCommands.configManager.getRootDataFolder());
        } catch (final IOException e) {
            errorCollector.add(e, Errors.Config.createDataFolderIOException);
            return errorCollector;
        }

        try {
            final UpgradesExecutor upgradeExecutor = new UpgradesExecutor(ChestCommands.configManager);
            final boolean allUpgradesSuccessful = upgradeExecutor.run(isFreshInstall, errorCollector);
            if (!allUpgradesSuccessful) {
                errorCollector.add(Errors.Upgrade.failedSomeUpgrades);
            }
        } catch (final UpgradeExecutorException e) {
            errorCollector.add(e, Errors.Upgrade.genericExecutorError);
            errorCollector.add(Errors.Upgrade.failedSomeUpgrades);
        }

        ChestCommands.configManager.tryLoadSettings(errorCollector);
        ChestCommands.configManager.tryLoadLang(errorCollector);
        ChestCommands.placeholders = ChestCommands.configManager.tryLoadCustomPlaceholders(errorCollector);
        PlaceholderManager.setStaticPlaceholders(ChestCommands.placeholders.getPlaceholders());

        // Create the menu folder with the example menu
        if (!Files.isDirectory(ChestCommands.configManager.getMenusFolder())) {
            final ConfigLoader exampleMenuLoader = ChestCommands.configManager
                    .getConfigLoader(ChestCommands.configManager.getMenusFolder().resolve("example.yml"));
            ChestCommands.configManager.tryCreateDefault(errorCollector, exampleMenuLoader);
        }

        final List<LoadedMenu> loadedMenus = ChestCommands.configManager.tryLoadMenus(errorCollector);
        for (final LoadedMenu loadedMenu : loadedMenus) {
            MenuManager.registerMenu(loadedMenu, errorCollector);
        }

        ChestCommands.lastLoadErrors = errorCollector;
        return errorCollector;
    }

    public static Plugin getInstance() { return ChestCommands.pluginInstance; }

    public static Path getDataFolderPath() { return ChestCommands.dataFolderPath; }

    public static boolean hasNewVersion() { return ChestCommands.newVersion != null; }

    public static String getNewVersion() { return ChestCommands.newVersion; }

    public static ErrorCollector getLastLoadErrors() { return ChestCommands.lastLoadErrors; }

}
