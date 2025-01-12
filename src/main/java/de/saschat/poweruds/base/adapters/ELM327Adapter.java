package de.saschat.poweruds.base.adapters;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AdapterInitializationException;
import de.saschat.poweruds.cli.PUDSCLI;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class ELM327Adapter extends AbstractAdapter implements DiscoveryListener {
    private final Object lock = new Object();

    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private static javax.bluetooth.UUID[] SERVICES = new javax.bluetooth.UUID[]{new javax.bluetooth.UUID("0000110100001000800000805F9B34FB", false)};
    private static LocalDevice DEVICE;
    private static DiscoveryAgent AGENT;

    private static boolean initialized = false;
    private boolean connected = false;
    private RemoteDevice selected;
    private StreamConnection connection;
    private DataInputStream in;
    private DataOutputStream out;

    public String selectedECUrx = "";
    public String selectedECUtx = "";


    static {
        try {
            DEVICE = LocalDevice.getLocalDevice();
            AGENT  = DEVICE.getDiscoveryAgent();
            initialized = true;
        } catch (BluetoothStateException e) {
            PUDSCLI.exception(e);
        }
    }

    private record InitCommand(String command, boolean echo) {};
    private static final InitCommand[] INITIALIZATION = new InitCommand[] {
            new InitCommand("AT Z", true), // reset
            new InitCommand("AT D", true), // all to default
            new InitCommand("AT E 0", false), // disable echo
            new InitCommand("AT E 0", false), // disable echo
            new InitCommand("AT L 0", false), // disable echo
            new InitCommand("AT H 0", false), // disable echo
            new InitCommand("AT S 0", false), // disable echo
            new InitCommand("AT AL", false), // disable echo
            new InitCommand("AT V 0", false), // disable echo
            new InitCommand("AT SP 6", false), // disable echo
    };

    public Map<String, RemoteDevice> DEVICES = new HashMap<>();
    public Map<RemoteDevice, String> AVAILABLE = new HashMap<>();


    @Override
    public String getName() {
        return "elm327";
    }

    @Override
    public void initialize(String options) throws AdapterInitializationException {
        if (!initialized)
            return;
        if (options.isEmpty()) {
            throw new RuntimeException("You need to specify device name or address.");
        }

        readPaired();
        RemoteDevice dev;
        boolean dnq = (dev = getDevice(options)) != null;
        if (!dnq && !inquire(true))
            return;
        try {
            if (dev != null || (dev = getDevice(options)) != null)
                if (connect(dev)) {
                    for (InitCommand initCommand : INITIALIZATION) { // initialize state
                        if (initCommand.echo()) { // yes echo command
                            String ret;
                            readPrompt();
                            if ((ret = safeWriteAndRead(initCommand.command()).get()).equals(initCommand.command())) {
                                System.out.println("D1: " + ret); // debug
                            } else {
                                throw new RuntimeException("command was not echoed: " + ret); // todo better
                            }
                        } else safeWriteAndIgnore(initCommand.command());

                        String ret = safeRead().get();
                        if(ret.equals("?")) {
                            throw new RuntimeException("command not understood: " + initCommand);
                        }
                    }
                    initialized = true;
                }
        } catch (Exception ex) {
            PUDSCLI.exception(ex);
            initialized = false;
        }
    }

    private void readPaired() {
        var dev = DEVICE.getDiscoveryAgent().retrieveDevices(DiscoveryAgent.PREKNOWN);
        for (RemoteDevice remoteDevice : dev) {
            DEVICES.put(remoteDevice.getBluetoothAddress(), remoteDevice);
            inquireSpecific(remoteDevice);
        }
    }

    public boolean connect(RemoteDevice device) {
        if(AVAILABLE.get(device) == null)
            inquireSpecific(device);
        // connect
        try {
            connection = (StreamConnection) Connector.open(AVAILABLE.get(device));
            in = connection.openDataInputStream();
            out = connection.openDataOutputStream();

            connected = true;
            return true;
        } catch (IOException e) {
            PUDSCLI.exception(e);
        }
        return false;
    }

    public void btWrite(String text) {
        try {
            for (char c : text.toCharArray()) {
                out.write(c);
            }
            out.write('\n');
        } catch (IOException e) {
            connected = false;
            PUDSCLI.exception(e);
            throw new RuntimeException(e);
        }
    }
    public String btRead() {
        StringBuilder text = new StringBuilder();
        try {
            // todo: find a not stupid way of having a timeout...
            char c = ' ';
            while((c = in.readChar()) != '\n')
                text.append(c);
            return text.toString();
        } catch (Throwable e) {
            connected = false;
            PUDSCLI.exception(e);
            throw new RuntimeException(e);
        }
    }
    private boolean readPrompt() throws ExecutionException, InterruptedException {
        return exec.submit(() -> {
            return in.readChar() == '>';
        }).get();
    }

    public void safeWriteAndIgnore(String text) {
        try {
            exec.submit(() -> rawWriteAndIgnore(text)).get();
        } catch (Throwable e) {
            PUDSCLI.exception(e);
            connected = false;
        }
    }

    public Future<String> safeRead() {
        return exec.submit(this::btRead);
    }

    public Future<String> safeWriteAndRead(String text) {
        try {
            return exec.submit(() -> {
                rawWriteAndIgnore(text);
                return btRead();
            });
        } catch (Throwable e) {
            PUDSCLI.exception(e);
            connected = false;
        }
        return null;
    }

    public void rawWriteAndIgnore(String text) {
        btWrite(text);
    }

    public String rawWriteAndRead(String text) {
        rawWriteAndIgnore(text);
        return btRead();
    }

    private void inquireSpecific(RemoteDevice device) {
        if(AVAILABLE.containsKey(device))
            return;
        try {
            selected = device;
            AGENT.searchServices(null, SERVICES, selected, this);
            synchronized (lock) {
                lock.wait();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean inquire(boolean doShort) {
        try {
            AGENT.startInquiry(DiscoveryAgent.GIAC, this);
            synchronized (lock) {
                lock.wait();
            }

            if (doShort)
                return true;

            for (RemoteDevice value : DEVICES.values()) {
                inquireSpecific(value);
            }
            selected = null;


        } catch (Throwable e) {
            PUDSCLI.exception(e);
            return false;
        }
        return true;
    }

    public RemoteDevice getDevice(String name) {
        RemoteDevice ret;
        if ((ret = DEVICES.get(name)) != null)
            return ret;
        for (RemoteDevice value : DEVICES.values())
            if (value.getBluetoothAddress().equals(name))
                return value;
        return null;
    }

    @Override
    public boolean isInitialized() {
        return initialized && connected;
    }

    @Override
    public void selectECU(String code) {
        String[] split = code.split(":");
        selectedECUtx = split[0];
        selectedECUrx = split[1];

        try {
            exec.submit(() -> {
                String resp;
                if((resp = rawWriteAndRead("ATSH" + selectedECUtx)).equals("?"))
                    throw new RuntimeException("Failed changing TXH (1)");
                if((resp = rawWriteAndRead("ATCRA" + selectedECUrx)).equals("?"))
                    throw new RuntimeException("Failed changing RXH (1)");
                if((resp = rawWriteAndRead("ATFCSH" + selectedECUtx)).equals("?"))
                    throw new RuntimeException("Failed changing TXH (2)");
                if((resp = rawWriteAndRead("ATFCSD300000")).equals("?"))
                    throw new RuntimeException("Failed changing flow control data");
                if((resp = rawWriteAndRead("ATFCSM1")).equals("?"))
                    throw new RuntimeException("Failed changing flow control mode");
            }).get();
        } catch (Throwable e) {
            connected = false;
            PUDSCLI.exception(e); // todo kill the connection on such conditions
        }
    }

    @Override
    public Future<String> sendUDS(String uds) {
        return exec.submit(() -> rawWriteAndRead(uds));
    }

    @Override
    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
        String name;
        try {
            name = remoteDevice.getFriendlyName(false);
        } catch (Exception e) {
            name = remoteDevice.getBluetoothAddress();
        }
        DEVICES.put(name, remoteDevice);
    }

    @Override
    public void servicesDiscovered(int i, ServiceRecord[] serviceRecords) {
        for (int i1 = 0; i1 < serviceRecords.length; i1++) {
            ServiceRecord rec = serviceRecords[i1];

            String url = rec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            if (url == null)
                url = rec.getConnectionURL(ServiceRecord.AUTHENTICATE_NOENCRYPT, false);
            if (url == null)
                url = rec.getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT, false);

            if (url != null) {
                AVAILABLE.put(selected, url);
            }
        }
    }

    @Override
    public void serviceSearchCompleted(int i, int i1) {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void inquiryCompleted(int i) {
        synchronized (lock) {
            lock.notify();
        }
    }
}
