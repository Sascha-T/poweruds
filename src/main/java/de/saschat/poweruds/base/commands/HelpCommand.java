package de.saschat.poweruds.base.commands;

import de.saschat.poweruds.cli.Command;
import de.saschat.poweruds.cli.PUDSCLI;

public class HelpCommand implements Command {
    @Override
    public void execute(PUDSCLI cli, String[] args) {
        for (Command command : cli.getCommands()) {
            System.out.println(command.getName() + " - " + command.getDescription());
        }
    }

    @Override
    public String getDescription() {
        return "Displays all commands implemented in this console.";
    }

    @Override
    public String getName() {
        return "help";
    }
}
