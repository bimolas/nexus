package com.example.nexus.model;

import java.time.LocalDateTime;
public class HistoryEntry {
    private int id;
    private int userId;
    private String title;
    private String url;
    private String faviconUrl;
    private int visitCount;
    private LocalDateTime lastVisit;
    private LocalDateTime firstVisit;


    public HistoryEntry() {
        this.firstVisit = LocalDateTime.now();
        this.lastVisit = LocalDateTime.now();
        this.visitCount = 1;
    }

    public HistoryEntry(String title, String url) {
        this();
        this.title = title;
        this.url = url;
    }

    public HistoryEntry(String title, String url, String faviconUrl) {
        this(title, url);
        this.faviconUrl = faviconUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { this.faviconUrl = faviconUrl; }

    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }

    public LocalDateTime getLastVisit() { return lastVisit; }
    public void setLastVisit(LocalDateTime lastVisit) { this.lastVisit = lastVisit; }

    public LocalDateTime getFirstVisit() { return firstVisit; }
    public void setFirstVisit(LocalDateTime firstVisit) { this.firstVisit = firstVisit; }

    // Added convenience method used by HistoryService for quick age checks (milliseconds since epoch)
    public long getLastVisitedAt() {
        if (lastVisit == null) return 0L;
        return java.time.ZonedDateTime.of(lastVisit, java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // Compatibility alias (some callers used a different method name)
    public long getLastVisitedMillis() {
        return getLastVisitedAt();
    }

    public String getDomain() {
        if (url == null || url.isEmpty()) return "";
        try {
            String domain = url.replaceFirst("^(https?://)?", "")
                               .replaceFirst("/.*$", "")
                               .replaceFirst("^www\\.", "");
            return domain;
        } catch (Exception e) {
            return url;
        }
    }

    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return getDomain();
    }

    public void incrementVisitCount() {
        this.visitCount++;
        this.lastVisit = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return getDisplayTitle();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HistoryEntry that = (HistoryEntry) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
