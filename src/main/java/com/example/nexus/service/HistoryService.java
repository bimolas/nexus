package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.HistoryEntry;
import com.example.nexus.repository.HistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HistoryService {
    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private final HistoryRepository historyRepository;

    // Executor and cache to coalesce frequent history writes for the same URL
    private final ScheduledExecutorService historyExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nexus-history-executor");
        t.setDaemon(true);
        return t;
    });
    private final ConcurrentHashMap<String, Long> recentHistoryWrites = new ConcurrentHashMap<>();
    private final long HISTORY_WRITE_TTL_MS = 5000L; // 5s coalescing window

    public HistoryService(DIContainer container) {
        this.historyRepository = container.getOrCreate(HistoryRepository.class);
    }

    public List<HistoryEntry> getAllHistory() {
        try {
            return historyRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all history", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve history", e);
        }
    }

    public void addToHistory(String url, String title, String faviconUrl) {
        if (url == null || url.trim().isEmpty()) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "URL cannot be null or empty");
        }

        try {
            String normalized = url.trim();
            HistoryEntry existingEntry = historyRepository.findByUrl(normalized);

            long now = System.currentTimeMillis();
            Long lastWrite = recentHistoryWrites.get(normalized);

            if (existingEntry != null) {
                // If we recently wrote for this URL, coalesce the update
                if (lastWrite != null && (now - lastWrite) < HISTORY_WRITE_TTL_MS) {
                    // schedule a lightweight touch to increment visit count and update last_visit later
                    recentHistoryWrites.put(normalized, now);
                    int id = existingEntry.getId();
                    historyExecutor.schedule(() -> {
                        try {
                            historyRepository.touchVisit(id, LocalDateTime.now());
                        } catch (Exception e) {
                            logger.debug("Failed to touch visit for {}: {}", normalized, e.getMessage());
                        }
                    }, 800, TimeUnit.MILLISECONDS);
                    logger.debug("Coalesced history update for URL: {}", normalized);
                } else {
                    // update existing entry immediately
                    existingEntry.incrementVisitCount();
                    if (title != null && !title.trim().isEmpty()) {
                        existingEntry.setTitle(title);
                    }
                    if (faviconUrl != null) {
                        existingEntry.setFaviconUrl(faviconUrl);
                    }
                    historyRepository.update(existingEntry);
                    recentHistoryWrites.put(normalized, now);
                    logger.info("Updated history entry for URL: {}", normalized);
                }
            } else {
                // create new entry
                HistoryEntry entry = new HistoryEntry(title, normalized, faviconUrl);
                historyRepository.save(entry);
                recentHistoryWrites.put(normalized, now);
                logger.info("Added new history entry for URL: {}", normalized);
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error adding to history: " + url, e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_SAVE_ERROR,
                "Failed to add to history", e);
        }
    }

    public void addToHistory(String url, String title) {
        addToHistory(url, title, null);
    }

    public void deleteHistoryEntry(int id) {
        try {
            historyRepository.delete(id);
            logger.info("Deleted history entry with ID: " + id);
        } catch (Exception e) {
            logger.error("Error deleting history entry with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_DELETE_ERROR,
                "Failed to delete history entry", e);
        }
    }

    public void clearHistory() {
        try {
            historyRepository.clearAll();
            logger.info("Cleared all history");
        } catch (Exception e) {
            logger.error("Error clearing history", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to clear history", e);
        }
    }

    public List<HistoryEntry> searchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllHistory();
        }

        try {
            return historyRepository.search(query.trim());
        } catch (Exception e) {
            logger.error("Error searching history with query: " + query, e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to search history", e);
        }
    }

    // call this when application/service is shutting down to cleanly stop the executor
    public void shutdown() {
        try {
            historyExecutor.shutdownNow();
        } catch (Exception ignored) {}
    }
}
