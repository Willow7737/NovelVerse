-- ============================================================
-- NovelVerse Gamification System 2.0 — Supabase Migration
-- Run in Supabase SQL Editor (Dashboard → SQL Editor → New query)
-- ============================================================

-- ── achievements catalog ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.achievements (
    id              TEXT        PRIMARY KEY,
    title           TEXT        NOT NULL,
    description     TEXT,
    asset_name      TEXT,
    rarity          TEXT        NOT NULL DEFAULT 'COMMON',   -- COMMON | RARE | EPIC | LEGENDARY
    category        TEXT        NOT NULL DEFAULT 'READER',   -- READER | WRITER | STREAK | SOCIAL | SUPPORTER
    xp_reward       INTEGER     NOT NULL DEFAULT 0,
    ink_reward      INTEGER     NOT NULL DEFAULT 0,
    quill_reward    INTEGER     NOT NULL DEFAULT 0,
    is_visible      BOOLEAN     NOT NULL DEFAULT TRUE,
    target_value    INTEGER     NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Anyone can read the catalog; only service role can write
ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "achievements_select" ON public.achievements
    FOR SELECT USING (true);

-- ── user_achievements ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_achievements (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    achievement_id      TEXT        NOT NULL REFERENCES public.achievements(id) ON DELETE CASCADE,
    is_unlocked         BOOLEAN     NOT NULL DEFAULT FALSE,
    unlocked_at         TIMESTAMPTZ,
    current_progress    INTEGER     NOT NULL DEFAULT 0,
    reward_claimed      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON public.user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_unlocked ON public.user_achievements(user_id, is_unlocked);

ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
CREATE POLICY "user_achievements_select" ON public.user_achievements
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "user_achievements_upsert" ON public.user_achievements
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "user_achievements_update" ON public.user_achievements
    FOR UPDATE USING (auth.uid() = user_id);

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_achievements_updated_at
    BEFORE UPDATE ON public.user_achievements
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── user_currency ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_currency (
    user_id         UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    ink_balance     INTEGER     NOT NULL DEFAULT 0 CHECK (ink_balance >= 0),
    quill_balance   INTEGER     NOT NULL DEFAULT 0 CHECK (quill_balance >= 0),
    version         BIGINT      NOT NULL DEFAULT 0,
    last_synced_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.user_currency ENABLE ROW LEVEL SECURITY;
CREATE POLICY "user_currency_select" ON public.user_currency
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "user_currency_upsert" ON public.user_currency
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "user_currency_update" ON public.user_currency
    FOR UPDATE USING (auth.uid() = user_id)
    -- Server-wins: only accept if server version >= client version
    WITH CHECK (version >= (SELECT version FROM public.user_currency WHERE user_id = auth.uid()));

CREATE TRIGGER user_currency_updated_at
    BEFORE UPDATE ON public.user_currency
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── token_transactions ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.token_transactions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type            TEXT        NOT NULL,   -- EARN_READING | EARN_ACHIEVEMENT | EARN_STREAK | SPEND_FREEZE | SPEND_SHIELD | SPEND_STORE | PURCHASE_QUILL | BONUS_DAILY
    ink_delta       INTEGER     NOT NULL DEFAULT 0,
    quill_delta     INTEGER     NOT NULL DEFAULT 0,
    ink_after       INTEGER     NOT NULL DEFAULT 0,
    quill_after     INTEGER     NOT NULL DEFAULT 0,
    reason          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_token_tx_user_id ON public.token_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_token_tx_created_at ON public.token_transactions(user_id, created_at DESC);

ALTER TABLE public.token_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "token_tx_select" ON public.token_transactions
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "token_tx_insert" ON public.token_transactions
    FOR INSERT WITH CHECK (auth.uid() = user_id);
-- Transactions are immutable — no UPDATE/DELETE policies

-- ── user_levels ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_levels (
    user_id             UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    xp_total            BIGINT      NOT NULL DEFAULT 0,
    current_level       INTEGER     NOT NULL DEFAULT 1,
    xp_in_level         INTEGER     NOT NULL DEFAULT 0,
    xp_level_target     INTEGER     NOT NULL DEFAULT 100,
    badge_slots         INTEGER     NOT NULL DEFAULT 1,
    equipped_badge_ids  TEXT,       -- comma-separated achievement IDs
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.user_levels ENABLE ROW LEVEL SECURITY;
CREATE POLICY "user_levels_select" ON public.user_levels
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "user_levels_upsert" ON public.user_levels
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "user_levels_update" ON public.user_levels
    FOR UPDATE USING (auth.uid() = user_id);

CREATE TRIGGER user_levels_updated_at
    BEFORE UPDATE ON public.user_levels
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── user_streaks ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_streaks (
    user_id                         UUID        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    current_streak                  INTEGER     NOT NULL DEFAULT 0,
    longest_streak                  INTEGER     NOT NULL DEFAULT 0,
    last_activity_date              TIMESTAMPTZ,
    grace_window_start              TIMESTAMPTZ,
    grace_used_in_window            BOOLEAN     NOT NULL DEFAULT FALSE,
    freeze_expires_at               TIMESTAMPTZ,
    free_freezes_used_this_month    INTEGER     NOT NULL DEFAULT 0,
    freeze_month_reset_at           TIMESTAMPTZ,
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.user_streaks ENABLE ROW LEVEL SECURITY;
CREATE POLICY "user_streaks_select" ON public.user_streaks
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "user_streaks_upsert" ON public.user_streaks
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "user_streaks_update" ON public.user_streaks
    FOR UPDATE USING (auth.uid() = user_id);

CREATE TRIGGER user_streaks_updated_at
    BEFORE UPDATE ON public.user_streaks
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ── daily_caps ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.daily_caps (
    id                          TEXT        PRIMARY KEY,  -- user_id + '_' + date_key
    user_id                     UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date_key                    DATE        NOT NULL,
    ink_from_reading            INTEGER     NOT NULL DEFAULT 0,
    reading_cap                 INTEGER     NOT NULL DEFAULT 500,
    achievements_today          INTEGER     NOT NULL DEFAULT 0,
    UNIQUE (user_id, date_key)
);

CREATE INDEX IF NOT EXISTS idx_daily_caps_user_date ON public.daily_caps(user_id, date_key);

ALTER TABLE public.daily_caps ENABLE ROW LEVEL SECURITY;
CREATE POLICY "daily_caps_select" ON public.daily_caps
    FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "daily_caps_upsert" ON public.daily_caps
    FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "daily_caps_update" ON public.daily_caps
    FOR UPDATE USING (auth.uid() = user_id);

-- ── Seed all 28 achievements ──────────────────────────────────────────────────
INSERT INTO public.achievements (id, title, description, asset_name, rarity, category, xp_reward, ink_reward, quill_reward, is_visible, target_value) VALUES
-- READER
('first_page',      'First Page',       'Read your first chapter',                          'achievement_first_page.png',       'COMMON',    'READER',    50,   20,  0,  false, 1),
('bookworm',        'Bookworm',         'Read 10 novels',                                   'achievement_bookworm.png',          'RARE',      'READER',   200,  100,  0,  true,  10),
('scholar',         'Scholar',          'Read 25 novels',                                   'achievement_scholar.png',           'RARE',      'READER',   400,  200,  0,  true,  25),
('sage',            'Sage',             'Read 50 novels',                                   'achievement_sage.png',              'EPIC',      'READER',   750,  350,  5,  true,  50),
('lorekeeper',      'Lorekeeper',       'Read 100 novels',                                  'achievement_lorekeeper.png',        'LEGENDARY', 'READER',  2000,  500, 20,  true,  100),
('completionist',   'Completionist',    'Finish a novel (read all chapters)',                'achievement_completionist.png',     'RARE',      'READER',   300,  150,  0,  false, 1),
('finished_strong', 'Finished Strong',  'Complete 5 novels',                                'achievement_finished_strong.png',   'EPIC',      'READER',   600,  300,  5,  true,  5),
('night_reader',    'Night Reader',     'Read between midnight and 4 AM',                   'achievement_night_reader.png',      'COMMON',    'READER',    75,   30,  0,  false, 1),
-- WRITER
('first_words',     'First Words',      'Publish your first chapter',                       'achievement_first_words.png',       'COMMON',    'WRITER',   100,   50,  0,  false, 1),
('published',       'Published',        'Publish a chapter with 1000+ words and 5 reads',   'achievement_published.png',         'RARE',      'WRITER',   250,  100,  0,  true,  1),
('rising_star',     'Rising Star',      'Reach 100 total reads across your novels',         'achievement_rising_star.png',       'RARE',      'WRITER',   350,  150,  0,  true,  100),
('going_viral',     'Going Viral',      'Reach 1,000 total reads',                          'achievement_going_viral.png',       'EPIC',      'WRITER',   800,  300, 10,  true,  1000),
('bestseller',      'Bestseller',       'Reach 10,000 total reads',                         'achievement_bestseller.png',        'LEGENDARY', 'WRITER',  3000,  500, 50,  true,  10000),
('prolific',        'Prolific',         'Publish 10 chapters',                              'achievement_prolific.png',          'RARE',      'WRITER',   400,  200,  0,  true,  10),
-- STREAK
('streak_3',        'On a Roll',        'Read 3 days in a row',                             'achievement_streak_3.png',          'COMMON',    'STREAK',    75,   30,  0,  true,  3),
('streak_7',        'Week Warrior',     'Read 7 days in a row',                             'achievement_streak_7.png',          'RARE',      'STREAK',   200,  100,  0,  true,  7),
('streak_30',       'Monthly Devotee',  'Read 30 days in a row',                            'achievement_streak_30.png',         'EPIC',      'STREAK',   750,  300,  5,  true,  30),
('streak_100',      'Century Reader',   'Read 100 days in a row',                           'achievement_streak_100.png',        'LEGENDARY', 'STREAK',  5000,  500, 100, true,  100),
-- SOCIAL
('bookmarked',      'Bookmarked',       'Add your first bookmark',                          'achievement_bookmarked.png',        'COMMON',    'SOCIAL',    25,   10,  0,  false, 1),
('critic',          'Critic',           'Write 10 reviews (50+ chars each)',                 'achievement_critic.png',            'RARE',      'SOCIAL',   250,  100,  0,  true,  10),
('trusted_voice',   'Trusted Voice',    'Have 25 reviews liked by others',                  'achievement_trusted_voice.png',     'EPIC',      'SOCIAL',   600,  250,  5,  true,  25),
('connected',       'Connected',        'Follow 10 authors',                                 'achievement_connected.png',         'COMMON',    'SOCIAL',   100,   50,  0,  true,  10),
('trendsetter',     'Trendsetter',      'Have 50 followers',                                 'achievement_trendsetter.png',       'EPIC',      'SOCIAL',   800,  300, 10,  true,  50),
-- SUPPORTER
('supporter_bronze','Bronze Supporter', 'Subscribe to NovelVerse Pro',                      'supporter_bronze_heart.png',        'RARE',      'SUPPORTER', 500,  200, 10,  false, 1),
('supporter_silver','Silver Supporter', 'Stay subscribed for 3 months',                     'supporter_silver_heart.png',        'EPIC',      'SUPPORTER',1000,  300, 20,  true,  3),
('supporter_gold',  'Gold Supporter',   'Stay subscribed for 12 months',                    'supporter_gold_heart.png',          'LEGENDARY', 'SUPPORTER',5000,  500, 100, true, 12)
ON CONFLICT (id) DO NOTHING;

-- ── Helper view: user gamification summary ────────────────────────────────────
CREATE OR REPLACE VIEW public.user_gamification_summary AS
SELECT
    u.id                                            AS user_id,
    COALESCE(uc.ink_balance,  0)                    AS ink_balance,
    COALESCE(uc.quill_balance, 0)                   AS quill_balance,
    COALESCE(ul.current_level, 1)                   AS current_level,
    COALESCE(ul.xp_total, 0)                        AS xp_total,
    COALESCE(us.current_streak, 0)                  AS current_streak,
    COALESCE(us.longest_streak, 0)                  AS longest_streak,
    COUNT(ua.id) FILTER (WHERE ua.is_unlocked)      AS achievements_unlocked
FROM auth.users u
LEFT JOIN public.user_currency    uc ON uc.user_id = u.id
LEFT JOIN public.user_levels      ul ON ul.user_id = u.id
LEFT JOIN public.user_streaks     us ON us.user_id = u.id
LEFT JOIN public.user_achievements ua ON ua.user_id = u.id
GROUP BY u.id, uc.ink_balance, uc.quill_balance,
         ul.current_level, ul.xp_total, us.current_streak, us.longest_streak;

-- ── Edge function stub comment ────────────────────────────────────────────────
-- Deploy supabase/functions/dedupe-views/index.ts for IP-based view deduplication.
-- The function should:
--   1. Accept { novel_id, viewer_ip } from the client
--   2. Check if viewer_ip has viewed novel_id in the past 24h (Redis/KV or DB table)
--   3. Only increment views count if no duplicate found
-- This prevents the "views" anti-exploit from inflating going_viral / bestseller counts.
