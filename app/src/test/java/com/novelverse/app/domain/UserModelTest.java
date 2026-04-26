package com.novelverse.app.domain;

import com.novelverse.app.domain.models.User;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link User} — validates default values and key business rules.
 */
public class UserModelTest {

    @Test
    public void defaultConstructor_setsFreeTier() {
        User user = new User();
        assertEquals("free", user.getSubscriptionTier());
    }

    @Test
    public void defaultConstructor_zeroPurchaseBalance() {
        User user = new User();
        assertEquals(0, user.getPointsBalance());
        assertEquals(0.0, user.getTotalSpent(), 0.001);
    }

    @Test
    public void defaultConstructor_notificationsEnabledByDefault() {
        User user = new User();
        assertTrue(user.isNotificationsEnabled());
        assertTrue(user.isPushNotifications());
        assertTrue(user.isEmailNotifications());
    }

    @Test
    public void defaultConstructor_marketingEmailsDisabledByDefault() {
        User user = new User();
        assertFalse(user.isMarketingEmails());
    }

    @Test
    public void defaultConstructor_defaultFontSize16() {
        User user = new User();
        assertEquals(16, user.getFontSize());
    }

    @Test
    public void setters_updateFields() {
        User user = new User();
        user.setId("abc-123");
        user.setUsername("novelreader");
        user.setEmail("test@example.com");
        user.setRole("author");

        assertEquals("abc-123",      user.getId());
        assertEquals("novelreader",  user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("author",       user.getRole());
    }

    @Test
    public void defaultPrivacySetting_isPublic() {
        User user = new User();
        assertEquals("public", user.getPrivacySetting());
    }

    @Test
    public void defaultFollowerCount_isZero() {
        User user = new User();
        assertEquals(0, user.getFollowersCount());
        assertEquals(0, user.getFollowingCount());
    }

    @Test
    public void defaultTtsSpeed_is1x() {
        User user = new User();
        assertEquals(1.0f, user.getTtsSpeed(), 0.001f);
    }

    @Test
    public void availableForPayout_defaultsToZero() {
        User user = new User();
        assertEquals(0.0, user.getAvailableForPayout(), 0.001);
    }

    @Test
    public void isBanned_defaultsFalse() {
        User user = new User();
        assertFalse(user.isBanned());
    }
}
