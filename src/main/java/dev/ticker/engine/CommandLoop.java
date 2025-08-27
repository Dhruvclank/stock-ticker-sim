package dev.ticker.engine;

import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

/** Parses commands from stdin and enqueues orders. */
public class CommandLoop implements Runnable {
    private final BlockingQueue<Order> orders;

    public CommandLoop(BlockingQueue<Order> orders) { this.orders = orders; }

    @Override public void run() {
        System.out.println("Commands: BUY <SYM> <QTY> | SELL <SYM> <QTY> | QUIT");
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toUpperCase(Locale.ROOT);
                try {
                    switch (cmd) {
                        case "BUY", "SELL" -> {
                            if (parts.length < 3) { System.out.println("Usage: BUY <SYM> <QTY>"); break; }
                            String sym = parts[1].toUpperCase(Locale.ROOT);
                            int qty = Integer.parseInt(parts[2]);
                            orders.put(new Order(cmd.equals("BUY") ? Order.Side.BUY : Order.Side.SELL, sym, qty));
                        }
                        case "QUIT", "EXIT" -> { System.out.println("Shutting down..."); return; }
                        default -> System.out.println("Unknown command");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }
}
