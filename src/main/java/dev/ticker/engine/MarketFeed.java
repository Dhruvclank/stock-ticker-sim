package dev.ticker.engine;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** Produces random “live” ticks for one symbol. */
public class MarketFeed implements Runnable {
    private final String symbol;
    private final BlockingQueue<Tick> out;
    private final Random rnd = new Random();
    private volatile boolean running = true;

    private double price;      // current mid
    private final double vol;  // volatility multiplier
    private final int tps;     // ticks per second

    public MarketFeed(String symbol, double startPrice, double volatility, int ticksPerSecond,
                      BlockingQueue<Tick> out) {
        this.symbol = symbol;
        this.price = startPrice;
        this.vol = volatility;
        this.tps = ticksPerSecond;
        this.out = out;
    }

    public void stop() { running = false; }

    @Override public void run() {
        long sleepNanos = 1_000_000_000L / Math.max(1, tps);
        while (running && !Thread.currentThread().isInterrupted()) {
            double step = rnd.nextGaussian() * vol;           // random walk
            price = Math.max(0.01, price * (1.0 + step));
            int qty = 1 + rnd.nextInt(50);
            long now = System.nanoTime();
            try {
                out.put(new Tick(symbol, price, qty, now));
                TimeUnit.NANOSECONDS.sleep(sleepNanos);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
