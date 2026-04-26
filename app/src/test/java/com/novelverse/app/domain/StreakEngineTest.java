package com.novelverse.app.domain;

import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.domain.gamification.StreakEngine;
import com.novelverse.app.domain.gamification.StreakEngine.StreakEvent;
import com.novelverse.app.domain.gamification.StreakEngine.StreakResult;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StreakEngine} — pure logic, no Android dependencies.
 *
 * Clock is injected as a long (milliseconds) so all tests are deterministic.
 */
public class StreakEngineTest {

    private static final long DAY_MS  = TimeUnit.DAYS.toMillis(1);
    private static final long BASE_MS = 1_700_000_000_000L; // fixed epoch anchor

    private UserStreakEntity baseStreak;

    @Before
    public void setup() {
        baseStreak = new UserStreakEntity();
        baseStreak.setUserId("user_123");
        baseStreak.setCurrentStreak(5);
        baseStreak.setLongestStreak(10);
        baseStreak.setLastActivityDate(BASE_MS);
        baseStreak.setGraceWindowStart(BASE_MS - TimeUnit.DAYS.toMillis(2));
        baseStreak.setFreezeMonthResetAt(BASE_MS - TimeUnit.DAYS.toMillis(15));
        baseStreak.setFreezeUsedThisMonth(0);
        baseStreak.setFreezesOwned(0);
    }

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    @Test
    public void firstEverActivity_bootstrapsAt1() {
        StreakResult result = StreakEngine.onActivity(null, BASE_MS);
        assertNotNull(result);
        assertEquals(1, result.getNewStreak());
        assertEquals(StreakEvent.EXTENDED, result.getEvent());
    }

    // ── Same-day guard ────────────────────────────────────────────────────────

    @Test
    public void activityWithin24h_returnsAlreadyToday() {
        long sameDay = BASE_MS + TimeUnit.HOURS.toMillis(6);
        StreakResult result = StreakEngine.onActivity(baseStreak, sameDay);
        assertEquals(StreakEvent.ALREADY_TODAY, result.getEvent());
        assertEquals(5, result.getNewStreak()); // unchanged
    }

    // ── Normal extension ──────────────────────────────────────────────────────

    @Test
    public void activityNextDay_extendsStreak() {
        long nextDay = BASE_MS + DAY_MS + TimeUnit.HOURS.toMillis(1);
        StreakResult result = StreakEngine.onActivity(baseStreak, nextDay);
        assertEquals(StreakEvent.EXTENDED, result.getEvent());
        assertEquals(6, result.getNewStreak());
    }

    // ── Reset after 48h ───────────────────────────────────────────────────────

    @Test
    public void activityAfter48hNoProtection_resetsStreak() {
        long twoDaysLater = BASE_MS + DAY_MS * 2 + TimeUnit.HOURS.toMillis(1);
        // No freezes, no grace remaining
        baseStreak.setFreezesOwned(0);
        StreakResult result = StreakEngine.onActivity(baseStreak, twoDaysLater);
        assertNotEquals(StreakEvent.EXTENDED, result.getEvent());
        // Streak should reset to 1
        assertEquals(1, result.getNewStreak());
    }

    // ── Freeze protection ─────────────────────────────────────────────────────

    @Test
    public void activityAfter25hWithFreeze_maintainsStreak() {
        long gapMs = BASE_MS + TimeUnit.HOURS.toMillis(26); // 26h — within freeze window
        baseStreak.setFreezesOwned(1);
        StreakResult result = StreakEngine.onActivity(baseStreak, gapMs);
        // Streak should NOT reset
        assertTrue("Streak should stay at 5 or extend",
                result.getNewStreak() >= 5);
    }

    // ── Constants ────────────────────────────────────────────────────────────

    @Test
    public void freezeCostInk_is50() {
        assertEquals(50, StreakEngine.FREEZE_COST_INK);
    }

    @Test
    public void shieldCostQuill_is150() {
        assertEquals(150, StreakEngine.SHIELD_COST_QUILL);
    }
}
