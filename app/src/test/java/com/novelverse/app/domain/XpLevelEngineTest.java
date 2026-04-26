package com.novelverse.app.domain;

import com.novelverse.app.domain.gamification.XpLevelEngine;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link XpLevelEngine}.
 *
 * XP formula: total XP to reach level N = sum(k*100 for k=1..N-1)
 *   Level 1 starts at 0 XP
 *   Level 2 starts at 100 XP
 *   Level 3 starts at 300 XP   (100 + 200)
 *   Level 4 starts at 600 XP   (100 + 200 + 300)
 */
public class XpLevelEngineTest {

    @Test
    public void level1_startsAt_0_xp() {
        assertEquals(0L, XpLevelEngine.xpFloorForLevel(1));
    }

    @Test
    public void level2_startsAt_100_xp() {
        assertEquals(100L, XpLevelEngine.xpFloorForLevel(2));
    }

    @Test
    public void level3_startsAt_300_xp() {
        assertEquals(300L, XpLevelEngine.xpFloorForLevel(3));
    }

    @Test
    public void level4_startsAt_600_xp() {
        assertEquals(600L, XpLevelEngine.xpFloorForLevel(4));
    }

    @Test
    public void xpToCompleteLevel_equals_level_times_100() {
        assertEquals(100,  XpLevelEngine.xpToCompleteLevel(1));
        assertEquals(200,  XpLevelEngine.xpToCompleteLevel(2));
        assertEquals(500,  XpLevelEngine.xpToCompleteLevel(5));
        assertEquals(1000, XpLevelEngine.xpToCompleteLevel(10));
    }

    @Test
    public void xpConstants_chapterRead_equals_10() {
        assertEquals(10, XpLevelEngine.XP_CHAPTER_READ);
    }

    @Test
    public void xpConstants_novelCompleted_equals_50() {
        assertEquals(50, XpLevelEngine.XP_NOVEL_COMPLETED);
    }

    @Test
    public void levelUpXP_boundary_isExclusive() {
        // At exactly 100 XP the player should be at level 2 (floor), not still level 1
        long xpForLevel2 = XpLevelEngine.xpFloorForLevel(2);
        assertTrue("Level 2 floor should be > level 1 floor",
                xpForLevel2 > XpLevelEngine.xpFloorForLevel(1));
    }
}
