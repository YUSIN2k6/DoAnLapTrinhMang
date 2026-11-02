package com.vku.lanmonitor.admin.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AdminMessageDispatcher {
    private final List<Consumer<String>> handlers = new ArrayList<>();

    public void addHandler(Consumer<String> handler) {
        handlers.add(handler);
    }

    public void removeHandler(Consumer<String> handler) {
        handlers.remove(handler);
    }

    public void dispatch(String message) {
        for (Consumer<String> h : new ArrayList<>(handlers)) {
            try {
                h.accept(message);
            } catch (Exception e) {
                System.err.println("[Dispatcher] Handler error: " + e.getMessage());
            }
        }
    }
}