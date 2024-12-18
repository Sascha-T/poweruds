package de.saschat.poweruds.base.commands;

import de.saschat.poweruds.PowerUDS;
import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.base.adapters.ELM327Adapter;
import de.saschat.poweruds.cli.Command;
import de.saschat.poweruds.cli.PUDSCLI;

import javax.bluetooth.RemoteDevice;
import java.util.Map;

public class BluetoothCommand implements Command {
    @Override
    public void execute(PUDSCLI cli, String[] args) {
        AbstractAdapter adapter;
        if((adapter = cli.getParent().getAdapter("elm327")) != null && adapter instanceof ELM327Adapter bt) {
            System.out.println("\tInquiring on Bluetooth, this may take a second...");
            bt.inquire(false);
            for (Map.Entry<String, RemoteDevice> r : bt.DEVICES.entrySet()) {
                System.out.printf("\t\t%s (%s): %s", r.getKey(), r.getValue().getBluetoothAddress(), bt.AVAILABLE.get(r.getValue()) != null ? " is a valid adapter" : " is not a valid adapter");
                System.out.println();
            }
        }
    }

    @Override
    public String getDescription() {
        return "Lists all available adapters and their names or addresses.";
    }

    @Override
    public String getName() {
        return "bluetooth";
    }
}
