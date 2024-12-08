package de.saschat.poweruds.adapter;

import java.util.List;

public abstract class AbstractAdapterProvider {
    public abstract String getName();
    public abstract List<String> getDetected();
    public abstract AbstractAdapter getAdapter(String id);
}
