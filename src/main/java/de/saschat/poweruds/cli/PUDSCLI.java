package de.saschat.poweruds.cli;

import de.saschat.poweruds.PowerUDS;
import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AdapterInitializationException;
import de.saschat.poweruds.base.adapters.DiagboxAdapter;
import de.saschat.poweruds.spi.ExtensionProvider;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PUDSCLI {
    PowerUDS parent;

    public PUDSCLI(PowerUDS uds) {
        this.parent = uds;
    }

    private Map<String, Command> cmdLookup = new HashMap<>();
    private AbstractAdapter adapter = null;

    public static void exception(Throwable e) {
        //if(System.getProperty("pudscli.debug") != null)
        e.printStackTrace();
    }

    public void registerCommand(Command command) {
        cmdLookup.put(command.getName(), command);
    }

    public Collection<Command> getCommands() {
        return Collections.unmodifiableCollection(cmdLookup.values());
    }

    public void run(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveState));
        loadState();

        System.out.println("PowerUDS CLI");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(">> ");
            String line = scanner.nextLine();

            if (line.startsWith(".")) {
                processCommand(line.substring(1));
            } else {
                if (line.length() % 2 == 1) {
                    System.out.println("## LINE REJECTED");
                    continue;
                }

                UDSExecution x = processUDS(line);
                if (x.processed())
                    System.out.println(x.ret());
            }
        }
    }

    private void loadState() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("state.txt")));
            String line = reader.readLine();
            if(!line.equals("null")) {
                String[] data = line.split(":");
                String adapter = data[0];
                String ecu = data[1];
                String args = reader.readLine();

                AbstractAdapter adp = getParent().getAdapter(adapter);
                adp.initialize(args);
                adp.selectECU(ecu);

                setAdapter(adp);
            }
        } catch (Throwable e) {
            exception(e);
        }
    }

    private void saveState() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("state.txt")));
            if (adapter == null) {
                writer.write("null");
                writer.newLine();
            } else {
                writer.write(adapter.getName());
                writer.write(":");
                writer.write(adapter.getECU());
                writer.newLine();
                writer.write(adapter.getArgs());
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (Throwable e) {
            exception(e);
        }
    }

    public record UDSExecution(String ret, boolean processed) {
    }


    public UDSExecution processUDS(String line) {
        if (adapter == null) {
            System.out.println("<# ADAPTER NOT PRESENT");
            return new UDSExecution(null, false);
        }
        if (!adapter.isInitialized()) {
            System.out.println("<# ADAPTER NOT READY");
            return new UDSExecution(null, false);
        }
        try {
            String ret = adapter.sendUDS(line).get();
            return new UDSExecution(ret, true);
        } catch (Throwable e) {
            exception(e);
            System.out.println("<# ADAPTER ERROR");
        }
        return new UDSExecution(null, false);
    }

    public void processCommand(String line) {
        String[] inputSegmented = StringSplitter.split(line);

        Command cmd = cmdLookup.get(inputSegmented[0]);
        if (cmd == null) {
            System.out.println("Command not found.");
        } else {
            cmd.execute(this, inputSegmented);
        }
    }

    public PowerUDS getParent() {
        return parent;
    }

    public void setAdapter(AbstractAdapter adapter) {
        this.adapter = adapter;
    }

    public AbstractAdapter getAdapter() {
        return this.adapter;
    }
}
