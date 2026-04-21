package com.novelverse.app.domain.models;

public class TrendingItem {
    private String query;
    private int rank;
    private int trendDirection; // -1 down, 0 flat, 1 up

    public TrendingItem() {}
    public TrendingItem(String query, int rank, int trendDirection) {
        this.query = query; this.rank = rank; this.trendDirection = trendDirection;
    }
    public String getQuery() { return query; }
    public void setQuery(String q) { this.query = q; }
    public int getRank() { return rank; }
    public void setRank(int r) { this.rank = r; }
    public int getTrendDirection() { return trendDirection; }
    public void setTrendDirection(int d) { this.trendDirection = d; }
    public String getTrendArrow() {
        if (trendDirection > 0) return "↑";
        if (trendDirection < 0) return "↓";
        return "→";
    }
    public int getTrendColor() {
        if (trendDirection > 0) return 0xFF10B981;
        if (trendDirection < 0) return 0xFFEF4444;
        return 0xFF94A3B8;
    }
}
