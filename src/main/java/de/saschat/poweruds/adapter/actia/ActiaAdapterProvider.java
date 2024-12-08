package de.saschat.poweruds.adapter.actia;

import de.saschat.poweruds.adapter.AbstractAdapter;
import de.saschat.poweruds.adapter.AbstractAdapterProvider;

import java.util.List;

public class ActiaAdapterProvider extends AbstractAdapterProvider {
    @Override
    public String getName() {
        return "actia";
    }

    @Override
    public List<String> getDetected() {
        return List.of();
    }

    @Override
    public AbstractAdapter getAdapter(String id) {
        return null;
    }
}
