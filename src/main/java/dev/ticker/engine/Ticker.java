package dev.ticker.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/** Consumes ticks and maintains last price, 60s VWAP and rolling volume. */
public class Ticker implements Runnable {
    private final Map<String, Deque<Tick>> window = new ConcurrentHashMap<>();
    private final Map<String, Double> last = new ConcurrentHashMap<>();
    private final Map<String, Long> vol60s = new ConcurrentHashMap<>();
    private final BlockingQueue<Tick> in;

    public Ticker(BlockingQueue<Tick> in) { this.in = in; }

    public double lastPrice(String sym) { return last.getOrDefault(sym, Double.NaN); }
    public long volume60s(String sym) { return vol60s.getOrDefault(sym, 0L); }

    /** On-demand VWAP over the last ~60 seconds. */
    public double vwap60s(String sym) {
        Deque<Tick> dq = window.get(sym);
        if (dq == null || dq.isEmpty()) return Double.NaN;
        long cutoff = System.nanoTime() - 60_000_000_000L;
        synchronized (dq) {
            while (!dq.isEmpty() && dq.peekFirst().tsNanos() < cutoff) dq.pollFirst();
            long vol = 0L; double pv = 0.0;
            for (Tick t : dq) { vol += t.qty(); pv += t.price() * t.qty(); }
            return vol == 0 ? Double.NaN : pv / vol;
        }
    }

    private void onTick(Tick t) {
        last.put(t.symbol(), t.price());
        Deque<Tick> dq = window.computeIfAbsent(t.symbol(), k -> new ArrayDeque<>());
        synchronized (dq) {
            dq.addLast(t);
            long cutoff = System.nanoTime() - 60_000_000_000L;
            long vol = 0;
            while (!dq.isEmpty() && dq.peekFirst().tsNanos() < cutoff) dq.pollFirst();
            for (Tick x : dq) vol += x.qty();
            vol60s.put(t.symbol(), vol);
        }
    }

    @Override public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) onTick(in.take());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
