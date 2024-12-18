package de.saschat.poweruds;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.base.adapters.DiagboxAdapter;
import de.saschat.poweruds.base.commands.UnlockCommand;
import de.saschat.poweruds.cli.PUDSCLI;
import de.saschat.poweruds.spi.ExtensionProvider;

import java.util.*;

public class PowerUDS {
    private static final ServiceLoader<ExtensionProvider> LOADER = ServiceLoader.load(ExtensionProvider.class);

    public static void main(String[] args) {
        byte[] seed = new byte[] {0x0D, 0x16, 0x61, 0x46};
        byte[] key = new byte[] {(byte) 0xB4, (byte) 0xE0};
        System.out.println(UnlockCommand.compute(key, seed));
        // new PowerUDS().startCLI(args);
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

    private Map<String, AbstractAdapter> ADAPTERS = new HashMap<>();
    public void register(AbstractAdapter diagboxAdapter) {
        ADAPTERS.put(diagboxAdapter.getName(), diagboxAdapter);
    }
    public AbstractAdapter getAdapter(String id) {
        return ADAPTERS.get(id);
    }

    public Collection<AbstractAdapter> getAdapters() {
        return Collections.unmodifiableCollection(ADAPTERS.values());
    }
}
