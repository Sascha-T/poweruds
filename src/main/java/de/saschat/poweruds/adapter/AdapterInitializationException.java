package de.saschat.poweruds.adapter;

import java.io.IOException;

public class AdapterInitializationException extends Exception {
    public AdapterInitializationException(IOException message) {
        super(message);
    }
}
