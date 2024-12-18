package de.saschat.poweruds.base.commands;

import de.saschat.poweruds.CommandHelper;
import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.cli.Command;
import de.saschat.poweruds.cli.PUDSCLI;

import java.util.regex.Pattern;

public class ECUCommand implements Command {
    private static Pattern pattern = Pattern.compile("/[\\dabcdefABCDEF]{3}:[\\dabcdefABCDEF]{3}/g");
    @Override
    public void execute(PUDSCLI cli, String[] args) {
        if(!CommandHelper.exactArgs(args, CommandHelper.FAULT_ARGS, 2))
            return;
        AbstractAdapter adapter = cli.getAdapter();
        if(!CommandHelper.needAdapter(cli, CommandHelper.FAULT_ADAPTER))
            return;
        /*if(!pattern.matcher(args[1]).matches()) {
            System.out.println("Invalid ECU.");
            return;
        }*/
        System.out.println("\tUsing: " + args[1]);
        adapter.selectECU(args[1]);
    }

    @Override
    public String getDescription() {
        return "[txh:rxh] => Select ECU";
    }

    @Override
    public String getName() {
        return "ecu";
    }
}
