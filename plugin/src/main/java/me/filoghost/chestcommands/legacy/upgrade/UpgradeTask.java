/*
 * Copyright (C) filoghost and contributors SPDX-License-Identifier:
 * GPL-3.0-or-later
 */
package me.filoghost.chestcommands.legacy.upgrade;

import java.io.IOException;
import java.nio.file.Path;
import me.filoghost.chestcommands.legacy.Backup;

import me.filoghost.chestcommands.logging.Errors;
import me.filoghost.fcommons.Preconditions;
import me.filoghost.fcommons.config.exception.ConfigLoadException;
import me.filoghost.fcommons.config.exception.ConfigSaveException;

public abstract class UpgradeTask {

    private boolean saveRequired;
    private boolean hasRun;

    protected void setSaveRequired() { this.saveRequired = true; }

    public boolean runAndBackupIfNecessary(final Backup backup) throws UpgradeTaskException {
        Preconditions.checkState(!this.hasRun, "Upgrade task has already run");
        this.hasRun = true;

        try {
            this.computeChanges();
        } catch (final ConfigLoadException e) {
            throw new UpgradeTaskException(Errors.Upgrade.loadError(this.getOriginalFile()), e);
        }

        if (this.saveRequired) {
            try {
                backup.addFile(this.getOriginalFile());
            } catch (final IOException e) {
                throw new UpgradeTaskException(Errors.Upgrade.backupError(this.getOriginalFile()), e);
            }

            try {
                this.saveChanges();
            } catch (final ConfigSaveException e) {
                throw new UpgradeTaskException(Errors.Upgrade.saveError(this.getUpgradedFile()), e);
            }

            return true;

        } else {
            return false;
        }
    }

    public abstract Path getOriginalFile();

    public abstract Path getUpgradedFile();

    protected abstract void computeChanges() throws ConfigLoadException;

    protected abstract void saveChanges() throws ConfigSaveException;

}
