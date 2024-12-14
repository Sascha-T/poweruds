package de.saschat.poweruds.cli;

public interface Command {
    /**
     * Executes this command
     * @param cli The parent CLI
     * @param args The arguments with {@code args[0]} being the value of {@code getName()}
     */
    public void execute(PUDSCLI cli, String[] args);
    public String getDescription();
    public String getName();
}
