/*
 * Copyright (C) filoghost and contributors SPDX-License-Identifier:
 * GPL-3.0-or-later
 */
package me.filoghost.chestcommands.icon;

import me.filoghost.chestcommands.api.ClickHandler;
import me.filoghost.chestcommands.api.StaticIcon;
import me.filoghost.fcommons.Preconditions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class APIStaticIcon implements StaticIcon {

    private ItemStack itemStack;
    private ClickHandler clickHandler;

    public APIStaticIcon(final ItemStack itemStack) {
        Preconditions.notNull(itemStack, "itemStack");
        this.itemStack = itemStack;
    }

    @Override
    public @NotNull ItemStack getItemStack() { return this.itemStack; }

    @Override
    public void setItemStack(@NotNull final ItemStack itemStack) {
        Preconditions.notNull(itemStack, "itemStack");
        this.itemStack = itemStack;
    }

    @Override
    public @Nullable ClickHandler getClickHandler() { return this.clickHandler; }

    @Override
    public void setClickHandler(@Nullable final ClickHandler clickHandler) { this.clickHandler = clickHandler; }

    @Override
    public ItemStack render(@NotNull final Player viewer) { return this.itemStack; }

}
