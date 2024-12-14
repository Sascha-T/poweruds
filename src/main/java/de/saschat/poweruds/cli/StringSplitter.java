package de.saschat.poweruds.cli;

import java.util.LinkedList;
import java.util.List;

public final class StringSplitter {
    public static String[] split(String in) {
        List<String> args = new LinkedList<String>();
        StringBuilder current = new StringBuilder();
        boolean quotes = false;
        boolean escaped = false;
        for (char c : in.toCharArray()) {
            if (c == '\\') {
                escaped = true;
            } else {
                if(escaped) {
                    current.append(c);
                    escaped = false;
                } else {
                    if(c == ' ' && !quotes) {
                        args.add(current.toString());
                        current = new StringBuilder();
                    } else if(c == '"') {
                        quotes = !quotes;
                    } else {
                        current.append(c);
                    }
                }
            }
        }
        if(!current.isEmpty()) {
            args.add(current.toString());
        }
        return args.toArray(new String[args.size()]);
    }
}
