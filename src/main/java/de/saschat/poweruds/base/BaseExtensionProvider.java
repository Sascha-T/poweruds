package de.saschat.poweruds.base;

import de.saschat.poweruds.PowerUDS;
import de.saschat.poweruds.base.adapters.DiagboxAdapter;
import de.saschat.poweruds.base.commands.AdapterCommand;
import de.saschat.poweruds.base.commands.ECUCommand;
import de.saschat.poweruds.base.commands.HelpCommand;
import de.saschat.poweruds.cli.PUDSCLI;
import de.saschat.poweruds.spi.ExtensionProvider;

public class BaseExtensionProvider implements ExtensionProvider {
    @Override
    public void provideAdapters(PowerUDS uds) {
        uds.register(new DiagboxAdapter());
    }

    @Override
    public void provideCommands(PUDSCLI cli) {
        cli.registerCommand(new HelpCommand());
        cli.registerCommand(new AdapterCommand());
        cli.registerCommand(new ECUCommand());
    }
}
