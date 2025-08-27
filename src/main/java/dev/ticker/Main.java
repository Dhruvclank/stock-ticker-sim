package dev.ticker;

import dev.ticker.engine.*;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String[] symbols = {"AAPL", "MSFT", "TSLA", "AMZN"};

        BlockingQueue<Tick>  tickQ  = new LinkedBlockingQueue<>(10_000);
        BlockingQueue<Order> orderQ = new LinkedBlockingQueue<>(1_000);

        Ticker ticker = new Ticker(tickQ);
        Portfolio portfolio = new Portfolio(100_000); // starting cash
        OrderBook orderBook = new OrderBook(orderQ, ticker, portfolio);
        TickerView view = new TickerView(symbols, ticker, portfolio);

        ExecutorService exec = Executors.newCachedThreadPool();
        ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);

        exec.submit(ticker);
        exec.submit(orderBook);
        sched.scheduleAtFixedRate(view, 500, 750, TimeUnit.MILLISECONDS);

        List<MarketFeed> feeds = List.of(
            new MarketFeed("AAPL", 180.0, 0.002, 10, tickQ),
            new MarketFeed("MSFT", 420.0, 0.002, 10, tickQ),
            new MarketFeed("TSLA", 230.0, 0.004, 10, tickQ),
            new MarketFeed("AMZN", 150.0, 0.003, 10, tickQ)
        );
        for (MarketFeed f : feeds) exec.submit(f);

        new CommandLoop(orderQ).run(); // blocks until QUIT

        for (MarketFeed f : feeds) f.stop();
        exec.shutdownNow();
        sched.shutdownNow();
        exec.awaitTermination(3, TimeUnit.SECONDS);
        sched.awaitTermination(3, TimeUnit.SECONDS);
    }
}
