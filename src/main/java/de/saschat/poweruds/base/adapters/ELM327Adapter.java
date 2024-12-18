package de.saschat.poweruds.base.adapters;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AdapterInitializationException;
import de.saschat.poweruds.cli.PUDSCLI;

import javax.bluetooth.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class ELM327Adapter extends AbstractAdapter implements DiscoveryListener {
    private final Object lock = new Object();

    private static javax.bluetooth.UUID[] SERVICES = new javax.bluetooth.UUID[]{new javax.bluetooth.UUID("0000110100001000800000805F9B34FB", false)};
    private static LocalDevice DEVICE;
    private static DiscoveryAgent AGENT;

    private static boolean initialized = false;
    private boolean connected = false;
    private RemoteDevice selected;

    static {
        try {
            DEVICE = LocalDevice.getLocalDevice();
            AGENT  = DEVICE.getDiscoveryAgent();
            initialized = true;
        } catch (BluetoothStateException e) {
            PUDSCLI.exception(e);
        }
    }

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

        if (!inquire(true))
            return;
        RemoteDevice dev;
        if ((dev = getDevice(options)) != null)
            if (connect(dev))
                initialized = true;
    }

    public boolean connect(RemoteDevice device) {
        // connect
        return false;
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
                selected = value;
                AGENT.searchServices(null, SERVICES, selected, this);
                synchronized (lock) {
                    lock.wait();
                }
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

    }

    @Override
    public Future<String> sendUDS(String uds) {
        return null;
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
