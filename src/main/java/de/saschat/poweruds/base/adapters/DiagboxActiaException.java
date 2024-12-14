package de.saschat.poweruds.base.adapters;

public class DiagboxActiaException extends RuntimeException {
    public DiagboxActiaException(int code) {
        super(String.format("Error Code %04X", code));
    }
}
