package de.saschat.poweruds.spi;

import de.saschat.poweruds.PowerUDS;
import de.saschat.poweruds.cli.PUDSCLI;

public interface ExtensionProvider {
    void provideAdapters(PowerUDS cli);
    void provideCommands(PUDSCLI cli);

}
