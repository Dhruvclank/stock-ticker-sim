package dev.ticker.engine;

public record Tick(String symbol, double price, int qty, long tsNanos) {}
