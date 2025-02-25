package de.saschat.poweruds.adapter;

import java.util.Observable;
import java.util.concurrent.Future;

public abstract class AbstractAdapter {
    public abstract String getName();
    public abstract void initialize(String options) throws AdapterInitializationException;
    public abstract boolean isInitialized();
    public abstract void selectECU(String code);
    public abstract Future<String> sendUDS(String uds);

    public abstract String getECU();
    public abstract String getArgs();
}
