package de.saschat.poweruds.base.adapters;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AdapterIOException;
import de.saschat.poweruds.adapter.AdapterInitializationException;


import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class DiagboxAdapter extends AbstractAdapter {
    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private Process vciProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String ecuDescriptor;
    private boolean openSession;

    private static String _DESC(String in) {
        String[] part = in.split(":");
        return String.format("%4s%4s", part[1], part[0]).replaceAll(" ", "0");
    }

    @Override
    public String getName() {
        return "diagbox";
    }

    @Override
    public void initialize(String options) throws AdapterInitializationException {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("vciproxy");
            vciProcess = builder.start();
        } catch (IOException e) {
            throw new AdapterInitializationException(e);
        }
        reader = new BufferedReader(new InputStreamReader(vciProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(vciProcess.getOutputStream()));

        try {
            String a = reader.readLine();
            if(!Objects.equals(a, "info|rdy"))
                throw new IOException("Fail");
            writer.write("get_version" + System.lineSeparator());
            writer.flush();
            String b = reader.readLine();
            if(b == null || !b.startsWith("get_version"))
                throw new IOException("Fail");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        exec.scheduleAtFixedRate(this::doKeepAlive, 3000, 3000, TimeUnit.MILLISECONDS);
    }

    private void doKeepAlive() {
        if(!openSession || ecuDescriptor == null)
            return;
        boolean success = false;
        try {
            writer.write(String.format("write_and_read|%s|%s|1000%s", _DESC(ecuDescriptor), "3E00", System.lineSeparator()));
            writer.flush();
            if(reader.readLine().split("\\|").length > 2)
                success = true;
        } catch (IOException x) { reset(); }
        if(!success) {
            openSession = false;
        }
    }

    public void reset() {
        vciProcess.destroyForcibly();
        vciProcess = null;
    }

    public String sendCommand(String... cmds) throws AdapterIOException {
        try {
            writer.write(String.join("|", cmds) + System.lineSeparator());
            writer.flush();
            String read_ = reader.readLine();
            String[] read = read_.split("\\|");
            int a = Integer.parseInt(read[1]);
            if(a < 0)
                throw new DiagboxActiaException(a);
            return read_;
        } catch (IOException e) {
            reset();
            throw new AdapterIOException(e);
        }
    }

    public int getStatus(String ret) {
        return Integer.parseInt(ret.split("\\|")[1]);
    }

    @Override
    public boolean isInitialized() {
        return (vciProcess != null && vciProcess.isAlive());
    }

    @Override
    public void selectECU(String code) {
        if(!isInitialized())
            throw new RuntimeException("Adapter not initialized");
        ecuDescriptor = code;
        int a = 0;
        try {
            try {
                sendCommand("close_session");
            } catch(Exception ignored) {}
            Thread.sleep(100);
            if((a = getStatus(sendCommand("open_session"))) < 0)
                throw new DiagboxActiaException(a);
            Thread.sleep(100);
            if((a = getStatus(sendCommand("change_com_line", "17"))) < 0)
                throw new DiagboxActiaException(a);
            Thread.sleep(100);
            if((a = getStatus(sendCommand("bind_protocol", "0310E8"))) < 0)
                throw new DiagboxActiaException(a);
            Thread.sleep(100);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Future<String> sendUDS(String uds) {
        if(!isInitialized())
            throw new RuntimeException("Adapter not initialized");
        if(ecuDescriptor == null)
            throw new RuntimeException("ECU descriptor not set");
        return exec.submit(() -> {
            String text = sendCommand("write_and_read", _DESC(ecuDescriptor), uds, "1000");
            return text.split("\\|")[2];
        });
    }

    @Override
    public String getECU() {
        return ecuDescriptor;
    }

    @Override
    public String getArgs() {
        return "";
    }

}
