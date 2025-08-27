package dev.ticker;

import dev.ticker.engine.*;
import dev.ticker.ui.SwingUI;

import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String[] symbols = {"AAPL", "MSFT", "TSLA", "AMZN"};
        boolean useGui = false;
        for (String a : args) if ("--gui".equalsIgnoreCase(a)) useGui = true;

        BlockingQueue<Tick>  tickQ  = new LinkedBlockingQueue<>(10_000);
        BlockingQueue<Order> orderQ = new LinkedBlockingQueue<>(1_000);

        Ticker    ticker    = new Ticker(tickQ);
        Portfolio portfolio = new Portfolio(100_000);
        OrderBook orderBook = new OrderBook(orderQ, ticker, portfolio);

        ExecutorService exec        = Executors.newCachedThreadPool();
        ScheduledExecutorService sd = Executors.newScheduledThreadPool(1);

        exec.submit(ticker);
        exec.submit(orderBook);

        // Market data generators
        List<MarketFeed> feeds = List.of(
            new MarketFeed("AAPL", 180.0, 0.002, 10, tickQ),
            new MarketFeed("MSFT", 420.0, 0.002, 10, tickQ),
            new MarketFeed("TSLA", 230.0, 0.004, 10, tickQ),
            new MarketFeed("AMZN", 150.0, 0.003, 10, tickQ)
        );
        for (MarketFeed f : feeds) exec.submit(f);

        if (useGui) {
            // GUI mode: no console view; buttons place orders
            SwingUI.launch(symbols, ticker, portfolio, orderQ);
            // Keep JVM alive until user closes the window or presses Ctrl+C
            // (simple sleep loop)
            while (true) Thread.sleep(1_000);
        } else {
            // Console mode: simple snapshots + command loop
            TickerView view = new TickerView(symbols, ticker, portfolio);
            sd.scheduleAtFixedRate(view, 0, 1000, TimeUnit.MILLISECONDS);
            new CommandLoop(orderQ).run(); // QUIT exits
        }

        // Shutdown (only reached from console mode)
        for (MarketFeed f : feeds) f.stop();
        exec.shutdownNow();
        sd.shutdownNow();
        exec.awaitTermination(3, TimeUnit.SECONDS);
        sd.awaitTermination(3, TimeUnit.SECONDS);
    }
}
