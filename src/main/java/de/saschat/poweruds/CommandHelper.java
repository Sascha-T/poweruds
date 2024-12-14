package de.saschat.poweruds;

import de.saschat.poweruds.cli.PUDSCLI;

public final class CommandHelper {
    public static final String FAULT_ARGS = "\tIncorrect number of arguments.";
    public static final String FAULT_ADAPTER = "\tAdapter is not present.";

    private static boolean printNotTrue(boolean b, String message) {
        if(!b && message != null)
            System.out.println(message);
        return b;
    }

    public static boolean minArgs(Object[] args, String message, int min) {
        return printNotTrue(args.length >= min, message);
    }
    public static boolean exactArgs(Object[] args, String message, int need) {
        return printNotTrue(args.length == need, message);
    }

    public static boolean needAdapter(PUDSCLI cli, String message) {
        return printNotTrue(cli.getAdapter() != null && cli.getAdapter().isInitialized(), message);
    }

}
