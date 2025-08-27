package dev.ticker.ui;

import dev.ticker.engine.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.BlockingQueue;

public final class SwingUI {
    private SwingUI() {}

    public static void launch(String[] symbols, Ticker ticker, Portfolio portfolio,
                              BlockingQueue<Order> orderQ) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Stock Ticker Simulator");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(900, 380);
            f.setLayout(new BorderLayout());

            // ---- Table (live market + positions) ----
            String[] cols = {"Symbol", "Last", "VWAP60", "Vol60", "Pos", "Avg", "UPL"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable table = new JTable(model);
            table.setRowHeight(22);
            for (String s : symbols) model.addRow(new Object[]{s, "-", "-", "-", 0, "0.00", "0.00"});
            f.add(new JScrollPane(table), BorderLayout.CENTER);

            // ---- Controls (symbol + qty + BUY/SELL) ----
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JComboBox<String> symbolBox = new JComboBox<>(symbols);
            JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
            JButton buy = new JButton("BUY");
            JButton sell = new JButton("SELL");
            JLabel status = new JLabel("Ready");

            buy.addActionListener(e -> enqueue(orderQ, Order.Side.BUY, (String) symbolBox.getSelectedItem(),
                                               (Integer) qty.getValue(), status));
            sell.addActionListener(e -> enqueue(orderQ, Order.Side.SELL, (String) symbolBox.getSelectedItem(),
                                                (Integer) qty.getValue(), status));
            controls.add(new JLabel("Symbol:")); controls.add(symbolBox);
            controls.add(new JLabel("Qty:")); controls.add(qty);
            controls.add(buy); controls.add(sell);
            controls.add(Box.createHorizontalStrut(20)); controls.add(status);
            f.add(controls, BorderLayout.SOUTH);

            // ---- Refresh timer (UI thread) ----
            new Timer(750, e -> {
                double totalUPL = 0.0;
                synchronized (portfolio) {
                    for (int i = 0; i < symbols.length; i++) {
                        String s = symbols[i];
                        double last = ticker.lastPrice(s);
                        double vwap = ticker.vwap60s(s);
                        long   vol  = ticker.volume60s(s);
                        Portfolio.Position p = portfolio.pos(s);
                        int q = (p == null) ? 0 : p.qty;
                        double avg = (p == null) ? 0.0 : p.avg;
                        double upl = (Double.isNaN(last) ? 0.0 : (last - avg) * q);
                        totalUPL += upl;

                        model.setValueAt(Double.isNaN(last) ? "-" : String.format("%.2f", last), i, 1);
                        model.setValueAt(Double.isNaN(vwap) ? "-" : String.format("%.2f", vwap), i, 2);
                        model.setValueAt(vol, i, 3);
                        model.setValueAt(q, i, 4);
                        model.setValueAt(String.format("%.2f", avg), i, 5);
                        model.setValueAt(String.format("%.2f", upl), i, 6);
                    }
                }
                f.setTitle(String.format("Stock Ticker Simulator — Unrealized P/L: %.2f", totalUPL));
            }).start();

            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private static void enqueue(BlockingQueue<Order> q, Order.Side side, String sym, int qty, JLabel status) {
        try {
            q.put(new Order(side, sym, qty));
            status.setText(String.format("Queued %s %s x%d", side, sym, qty));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            status.setText("Interrupted");
        }
    }
}
