-- ═══════════════════════════════════════════════════════════════════════════
-- NovelVerse — Game Assets SQL
-- Supabase project: ztfvifgtebptxfoasrae
-- Run via Dashboard → SQL Editor (or psql).
-- Idempotent: safe to re-run.
-- ═══════════════════════════════════════════════════════════════════════════

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────
-- 1. STORAGE BUCKET (game-assets)
--    Public CDN reads · admin-only writes · 5 MB cap · PNG + JPG + WebP
-- ─────────────────────────────────────────────────────────────────────────
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'game-assets', 'game-assets', true, 5242880,
    ARRAY['image/png','image/jpeg','image/webp','image/svg+xml']
)
ON CONFLICT (id) DO UPDATE SET
    public             = EXCLUDED.public,
    file_size_limit    = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- ─────────────────────────────────────────────────────────────────────────
-- 2. STORAGE RLS POLICIES
-- ─────────────────────────────────────────────────────────────────────────
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "game_assets_public_read"  ON storage.objects;
CREATE POLICY "game_assets_public_read" ON storage.objects
    FOR SELECT USING (bucket_id = 'game-assets');

DROP POLICY IF EXISTS "game_assets_admin_insert" ON storage.objects;
CREATE POLICY "game_assets_admin_insert" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'game-assets'
        AND EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin')
    );

DROP POLICY IF EXISTS "game_assets_admin_update" ON storage.objects;
CREATE POLICY "game_assets_admin_update" ON storage.objects
    FOR UPDATE TO authenticated
    USING (
        bucket_id = 'game-assets'
        AND EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin')
    );

DROP POLICY IF EXISTS "game_assets_admin_delete" ON storage.objects;
CREATE POLICY "game_assets_admin_delete" ON storage.objects
    FOR DELETE TO authenticated
    USING (
        bucket_id = 'game-assets'
        AND EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin')
    );

-- ─────────────────────────────────────────────────────────────────────────
-- 3. ACHIEVEMENTS CATALOG TABLE
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.achievements (
    id           TEXT        PRIMARY KEY,
    title        TEXT        NOT NULL,
    description  TEXT,
    asset_name   TEXT,           -- filename inside achievements/ folder, e.g. "04_achievement_bookworm.png"
    badge_url    TEXT,           -- full public CDN URL (populated after upload)
    rarity       TEXT        NOT NULL DEFAULT 'COMMON'
                 CHECK (rarity   IN ('COMMON','RARE','EPIC','LEGENDARY')),
    category     TEXT        NOT NULL DEFAULT 'READER'
                 CHECK (category IN ('READER','WRITER','STREAK','SOCIAL','SUPPORTER')),
    xp_reward    INTEGER     NOT NULL DEFAULT 0 CHECK (xp_reward    >= 0),
    ink_reward   INTEGER     NOT NULL DEFAULT 0 CHECK (ink_reward   >= 0),
    quill_reward INTEGER     NOT NULL DEFAULT 0 CHECK (quill_reward >= 0),
    is_visible   BOOLEAN     NOT NULL DEFAULT TRUE,
    target_value INTEGER     NOT NULL DEFAULT 1  CHECK (target_value > 0),
    sort_order   SMALLINT    NOT NULL DEFAULT 0,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "achievements_public_read"  ON public.achievements;
CREATE POLICY "achievements_public_read" ON public.achievements
    FOR SELECT USING (is_active = TRUE);

DROP POLICY IF EXISTS "achievements_admin_write"  ON public.achievements;
CREATE POLICY "achievements_admin_write" ON public.achievements
    FOR ALL TO authenticated
    USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

-- ─────────────────────────────────────────────────────────────────────────
-- 4. USER ACHIEVEMENTS TABLE
-- ─────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_achievements (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    achievement_id   TEXT        NOT NULL REFERENCES public.achievements(id) ON DELETE CASCADE,
    is_unlocked      BOOLEAN     NOT NULL DEFAULT FALSE,
    unlocked_at      TIMESTAMPTZ,
    current_progress INTEGER     NOT NULL DEFAULT 0 CHECK (current_progress >= 0),
    reward_claimed   BOOLEAN     NOT NULL DEFAULT FALSE,
    notified         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

CREATE INDEX IF NOT EXISTS idx_ua_user_unlocked
    ON public.user_achievements (user_id, is_unlocked, unlocked_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_ua_notify
    ON public.user_achievements (user_id, notified)
    WHERE notified = FALSE AND unlocked_at IS NOT NULL;

ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "ua_select_own"   ON public.user_achievements;
CREATE POLICY "ua_select_own" ON public.user_achievements
    FOR SELECT TO authenticated USING (user_id = auth.uid());

DROP POLICY IF EXISTS "ua_insert_own"   ON public.user_achievements;
CREATE POLICY "ua_insert_own" ON public.user_achievements
    FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "ua_update_own"   ON public.user_achievements;
CREATE POLICY "ua_update_own" ON public.user_achievements
    FOR UPDATE TO authenticated
    USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "ua_admin_read"   ON public.user_achievements;
CREATE POLICY "ua_admin_read" ON public.user_achievements
    FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND role = 'admin'));

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$;

DROP TRIGGER IF EXISTS trg_ua_updated_at ON public.user_achievements;
CREATE TRIGGER trg_ua_updated_at
    BEFORE UPDATE ON public.user_achievements
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ─────────────────────────────────────────────────────────────────────────
-- 5. ACHIEVEMENT CATALOG SEED  (all 48 achievements)
--    asset_name  = exact filename inside achievements/ bucket folder
--    badge_url   = full public CDN URL built from asset_name
-- ─────────────────────────────────────────────────────────────────────────
DO $$
DECLARE base TEXT := 'https://ztfvifgtebptxfoasrae.supabase.co/storage/v1/object/public/game-assets/achievements/';
BEGIN

INSERT INTO public.achievements
    (id, title, description, asset_name, badge_url, rarity, category,
     xp_reward, ink_reward, quill_reward, is_visible, target_value, sort_order)
VALUES
-- ── READER ──────────────────────────────────────────────────────────────
('first_page',    'First Page',    'Read your first chapter',     '03_achievement_first_page.png',    base||'03_achievement_first_page.png',    'COMMON',    'READER',   50, 20, 0, FALSE,   1,  10),
('bookworm',      'Bookworm',      'Read 10 novels',              '04_achievement_bookworm.png',      base||'04_achievement_bookworm.png',      'RARE',      'READER',  200,100, 0, TRUE,   10,  20),
('scholar',       'Scholar',       'Read 25 novels',              '05_achievement_scholar.png',       base||'05_achievement_scholar.png',       'RARE',      'READER',  400,200, 0, TRUE,   25,  30),
('sage',          'Sage',          'Read 50 novels',              '06_achievement_sage.png',          base||'06_achievement_sage.png',          'EPIC',      'READER',  750,350, 5, TRUE,   50,  40),
('lorekeeper',    'Lorekeeper',    'Read 100 novels',             '07_achievement_lorekeeper.png',    base||'07_achievement_lorekeeper.png',    'LEGENDARY', 'READER', 2000,500,20, TRUE,  100,  50),
('completionist', 'Completionist', 'Finish a novel',              '08_achievement_completionist.png', base||'08_achievement_completionist.png', 'RARE',      'READER',  300,120, 0, TRUE,    1,  15),
('finished_strong','Finished Strong','Complete 5 novels',         '09_achievement_finished_strong.png',base||'09_achievement_finished_strong.png','EPIC',    'READER',  600,250, 5, TRUE,    5,  25),
('night_reader',  'Night Reader',  'Read midnight–4 AM',          '10_achievement_night_reader.png',  base||'10_achievement_night_reader.png',  'RARE',      'READER',  250,100, 0, FALSE,   1,  35),
('speed_reader',  'Speed Reader',  'Read 50k words in one day',   '11_achievement_speed_reader.png',  base||'11_achievement_speed_reader.png',  'EPIC',      'READER',  500,200, 2, TRUE,    1,  45),
('deep_dive',     'Deep Dive',     '4-hour reading session',      '12_achievement_deep_dive.png',     base||'12_achievement_deep_dive.png',     'RARE',      'READER',  300,150, 0, TRUE,    1,  55),
('genre_fantasy_master','Fantasy Sage','Read 20 Fantasy novels',  '90_genre_fantasy_master.png',      base||'90_genre_fantasy_master.png',      'EPIC',      'READER',  600,200, 5, TRUE,   20,  60),
('genre_romance_master','Romance Connoisseur','Read 20 Romance novels','91_genre_romance_master.png', base||'91_genre_romance_master.png',      'EPIC',      'READER',  600,200, 5, TRUE,   20,  61),
('genre_horror_master','Horror Survivor','Read 20 Horror novels', '92_genre_horror_master.png',       base||'92_genre_horror_master.png',       'EPIC',      'READER',  600,200, 5, TRUE,   20,  62),
('genre_scifi_master','Sci-Fi Explorer','Read 20 Sci-Fi novels',  '93_genre_scifi_master.png',        base||'93_genre_scifi_master.png',        'EPIC',      'READER',  600,200, 5, TRUE,   20,  63),
('genre_mystery_master','Master Detective','Read 20 Mystery novels','94_genre_mystery_master.png',    base||'94_genre_mystery_master.png',      'EPIC',      'READER',  600,200, 5, TRUE,   20,  64),
-- ── WRITER ──────────────────────────────────────────────────────────────
('first_words',   'First Words',   'Publish first chapter',       '13_achievement_first_words.png',   base||'13_achievement_first_words.png',   'COMMON',    'WRITER',  100, 40, 0, FALSE,   1,  10),
('published',     'Published',     'Publish with 1k words',       '14_achievement_published.png',     base||'14_achievement_published.png',     'RARE',      'WRITER',  300,120, 2, TRUE,    1,  20),
('rising_star',   'Rising Star',   '100 total reads',             '15_achievement_rising_star.png',   base||'15_achievement_rising_star.png',   'COMMON',    'WRITER',  150, 60, 0, TRUE,  100,  30),
('going_viral',   'Going Viral',   '1,000 total reads',           '16_achievement_going_viral.png',   base||'16_achievement_going_viral.png',   'RARE',      'WRITER',  400,150, 2, TRUE, 1000,  40),
('bestseller',    'Bestseller',    '10,000 total reads',          '17_achievement_bestseller.png',    base||'17_achievement_bestseller.png',    'LEGENDARY', 'WRITER', 2500,500,25, TRUE,10000,  50),
('prolific',      'Prolific',      'Publish 10 chapters',         '18_achievement_prolific.png',      base||'18_achievement_prolific.png',      'RARE',      'WRITER',  350,140, 0, TRUE,   10,  25),
('chapter_50',    'Serial Author', '50 chapters published',       '79_achievement_chapter_50.png',    base||'79_achievement_chapter_50.png',    'EPIC',      'WRITER',  700,280, 8, TRUE,   50,  35),
('novels_3',      'Trilogy Master','3 novels completed',          '80_achievement_novels_3.png',      base||'80_achievement_novels_3.png',      'EPIC',      'WRITER',  600,240, 6, TRUE,    3,  45),
('words_100k',    'Century Scribe','100k words written',          '81_achievement_words_100k.png',    base||'81_achievement_words_100k.png',    'EPIC',      'WRITER',  700,280, 8, TRUE, 1000,  55),
('words_1m',      'Millionaire Author','1M words written',        '82_achievement_words_1m.png',      base||'82_achievement_words_1m.png',      'LEGENDARY', 'WRITER', 3000,600,30, TRUE,10000,  65),
('followers_100', 'Rising Influencer','100 followers',            '83_achievement_followers_100.png', base||'83_achievement_followers_100.png', 'RARE',      'WRITER',  300,120, 2, TRUE,  100,  75),
('followers_1k',  'Influencer',    '1,000 followers',             '84_achievement_followers_1k.png',  base||'84_achievement_followers_1k.png',  'EPIC',      'WRITER',  700,280, 8, TRUE, 1000,  85),
('tipped',        'Appreciated',   'Received first tip',          '85_achievement_tipped.png',        base||'85_achievement_tipped.png',        'RARE',      'WRITER',  400,160, 3, FALSE,  1,  95),
('earnings_100',  'First Paycheck','Earned $100',                 '86_achievement_earnings_100.png',  base||'86_achievement_earnings_100.png',  'EPIC',      'WRITER',  800,320,10, TRUE,  100, 105),
-- ── STREAK ──────────────────────────────────────────────────────────────
('streak_3',      'On a Roll',     '3-day streak',                '19_achievement_streak_3.png',      base||'19_achievement_streak_3.png',      'COMMON',    'STREAK',  100, 40, 0, TRUE,    3,  10),
('streak_7',      'Week Warrior',  '7-day streak',                '20_achievement_streak_7.png',      base||'20_achievement_streak_7.png',      'COMMON',    'STREAK',  200, 80, 0, TRUE,    7,  20),
('streak_14',     'Fortnight',     '14-day streak',               '75_achievement_streak_14.png',     base||'75_achievement_streak_14.png',     'RARE',      'STREAK',  400,160, 2, TRUE,   14,  30),
('streak_30',     'Monthly Devotee','30-day streak',              '21_achievement_streak_30.png',     base||'21_achievement_streak_30.png',     'RARE',      'STREAK',  700,280, 5, TRUE,   30,  40),
('streak_60',     'Bimonthly',     '60-day streak',               '76_achievement_streak_60.png',     base||'76_achievement_streak_60.png',     'EPIC',      'STREAK', 1200,480,10, TRUE,   60,  50),
('streak_100',    'Century Reader','100-day streak',              '22_achievement_streak_100.png',    base||'22_achievement_streak_100.png',    'EPIC',      'STREAK', 1500,600,15, TRUE,  100,  60),
('streak_180',    'Half-Year Hero','180-day streak',              '77_achievement_streak_180.png',    base||'77_achievement_streak_180.png',    'LEGENDARY', 'STREAK', 3000,800,30, TRUE,  180,  70),
('streak_365',    'Legendary Dedication','1-year streak',         '78_achievement_streak_365.png',    base||'78_achievement_streak_365.png',    'LEGENDARY', 'STREAK', 5000,999,50, TRUE,  365,  80),
-- ── SOCIAL ──────────────────────────────────────────────────────────────
('bookmarked',    'Bookmarked',    'First bookmark',              '23_achievement_bookmarked.png',    base||'23_achievement_bookmarked.png',    'COMMON',    'SOCIAL',   50, 20, 0, FALSE,  1,  10),
('critic',        'Critic',        '10 reviews',                  '24_achievement_critic.png',        base||'24_achievement_critic.png',        'RARE',      'SOCIAL',  250,100, 0, TRUE,   10,  20),
('trusted_voice', 'Trusted Voice', '25 review likes',             '25_achievement_trusted_voice.png', base||'25_achievement_trusted_voice.png', 'EPIC',      'SOCIAL',  600,250, 5, TRUE,   25,  30),
('connected',     'Connected',     'Follow 10 authors',           '26_achievement_connected.png',     base||'26_achievement_connected.png',     'COMMON',    'SOCIAL',  100, 40, 0, TRUE,   10,  40),
('trendsetter',   'Trendsetter',   '50 followers',                '27_achievement_trendsetter.png',   base||'27_achievement_trendsetter.png',   'EPIC',      'SOCIAL',  800,300,10, TRUE,   50,  50),
('comments_50',   'Conversation Starter','50 comments',           '87_achievement_comments_50.png',   base||'87_achievement_comments_50.png',   'RARE',      'SOCIAL',  300,120, 0, TRUE,   50,  55),
('replies_100',   'Engaged Reader','100 replies',                  '88_achievement_replies_100.png',   base||'88_achievement_replies_100.png',   'RARE',      'SOCIAL',  350,140, 0, TRUE,  100,  60),
('liked_500',     'Community Favorite','500 review likes',        '89_achievement_liked_500.png',     base||'89_achievement_liked_500.png',     'LEGENDARY', 'SOCIAL', 1500,600,20, TRUE,  500,  65),
-- ── SUPPORTER ───────────────────────────────────────────────────────────
('supporter_bronze','Bronze Supporter','Subscribe to NovelVerse Pro','28_supporter_bronze.png',      base||'28_supporter_bronze.png',          'RARE',      'SUPPORTER', 500,200,10, FALSE, 1, 10),
('supporter_silver','Silver Supporter','3 months subscribed',     '29_supporter_silver.png',          base||'29_supporter_silver.png',          'EPIC',      'SUPPORTER',1000,300,20, TRUE,  3,  20),
('supporter_gold', 'Gold Supporter','12 months subscribed',       '30_supporter_gold.png',            base||'30_supporter_gold.png',            'LEGENDARY', 'SUPPORTER',5000,500,100,TRUE, 12,  30)

ON CONFLICT (id) DO UPDATE SET
    title        = EXCLUDED.title,
    description  = EXCLUDED.description,
    asset_name   = EXCLUDED.asset_name,
    badge_url    = EXCLUDED.badge_url,
    rarity       = EXCLUDED.rarity,
    category     = EXCLUDED.category,
    xp_reward    = EXCLUDED.xp_reward,
    ink_reward   = EXCLUDED.ink_reward,
    quill_reward = EXCLUDED.quill_reward,
    is_visible   = EXCLUDED.is_visible,
    target_value = EXCLUDED.target_value,
    sort_order   = EXCLUDED.sort_order;

END; $$;

-- ─────────────────────────────────────────────────────────────────────────
-- 6. MINIMUM GRANTS
-- ─────────────────────────────────────────────────────────────────────────
GRANT SELECT ON public.achievements     TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE
      ON public.user_achievements       TO authenticated;
GRANT USAGE ON SCHEMA public           TO anon, authenticated;

COMMIT;

-- ─────────────────────────────────────────────────────────────────────────
-- POST-DEPLOY CHECKLIST
-- ─────────────────────────────────────────────────────────────────────────
-- 1. Bucket is already public — verify: SELECT public FROM storage.buckets WHERE id='game-assets';
-- 2. Test a CDN URL in browser (no auth header needed):
--    https://ztfvifgtebptxfoasrae.supabase.co/storage/v1/object/public/game-assets/achievements/04_achievement_bookworm.png
-- 3. GameAssets.getAchievementUrl("bookworm") must return that exact URL.
-- 4. Confirm RLS: anon SELECT from public.achievements works; non-admin INSERT to storage fails.
-- ─────────────────────────────────────────────────────────────────────────
