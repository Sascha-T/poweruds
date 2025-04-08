package de.saschat.poweruds;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.base.adapters.DiagboxAdapter;
import de.saschat.poweruds.base.commands.UnlockCommand;
import de.saschat.poweruds.cli.PUDSCLI;
import de.saschat.poweruds.spi.ExtensionProvider;

import java.util.*;
import java.util.function.Supplier;

public class PowerUDS {
    private static final ServiceLoader<ExtensionProvider> LOADER = ServiceLoader.load(ExtensionProvider.class);

    public static void main(String[] args) {
        new PowerUDS().startCLI(args);
    }
    private PUDSCLI cli;
    public PowerUDS() {
        LOADER.reload();
        for (ExtensionProvider prov : LOADER) {
            prov.provideAdapters(this);
        }
    }


    public void startCLI(String[] args) {
        if(cli == null) {
            cli = new PUDSCLI(this);

            LOADER.reload();
            for (ExtensionProvider prov : LOADER) {
                prov.provideCommands(cli);
            }
        }
        cli.run(args);
    }

    private Map<String, Supplier<AbstractAdapter>> ADAPTERS = new HashMap<>();
    public void register(String name, Supplier<AbstractAdapter> adp) {
        ADAPTERS.put(name, adp);
    }
    public AbstractAdapter getAdapter(String id) {
        return ADAPTERS.get(id).get();
    }

    public Collection<String> getAdapters() {
        return Collections.unmodifiableCollection(ADAPTERS.keySet());
    }
}
