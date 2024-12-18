package de.saschat.poweruds.base.commands;

import de.saschat.poweruds.CommandHelper;
import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.cli.Command;
import de.saschat.poweruds.cli.PUDSCLI;

import java.util.concurrent.ExecutionException;

public class UnlockCommand implements Command {

    // 100% stolen from prototux: https://github.com/prototux/PSA-RE/blob/master/sandbox/uds_auth_algorithm.c
    private static byte[] SEC_1 = new byte[] {(byte) 0xB2, 0x3F, (byte) 0xAA};
    private static byte[] SEC_2 = new byte[] {(byte) 0xB1, 0x02, (byte) 0xAB};
    private static short transform(byte data_msb, byte data_lsb, byte[] sec) {
        short data = (short) ((short) (data_msb << 8) | (short) data_lsb);
        System.out.printf("%02X%02X\n", data_msb, data_lsb);
        int result = ((data % sec[0]) * sec[2]) - ((data / sec[0]) * sec[1]);
        if(result < 0)
            result += (sec[0] * sec[2]) + sec[1];
        return (short) result;
    }
    private static int compute(byte[] pin, byte[] chg) {
        short res_msb = (short) (transform(pin[0], pin[1], SEC_1) | transform(chg[0], chg[3], SEC_2));
        short res_lsb = (short) (transform(chg[1], chg[2], SEC_1) | transform((byte) (res_msb >> 8), (byte) (res_msb & 0xFF), SEC_2));

        return (res_msb << 16) | res_lsb;
    }

    private static byte[] stringToByte(String s) {
        byte[] seedBytes = new byte[4];
        for (int i = 0; i < s.length() / 2; i++)
            seedBytes[i] = (byte) Integer.parseInt(s.substring(i*2, (i+1)*2), 16);
        return seedBytes;
    }
    @Override
    public void execute(PUDSCLI cli, String[] args) {
        if(!CommandHelper.exactArgs(args, CommandHelper.FAULT_ARGS, 2))
            return;
        AbstractAdapter adapter = cli.getAdapter();
        if(!CommandHelper.needAdapter(cli, CommandHelper.FAULT_ADAPTER))
            return;
        try {
            var diag = cli.processUDS("1003");
            if(!diag.processed() || diag.ret().startsWith("7F"))
                throw new RuntimeException("Opening diagnostic session failed.");
            var unlock = cli.processUDS("2703");
            if(!unlock.processed() || unlock.ret().startsWith("7F"))
                throw new RuntimeException("Preparing unlock failed.");
            var seed = unlock.ret().substring(4); // seed
            System.out.println("\tReceived seed: " + seed);
            byte[] chg = stringToByte(seed);
            byte[] key = stringToByte(args[1]);
            String unlockKey = String.format("%04X", compute(key, chg));
            System.out.println("\tUnlocking with: " + unlockKey);
            var response = cli.processUDS("2704" + unlockKey);
            if(!response.processed() || response.ret().startsWith("7F")) {
                System.out.println("\t\tInvalid response: " + response.ret());
                throw new RuntimeException("Key was invalid or unlocking failed otherwise.");
            }
            System.out.println("\t\tSuccess!");
        } catch (Throwable e) {
            PUDSCLI.exception(e);
            System.out.println("\tUnlocking generically failed.");
        }
    }

    @Override
    public String getDescription() {
        return "[key] => Unlocks ECU";
    }

    @Override
    public String getName() {
        return "unlock";
    }
}
