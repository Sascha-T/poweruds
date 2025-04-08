package de.saschat.poweruds.base.commands;

import de.saschat.poweruds.PowerUDS;
import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AdapterInitializationException;
import de.saschat.poweruds.cli.Command;
import de.saschat.poweruds.cli.PUDSCLI;

public class AdapterCommand implements Command {
    @Override
    public void execute(PUDSCLI cli, String[] args) {
        if(args.length == 1) {
            for (String adapter : cli.getParent().getAdapters()) {
                System.out.println("\t" + adapter);
            }
            return;
        }
        AbstractAdapter adapter = cli.getParent().getAdapter(args[1]);
        if(adapter == null) {
            System.out.println("\tAdapter not found.");
            return;
        }

        String option = "";
        if(args.length > 2)
            option = args[2];

        try {
            adapter.initialize(option);
            System.out.println("\tSuccessfully initialized adapter.");
        } catch (Throwable e) {
            PUDSCLI.exception(e);
            System.out.println("\tFailed to initialize adapter.");
        }

        cli.setAdapter(adapter);
    }

    @Override
    public String getDescription() {
        return "[name] [option] => Initializes adapter with name with options for use";
    }

    @Override
    public String getName() {
        return "adapter";
    }
}
