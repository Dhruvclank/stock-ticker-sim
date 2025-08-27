package dev.ticker.engine;

/** Prints a live table of market + positions periodically (called by a scheduler). */
public class TickerView implements Runnable {
    private final String[] symbols;
    private final Ticker ticker;
    private final Portfolio portfolio;

    public TickerView(String[] symbols, Ticker ticker, Portfolio portfolio) {
        this.symbols = symbols; this.ticker = ticker; this.portfolio = portfolio;
    }

    @Override public void run() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nSYMBOL  LAST     VWAP60  VOL60   POS   AVG      UPL\n");
        double totalUPL = 0.0;

        synchronized (portfolio) {
            for (String s : symbols) {
                double last = ticker.lastPrice(s);
                double vw = ticker.vwap60s(s);
                long vol = ticker.volume60s(s);
                Portfolio.Position p = portfolio.pos(s);
                int q = p == null ? 0 : p.qty;
                double avg = p == null ? 0.0 : p.avg;
                double upl = (Double.isNaN(last) ? 0.0 : (last - avg) * q);
                totalUPL += upl;
                sb.append(String.format("%-6s  %-7s %-7s %-6d  %-4d  %-7.2f  %-8.2f%n",
                        s,
                        Double.isNaN(last) ? "-" : String.format("%.2f", last),
                        Double.isNaN(vw)   ? "-" : String.format("%.2f", vw),
                        vol, q, avg, upl));
            }
            sb.append(String.format("TOTAL  cash=%.2f  realized=%.2f  unrealized=%.2f%n",
                    portfolio.cash(), portfolio.realizedPnL(), totalUPL));
        }
        System.out.print(sb);
    }
}
