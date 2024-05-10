/*
 * Copyright (C) filoghost and contributors SPDX-License-Identifier:
 * GPL-3.0-or-later
 */
package me.filoghost.chestcommands.api.internal;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import me.filoghost.chestcommands.api.ConfigurableIcon;
import me.filoghost.chestcommands.api.Menu;
import me.filoghost.chestcommands.api.PlaceholderReplacer;
import me.filoghost.chestcommands.api.StaticIcon;
import me.filoghost.fcommons.Preconditions;

/**
 * Do not use this class: it is intended only for internal use and may change at
 * any time.
 */
@ApiStatus.Internal
public abstract class BackendAPI {

    private static BackendAPI implementation;

    public static void setImplementation(@NotNull final BackendAPI implementation) {
        Preconditions.notNull(implementation, "implementation");
        Preconditions.checkState(BackendAPI.implementation == null, "implementation already set");

        BackendAPI.implementation = implementation;
    }

    public static @NotNull BackendAPI getImplementation() {
        Preconditions.checkState(BackendAPI.implementation != null, "no implementation set");

        return BackendAPI.implementation;
    }

    public abstract boolean isInternalMenuLoaded(@NotNull String menuFileName);

    public abstract boolean openInternalMenu(@NotNull Player player, @NotNull String menuFileName);

    public abstract @NotNull Menu createMenu(@NotNull Plugin plugin, @NotNull String title, int rows);

    public abstract @NotNull ConfigurableIcon createConfigurableIcon(@NotNull Material material);

    public abstract @NotNull StaticIcon createStaticIcon(@NotNull ItemStack itemStack);

    public abstract void registerPlaceholder(@NotNull Plugin plugin, @NotNull String identifier,
            @NotNull PlaceholderReplacer placeholderReplacer);

    public abstract boolean unregisterPlaceholder(@NotNull Plugin plugin, @NotNull String identifier);

}
