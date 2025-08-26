package dev.ticker.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe portfolio with cash, positions and P/L. */
public class Portfolio {
    public static class Position {
        public int qty;
        public double avg;
    }

    private final Map<String, Position> positions = new ConcurrentHashMap<>();
    private double cash;
    private double realizedPnL;

    public Portfolio(double startingCash) {
        this.cash = startingCash;
    }

    public synchronized void fill(Order o, double fillPx) {
        Position p = positions.computeIfAbsent(o.symbol(), s -> new Position());
        int q = o.qty();
        if (o.side() == Order.Side.BUY) {
            double notional = fillPx * q;
            cash -= notional;
            int newQty = p.qty + q;
            p.avg = newQty == 0 ? 0 : (p.avg * p.qty + notional) / newQty;
            p.qty = newQty;
        } else { // SELL
            int sell = Math.min(q, p.qty);
            double notional = fillPx * sell;
            cash += notional;
            realizedPnL += (fillPx - p.avg) * sell;
            p.qty -= sell;
            if (p.qty == 0) p.avg = 0;
        }
    }

    public synchronized double cash() { return cash; }
    public synchronized double realizedPnL() { return realizedPnL; }
    public synchronized Position pos(String sym) { return positions.get(sym); }
    public synchronized Map<String, Position> all() { return Map.copyOf(positions); }
}
