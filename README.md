# Stock Ticker Simulator

A Java project that simulates a real-time trading environment.  
Generates live market ticks (random-walk or real prices via Finnhub),  
lets you place **BUY** and **SELL** orders, and tracks your **portfolio and P/L** in real time.  

👉 Features both a **console mode** and a **Swing GUI** with a live table + buttons.

---

## ✨ Features
- Core Java concurrency (Executors, BlockingQueue)
- Order book + portfolio with cash, average price, realized/unrealized P/L
- Random-walk price feed (offline) or live **Finnhub API** prices (optional)
- GUI: Swing table + Buy/Sell buttons
- Safe trading: **no shorting** and no selling more than you own

---

## 🚀 Quick Start

Clone and build:

```bash
git clone https://github.com/dhruvclank/stock-ticker-sim.git
cd stock-ticker-sim
mvn clean package
