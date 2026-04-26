package com.novelverse.app.data;

import com.novelverse.app.presentation.notifications.NotificationsFragment.NotificationItem;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link NotificationItem} — the data model used by the
 * notifications list. Validates field accessors and mutable read state.
 */
public class NotificationItemTest {

    private NotificationItem makeItem(boolean read) {
        return new NotificationItem(
                "id-1",
                "new_chapter",
                "New chapter available",
                "Chapter 42 of your favourite novel is out!",
                read,
                "2025-01-01T12:00:00Z"
        );
    }

    @Test
    public void getId_returnsCorrectValue() {
        assertEquals("id-1", makeItem(false).getId());
    }

    @Test
    public void getType_returnsCorrectValue() {
        assertEquals("new_chapter", makeItem(false).getType());
    }

    @Test
    public void getTitle_returnsCorrectValue() {
        assertEquals("New chapter available", makeItem(false).getTitle());
    }

    @Test
    public void getBody_returnsCorrectValue() {
        assertTrue(makeItem(false).getBody().contains("Chapter 42"));
    }

    @Test
    public void isRead_falseByDefault_whenPassedFalse() {
        assertFalse(makeItem(false).isRead());
    }

    @Test
    public void isRead_trueWhenPassedTrue() {
        assertTrue(makeItem(true).isRead());
    }

    @Test
    public void setRead_togglesState() {
        NotificationItem item = makeItem(false);
        assertFalse(item.isRead());
        item.setRead(true);
        assertTrue(item.isRead());
    }

    @Test
    public void getCreatedAt_returnsIsoString() {
        assertEquals("2025-01-01T12:00:00Z", makeItem(false).getCreatedAt());
    }
}
