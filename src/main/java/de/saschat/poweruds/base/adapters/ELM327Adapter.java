package de.saschat.poweruds.base.adapters;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AdapterInitializationException;
import de.saschat.poweruds.cli.PUDSCLI;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

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
    private InputStream in;
    private OutputStream out;


    static {
        try {
            DEVICE = LocalDevice.getLocalDevice();
            AGENT  = DEVICE.getDiscoveryAgent();
            initialized = true;
        } catch (BluetoothStateException e) {
            PUDSCLI.exception(e);
        }
    }

    private static final String[] INIT_COMMANDS = new String[] {
            "AT L1", "AT Z", "AT L1", "AT WS", "AT D", "AT E0", /*"AT L0" no,*/ "AT H0", "AT S0", "AT AL", "AT V0", "AT SP6"
    };

    public Map<String, RemoteDevice> DEVICES = new HashMap<>();
    public Map<RemoteDevice, String> AVAILABLE = new HashMap<>();

    public Queue<Character> IN_QUEUE = new ArrayBlockingQueue<>(1024);
    public Thread READ_QUEUE;
    public AtomicInteger READ_QUEUE_INDEX = new AtomicInteger(0);

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

        RemoteDevice dev;
        boolean dnq = (dev = getDevice(options)) != null;
        if (!dnq && !inquire(true))
            return;
        if (dev != null || (dev = getDevice(options)) != null) {
            System.out.println("Dev 1");
            if (connect(dev)) {
                System.out.println("Dev 2");
                for (String initCommand : INIT_COMMANDS) { // initialize state
                    if (writeAndRead(initCommand).equals("?"))
                        return;
                }
                initialized = true;
            }
        }
    }

    public boolean connect(RemoteDevice device) {
        if(AVAILABLE.get(device) == null)
            inquireSpecific(device);
        // connect
        try {
            connection = (StreamConnection) Connector.open(AVAILABLE.get(device));
            in = connection.openInputStream();
            out = connection.openOutputStream();

            READ_QUEUE = new Thread(() -> {
                try {
                    while(true) {
                        boolean reading = true;
                        if(READ_QUEUE_INDEX.get() > 0)
                            while(reading) {
                                char read = (char) in.read();
                                System.out.println("char: " + read);
                                IN_QUEUE.add(read);
                                if(read == '\n')
                                    reading = false;
                            }
                    }
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            READ_QUEUE.start();

            connected = true;
            return true;
        } catch (IOException e) {
            PUDSCLI.exception(e);
        }
        return false;
    }

    public void writeCommand(String text) {
        try {
            System.out.println("out: " + text);
            byte[] w = text.getBytes(StandardCharsets.US_ASCII);
            out.write(w, 0, w.length);
            out.write('\n');


        } catch (IOException e) {
            PUDSCLI.exception(e);
            throw new RuntimeException(e);
        }
    }
    public String readReply() {
        StringBuilder text = new StringBuilder();
        try {
            READ_QUEUE_INDEX.addAndGet(1);
            for (int i = 0; i < 20; i++) {
                if(!IN_QUEUE.isEmpty()) {
                    if(IN_QUEUE.contains('\n')) {
                        // consume one line

                        Character character = null;
                        while((character = IN_QUEUE.remove()) != null && character != '\n')
                            text.append(character);

                        return text.toString();
                    }
                }
                Thread.sleep(100);
            }
            return text.toString();
        } catch (Throwable e) {
            PUDSCLI.exception(e);
            throw new RuntimeException(e);
        }
    }

    public String writeAndRead(String text) {
        writeCommand(text);
        return readReply();
    }

    private void inquireSpecific(RemoteDevice device) {
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
        return initialized && connected && READ_QUEUE != null && READ_QUEUE.isAlive();
    }

    @Override
    public void selectECU(String code) {
        String[] split = code.split(":");
        String txh = split[0];
        String rxh = split[1];

        String resp;
        if((resp = writeAndRead("ATSH" + txh)).equals("?"))
            throw new RuntimeException("Failed changing TXH (1)");
        if((resp = writeAndRead("ATCRA" + rxh)).equals("?"))
            throw new RuntimeException("Failed changing RXH (1)");
        if((resp = writeAndRead("ATFCSH" + txh)).equals("?"))
            throw new RuntimeException("Failed changing TXH (2)");
        if((resp = writeAndRead("ATFCSD300000")).equals("?"))
            throw new RuntimeException("Failed changing flow control data");
        if((resp = writeAndRead("ATFCSM1")).equals("?"))
            throw new RuntimeException("Failed changing flow control mode");
    }

    @Override
    public Future<String> sendUDS(String uds) {
        return exec.submit(() -> writeAndRead(uds));
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
