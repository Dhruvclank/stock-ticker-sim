package dev.ticker.engine;

public record Order(Side side, String symbol, int qty) {
    public enum Side { BUY, SELL }
}
