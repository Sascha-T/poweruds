package de.saschat.poweruds.base.commands;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.saschat.poweruds.CommandHelper;
import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.cli.Command;
import de.saschat.poweruds.cli.PUDSCLI;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class UnlockCommand implements Command {

    // 100% stolen from prototux: https://github.com/prototux/PSA-RE/blob/master/sandbox/uds_auth_algorithm.c
    private static short[] SEC_1 = new short[] {0xB2, 0x3F, 0xAA};
    private static short[] SEC_2 = new short[] {0xB1, 0x02, 0xAB};
    private static short transform(byte data_msb, byte data_lsb, short[] sec) {
        int data = (Byte.toUnsignedInt(data_msb) << 8) | Byte.toUnsignedInt(data_lsb);
        if (data > 32767) {
            data = -(32768 - (data % 32768));
        }

        int a = (((data % sec[0]) * sec[2]) & 0xFFFFFFF);
        int b = (((data / sec[0]) & 0xFFFFFFF) * sec[1]) & 0xFFFFFFF;

        int result = (int) (a - b);

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

    private static Map<String, String> keyAssociations = new HashMap<>();
    private static void load() {
        try {
            Type map = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> temp = (new Gson().fromJson(new InputStreamReader(new FileInputStream("ecus.json")), map));
            keyAssociations = new HashMap<>(temp);
        } catch (Throwable e) {
            PUDSCLI.exception(e);
        }
    }
    private static void save() {
        try {
            var x = new OutputStreamWriter(new FileOutputStream("ecus.json"));
            new Gson().toJson(keyAssociations, x);
            x.flush();
            x.close();
        } catch (Throwable e) {
            PUDSCLI.exception(e);
        }
    }
    static {
        load();
    }

    private static String hash(String input) {
        try {
            byte[] bytesOfMessage = input.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] theMD5digest = md.digest(bytesOfMessage);
            StringBuilder sb = new StringBuilder();
            for (byte b : theMD5digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            PUDSCLI.exception(ex);
            throw new RuntimeException("No MD5?");
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void execute(PUDSCLI cli, String[] args_) {
        if(!CommandHelper.needAdapter(cli, CommandHelper.FAULT_ADAPTER))
            return;
        try {
            var diag = cli.processUDS("1003");
            if(!diag.processed() || diag.ret().startsWith("7F"))
                throw new RuntimeException("Opening diagnostic session failed.");
            sleep();

            var swZone = cli.processUDS("22F195"); // read zone F195
            if(!swZone.processed()) {
                throw new RuntimeException("Failed to read software version zone.");
            }
            sleep();
            String swHash = hash(swZone.ret());

            String[] args = args_;
            if(args.length == 1) {
                String seed = keyAssociations.get(swHash);
                if(seed == null) {
                    throw new RuntimeException("Couldn't find associated unlocking key.");
                }
                args = new String[] {args_[0], seed};
            }

            var unlock = cli.processUDS("2703");
            sleep();
            System.out.println(unlock);
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

            keyAssociations.put(swHash, args[1]);
            save();
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
