package dev.ticker.engine;

import java.util.concurrent.BlockingQueue;

/** Consumes orders, executes at latest market price, updates portfolio. */
public class OrderBook implements Runnable {
    private final BlockingQueue<Order> in;
    private final Ticker ticker;
    private final Portfolio portfolio;

    public OrderBook(BlockingQueue<Order> in, Ticker ticker, Portfolio portfolio) {
        this.in = in; this.ticker = ticker; this.portfolio = portfolio;
    }

    @Override public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Order o = in.take();
                double px = ticker.lastPrice(o.symbol());
                if (Double.isNaN(px)) {
                    System.out.println("No market price yet for " + o.symbol() + ", order ignored.");
                    continue;
                }
                // reject SELL if you don't own enough
                if (o.side() == Order.Side.SELL) {
                    Portfolio.Position pp = portfolio.pos(o.symbol());
                    int have = (pp == null ? 0 : pp.qty);
                    if (have <= 0) {
                        System.out.printf("REJECT SELL %s x%d — no position%n", o.symbol(), o.qty());
                        continue;
                    }
                    if (o.qty() > have) {
                        System.out.printf("REJECT SELL %s x%d — exceeds position (%d)%n", o.symbol(), o.qty(), have);
                        continue;
                    }
                }
                portfolio.fill(o, px);
                System.out.printf("FILLED %s %s x%d @ %.2f%n", o.side(), o.symbol(), o.qty(), px);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
