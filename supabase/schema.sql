-- NovelVerse Supabase Database Schema
-- Complete schema with RLS policies, indexes, and triggers

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- For full-text search

-- ============================================
-- CORE TABLES
-- ============================================

-- Profiles table (extends auth.users)
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    email VARCHAR(255) UNIQUE NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    role VARCHAR(20) DEFAULT 'reader' CHECK (role IN ('guest', 'reader', 'author', 'admin', 'moderator')),
    points_balance INTEGER DEFAULT 0,
    total_spent DECIMAL(10,2) DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_phone_verified BOOLEAN DEFAULT FALSE,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    country_code VARCHAR(5),
    language_preference VARCHAR(10) DEFAULT 'en',
    theme_preference VARCHAR(10) DEFAULT 'light',
    font_size INTEGER DEFAULT 16,
    line_spacing FLOAT DEFAULT 1.5,
    auto_scroll_speed INTEGER DEFAULT 50,
    tts_speed FLOAT DEFAULT 1.0,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    email_notifications BOOLEAN DEFAULT TRUE,
    push_notifications BOOLEAN DEFAULT TRUE,
    marketing_emails BOOLEAN DEFAULT FALSE,
    privacy_setting VARCHAR(20) DEFAULT 'public' CHECK (privacy_setting IN ('public', 'friends', 'private')),
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    referral_code VARCHAR(20) UNIQUE,
    referred_by UUID REFERENCES profiles(id),
    subscription_tier VARCHAR(20) DEFAULT 'free' CHECK (subscription_tier IN ('free', 'premium', 'vip')),
    subscription_expires_at TIMESTAMP WITH TIME ZONE,
    stripe_customer_id VARCHAR(255),
    payout_method VARCHAR(50),
    payout_details JSONB,
    total_earnings DECIMAL(10,2) DEFAULT 0,
    available_for_payout DECIMAL(10,2) DEFAULT 0,
    is_banned BOOLEAN DEFAULT FALSE,
    ban_reason TEXT,
    ban_expires_at TIMESTAMP WITH TIME ZONE,
    followers_count INTEGER NOT NULL DEFAULT 0,
    following_count INTEGER NOT NULL DEFAULT 0
);

-- Novels table
CREATE TABLE novels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE,
    description TEXT NOT NULL,
    cover_image_url TEXT,
    banner_image_url TEXT,
    status VARCHAR(20) DEFAULT 'ongoing' CHECK (status IN ('ongoing', 'completed', 'hiatus', 'dropped', 'draft')),
    visibility VARCHAR(20) DEFAULT 'public' CHECK (visibility IN ('public', 'private', 'unlisted')),
    price_type VARCHAR(20) DEFAULT 'free' CHECK (price_type IN ('free', 'paid', 'freemium')),
    price DECIMAL(10,2),
    points_cost INTEGER,
    age_rating VARCHAR(20) DEFAULT 'all' CHECK (age_rating IN ('all', 'teen', 'mature', 'adult')),
    content_warnings TEXT[],
    license_type VARCHAR(50) DEFAULT 'all_rights_reserved',
    language VARCHAR(10) DEFAULT 'en',
    total_chapters INTEGER DEFAULT 0,
    total_words BIGINT DEFAULT 0,
    total_views BIGINT DEFAULT 0,
    total_likes INTEGER DEFAULT 0,
    total_bookmarks INTEGER DEFAULT 0,
    total_comments INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,
    rating_count INTEGER DEFAULT 0,
    is_published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    scheduled_publish_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    is_featured BOOLEAN DEFAULT FALSE,
    featured_at TIMESTAMP WITH TIME ZONE,
    featured_until TIMESTAMP WITH TIME ZONE,
    seo_title VARCHAR(255),
    seo_description TEXT,
    tags TEXT[],
    genres TEXT[],
    metadata JSONB DEFAULT '{}',
    moderation_status VARCHAR(20) DEFAULT 'pending' CHECK (moderation_status IN ('pending', 'approved', 'rejected', 'flagged')),
    moderation_notes TEXT,
    moderated_by UUID REFERENCES profiles(id),
    moderated_at TIMESTAMP WITH TIME ZONE,
    search_vector TSVECTOR
);

-- Chapters table
CREATE TABLE chapters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    content TEXT NOT NULL,
    word_count INTEGER DEFAULT 0,
    is_published BOOLEAN DEFAULT FALSE,
    is_free BOOLEAN DEFAULT TRUE,
    price DECIMAL(10,2),
    points_cost INTEGER,
    scheduled_publish_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    total_views BIGINT DEFAULT 0,
    total_comments INTEGER DEFAULT 0,
    average_read_time INTEGER, -- in seconds
    UNIQUE(novel_id, chapter_number)
);

-- Chapter versions (for history/rollback)
CREATE TABLE chapter_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    word_count INTEGER DEFAULT 0,
    created_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    change_summary TEXT
);

-- Characters table
CREATE TABLE characters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    description TEXT,
    role VARCHAR(50), -- protagonist, antagonist, supporting, etc.
    appearance_description TEXT,
    personality_traits TEXT[],
    background TEXT,
    relationships JSONB DEFAULT '[]',
    is_spoiler BOOLEAN DEFAULT FALSE,
    spoiler_chapter INTEGER, -- reveal after this chapter
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Categories/Genres table
CREATE TABLE genres (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT,
    color VARCHAR(7),
    novel_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Novel-Genre junction
CREATE TABLE novel_genres (
    novel_id UUID REFERENCES novels(id) ON DELETE CASCADE,
    genre_id UUID REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (novel_id, genre_id)
);

-- Tags table
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    novel_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Novel-Tag junction
CREATE TABLE novel_tags (
    novel_id UUID REFERENCES novels(id) ON DELETE CASCADE,
    tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (novel_id, tag_id)
);

-- ============================================
-- USER INTERACTION TABLES
-- ============================================

-- Reading progress
CREATE TABLE reading_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    scroll_position INTEGER DEFAULT 0,
    progress_percentage DECIMAL(5,2) DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    total_reading_time INTEGER DEFAULT 0, -- in seconds
    device_info VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, novel_id)
);

-- Bookmarks
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_id UUID REFERENCES chapters(id) ON DELETE CASCADE,
    position INTEGER DEFAULT 0,
    note TEXT,
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, novel_id, chapter_id, position)
);

-- User library (saved novels)
CREATE TABLE user_library (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'reading' CHECK (status IN ('reading', 'completed', 'on_hold', 'dropped', 'plan_to_read')),
    is_purchased BOOLEAN DEFAULT FALSE,
    purchased_at TIMESTAMP WITH TIME ZONE,
    purchase_price DECIMAL(10,2),
    auto_unlock_chapters BOOLEAN DEFAULT FALSE,
    notify_new_chapters BOOLEAN DEFAULT TRUE,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, novel_id)
);

-- Notes/Highlights
CREATE TABLE user_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    start_position INTEGER NOT NULL,
    end_position INTEGER NOT NULL,
    selected_text TEXT NOT NULL,
    note TEXT,
    color VARCHAR(7) DEFAULT '#FFFF00',
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Ratings
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    is_spoiler BOOLEAN DEFAULT FALSE,
    likes_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, novel_id)
);

-- Likes on reviews
CREATE TABLE review_likes (
    review_id UUID REFERENCES ratings(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (review_id, user_id)
);

-- Comments
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID REFERENCES novels(id) ON DELETE CASCADE,
    chapter_id UUID REFERENCES chapters(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    likes_count INTEGER DEFAULT 0,
    dislikes_count INTEGER DEFAULT 0,
    replies_count INTEGER DEFAULT 0,
    is_edited BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_spoiler BOOLEAN DEFAULT FALSE,
    moderation_status VARCHAR(20) DEFAULT 'approved' CHECK (moderation_status IN ('pending', 'approved', 'rejected', 'flagged')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Comment reactions
CREATE TABLE comment_reactions (
    comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    reaction_type VARCHAR(10) NOT NULL CHECK (reaction_type IN ('like', 'dislike')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (comment_id, user_id)
);

-- Follow authors
CREATE TABLE author_follows (
    follower_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    author_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    notify_new_novels BOOLEAN DEFAULT TRUE,
    notify_new_chapters BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (follower_id, author_id)
);

-- Reading lists/collections
CREATE TABLE reading_lists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    cover_image_url TEXT,
    novel_count INTEGER DEFAULT 0,
    followers_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Reading list items
CREATE TABLE reading_list_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    list_id UUID NOT NULL REFERENCES reading_lists(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    note TEXT,
    display_order INTEGER DEFAULT 0,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(list_id, novel_id)
);

-- ============================================
-- MONETIZATION TABLES
-- ============================================

-- Point transactions
CREATE TABLE point_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('purchase', 'spend', 'earn', 'refund', 'bonus', 'tip_given', 'tip_received', 'referral', 'promo_code')),
    description TEXT NOT NULL,
    reference_type VARCHAR(50), -- novel, chapter, user, etc.
    reference_id UUID,
    balance_after INTEGER NOT NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Purchases (novels/chapters)
CREATE TABLE purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_id UUID REFERENCES chapters(id) ON DELETE CASCADE,
    purchase_type VARCHAR(20) NOT NULL CHECK (purchase_type IN ('novel', 'chapter')),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('points', 'google_play', 'apple_pay', 'stripe')),
    amount DECIMAL(10,2) NOT NULL,
    points_used INTEGER DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    google_play_order_id VARCHAR(255),
    google_play_purchase_token TEXT,
    google_play_product_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'completed', 'failed', 'refunded', 'cancelled')),
    purchased_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_consumable BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'
);

-- Point packages (for Google Play)
CREATE TABLE point_packages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    google_play_product_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    points_amount INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    bonus_points INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    icon_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Tips/Donations
CREATE TABLE tips (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    novel_id UUID REFERENCES novels(id) ON DELETE SET NULL,
    chapter_id UUID REFERENCES chapters(id) ON DELETE SET NULL,
    amount DECIMAL(10,2) NOT NULL,
    points_amount INTEGER,
    payment_method VARCHAR(20) NOT NULL,
    message TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    google_play_order_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'completed', 'failed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Subscriptions
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('premium', 'vip')),
    google_play_product_id VARCHAR(255),
    google_play_purchase_token TEXT,
    google_play_order_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    interval VARCHAR(20) NOT NULL CHECK (interval IN ('monthly', 'yearly')),
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'cancelled', 'expired', 'paused')),
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Promo codes
CREATE TABLE promo_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    reward_type VARCHAR(20) NOT NULL CHECK (reward_type IN ('points', 'novel_unlock', 'subscription_discount')),
    reward_amount INTEGER,
    discount_percentage INTEGER,
    max_uses INTEGER,
    used_count INTEGER DEFAULT 0,
    valid_from TIMESTAMP WITH TIME ZONE,
    valid_until TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Promo code redemptions
CREATE TABLE promo_code_redemptions (
    promo_code_id UUID REFERENCES promo_codes(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    redeemed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (promo_code_id, user_id)
);

-- ============================================
-- SOCIAL & COMMUNITY TABLES
-- ============================================

-- Reading groups/book clubs
CREATE TABLE reading_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    cover_image_url TEXT,
    creator_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    is_public BOOLEAN DEFAULT TRUE,
    max_members INTEGER DEFAULT 100,
    member_count INTEGER DEFAULT 1,
    current_novel_id UUID REFERENCES novels(id),
    current_chapter_id UUID REFERENCES chapters(id),
    discussion_schedule TEXT,
    rules TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Group members
CREATE TABLE reading_group_members (
    group_id UUID REFERENCES reading_groups(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'member' CHECK (role IN ('member', 'moderator', 'admin')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_read_at TIMESTAMP WITH TIME ZONE,
    is_muted BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (group_id, user_id)
);

-- Group messages
CREATE TABLE group_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES reading_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    reply_to_id UUID REFERENCES group_messages(id),
    is_edited BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Direct messages
CREATE TABLE direct_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    is_deleted_by_sender BOOLEAN DEFAULT FALSE,
    is_deleted_by_recipient BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Achievements/Badges
CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon_url TEXT,
    badge_color VARCHAR(7),
    requirement_type VARCHAR(50) NOT NULL,
    requirement_value INTEGER NOT NULL,
    points_reward INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- User achievements
CREATE TABLE user_achievements (
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    achievement_id UUID REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    progress INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, achievement_id)
);

-- Reading streaks
CREATE TABLE reading_streaks (
    user_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    last_read_date DATE,
    total_reading_days INTEGER DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- MODERATION TABLES
-- ============================================

-- Reports
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    reported_user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL CHECK (report_type IN ('novel', 'chapter', 'comment', 'user', 'message')),
    target_id UUID NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'investigating', 'resolved', 'dismissed')),
    resolution TEXT,
    resolved_by UUID REFERENCES profiles(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Banned words
CREATE TABLE banned_words (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    word VARCHAR(100) NOT NULL,
    category VARCHAR(50) DEFAULT 'general',
    severity VARCHAR(20) DEFAULT 'medium' CHECK (severity IN ('low', 'medium', 'high')),
    action VARCHAR(20) DEFAULT 'flag' CHECK (action IN ('flag', 'block', 'auto_delete')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Content moderation queue
CREATE TABLE moderation_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('novel', 'chapter', 'comment')),
    content_id UUID NOT NULL,
    author_id UUID REFERENCES profiles(id),
    ai_score DECIMAL(5,2),
    ai_flags JSONB,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    reviewed_by UUID REFERENCES profiles(id),
    review_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    reviewed_at TIMESTAMP WITH TIME ZONE
);

-- ============================================
-- NOTIFICATIONS
-- ============================================

-- Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',
    image_url TEXT,
    action_url TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    sent_via VARCHAR(20) DEFAULT 'push' CHECK (sent_via IN ('push', 'email', 'in_app')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Notification preferences
CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    new_chapters BOOLEAN DEFAULT TRUE,
    new_followers BOOLEAN DEFAULT TRUE,
    comments_on_my_content BOOLEAN DEFAULT TRUE,
    replies_to_my_comments BOOLEAN DEFAULT TRUE,
    tips_received BOOLEAN DEFAULT TRUE,
    promotional BOOLEAN DEFAULT FALSE,
    reading_reminders BOOLEAN DEFAULT TRUE,
    achievement_unlocked BOOLEAN DEFAULT TRUE,
    group_activity BOOLEAN DEFAULT TRUE,
    email_digest_frequency VARCHAR(20) DEFAULT 'daily' CHECK (email_digest_frequency IN ('never', 'daily', 'weekly')),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- ANALYTICS TABLES
-- ============================================

-- Novel analytics (daily aggregated)
CREATE TABLE novel_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    novel_id UUID NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    views INTEGER DEFAULT 0,
    unique_readers INTEGER DEFAULT 0,
    new_bookmarks INTEGER DEFAULT 0,
    new_ratings INTEGER DEFAULT 0,
    comments_added INTEGER DEFAULT 0,
    chapters_read INTEGER DEFAULT 0,
    purchases INTEGER DEFAULT 0,
    revenue DECIMAL(10,2) DEFAULT 0,
    drop_off_chapter INTEGER,
    avg_session_duration INTEGER, -- seconds
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(novel_id, date)
);

-- Chapter analytics
CREATE TABLE chapter_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    views INTEGER DEFAULT 0,
    unique_readers INTEGER DEFAULT 0,
    avg_read_time INTEGER, -- seconds
    completion_rate DECIMAL(5,2),
    drop_off_rate DECIMAL(5,2),
    comments_added INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(chapter_id, date)
);

-- Page views (for real-time)
CREATE TABLE page_views (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    novel_id UUID REFERENCES novels(id),
    chapter_id UUID REFERENCES chapters(id),
    user_id UUID REFERENCES profiles(id),
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    referrer TEXT,
    duration INTEGER, -- seconds
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- SYSTEM TABLES
-- ============================================

-- App configuration
CREATE TABLE app_config (
    key VARCHAR(100) PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by UUID REFERENCES profiles(id)
);

-- Audit log
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id),
    action VARCHAR(100) NOT NULL,
    table_name VARCHAR(100),
    record_id UUID,
    old_data JSONB,
    new_data JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- INDEXES
-- ============================================

-- Profiles indexes
CREATE INDEX idx_profiles_username ON profiles(username);
CREATE INDEX idx_profiles_role ON profiles(role);
CREATE INDEX idx_profiles_referral_code ON profiles(referral_code);

-- Novels indexes
CREATE INDEX idx_novels_author ON novels(author_id);
CREATE INDEX idx_novels_status ON novels(status);
CREATE INDEX idx_novels_visibility ON novels(visibility);
CREATE INDEX idx_novels_price_type ON novels(price_type);
CREATE INDEX idx_novels_age_rating ON novels(age_rating);
CREATE INDEX idx_novels_published_at ON novels(published_at DESC);
CREATE INDEX idx_novels_total_views ON novels(total_views DESC);
CREATE INDEX idx_novels_average_rating ON novels(average_rating DESC);
CREATE INDEX idx_novels_featured ON novels(is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_novels_search ON novels USING GIN(search_vector);
CREATE INDEX idx_novels_genres ON novels USING GIN(genres);
CREATE INDEX idx_novels_tags ON novels USING GIN(tags);

-- Chapters indexes
CREATE INDEX idx_chapters_novel ON chapters(novel_id);
CREATE INDEX idx_chapters_number ON chapters(novel_id, chapter_number);
CREATE INDEX idx_chapters_published ON chapters(is_published, published_at DESC);

-- Comments indexes
CREATE INDEX idx_comments_novel ON comments(novel_id);
CREATE INDEX idx_comments_chapter ON comments(chapter_id);
CREATE INDEX idx_comments_parent ON comments(parent_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_created ON comments(created_at DESC);

-- Reading progress indexes
CREATE INDEX idx_reading_progress_user ON reading_progress(user_id);
CREATE INDEX idx_reading_progress_novel ON reading_progress(novel_id);
CREATE INDEX idx_reading_progress_last_read ON reading_progress(last_read_at DESC);

-- Library indexes
CREATE INDEX idx_library_user ON user_library(user_id);
CREATE INDEX idx_library_novel ON user_library(novel_id);
CREATE INDEX idx_library_status ON user_library(status);

-- Search indexes
CREATE INDEX idx_novels_title_trgm ON novels USING GIN(title gin_trgm_ops);
CREATE INDEX idx_novels_description_trgm ON novels USING GIN(description gin_trgm_ops);

-- Point transactions
CREATE INDEX idx_point_transactions_user ON point_transactions(user_id);
CREATE INDEX idx_point_transactions_created ON point_transactions(created_at DESC);

-- Notifications
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;

-- ============================================
-- ROW LEVEL SECURITY POLICIES
-- ============================================

-- Enable RLS on all tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE novels ENABLE ROW LEVEL SECURITY;
ALTER TABLE chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE chapter_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE characters ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE bookmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_library ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE review_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE comment_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE author_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_list_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE point_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE purchases ENABLE ROW LEVEL SECURITY;
ALTER TABLE tips ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE promo_code_redemptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE direct_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE reading_streaks ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE moderation_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Profiles are viewable by everyone" 
    ON profiles FOR SELECT USING (true);

CREATE POLICY "Users can insert their own profile" 
    ON profiles FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile" 
    ON profiles FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Admins can update any profile" 
    ON profiles FOR UPDATE USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'moderator'))
    );

-- Novels policies
CREATE POLICY "Published novels are viewable by everyone" 
    ON novels FOR SELECT USING (is_published = TRUE AND deleted_at IS NULL);

CREATE POLICY "Authors can view their unpublished novels" 
    ON novels FOR SELECT USING (author_id = auth.uid());

CREATE POLICY "Admins can view all novels" 
    ON novels FOR SELECT USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'moderator'))
    );

CREATE POLICY "Authors can create novels" 
    ON novels FOR INSERT WITH CHECK (
        author_id = auth.uid() AND 
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('author', 'admin'))
    );

CREATE POLICY "Authors can update their novels" 
    ON novels FOR UPDATE USING (author_id = auth.uid());

CREATE POLICY "Admins can update any novel" 
    ON novels FOR UPDATE USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND role IN ('admin', 'moderator'))
    );

CREATE POLICY "Authors can delete their novels" 
    ON novels FOR DELETE USING (author_id = auth.uid());

-- Chapters policies
CREATE POLICY "Published chapters are viewable by everyone" 
    ON chapters FOR SELECT USING (is_published = TRUE AND deleted_at IS NULL);

CREATE POLICY "Authors can view their unpublished chapters" 
    ON chapters FOR SELECT USING (
        EXISTS (SELECT 1 FROM novels WHERE id = novel_id AND author_id = auth.uid())
    );

CREATE POLICY "Authors can create chapters for their novels" 
    ON chapters FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM novels WHERE id = novel_id AND author_id = auth.uid())
    );

CREATE POLICY "Authors can update their chapters" 
    ON chapters FOR UPDATE USING (
        EXISTS (SELECT 1 FROM novels WHERE id = novel_id AND author_id = auth.uid())
    );

-- Comments policies
CREATE POLICY "Comments are viewable by everyone" 
    ON comments FOR SELECT USING (is_deleted = FALSE);

CREATE POLICY "Authenticated users can create comments" 
    ON comments FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own comments" 
    ON comments FOR UPDATE USING (user_id = auth.uid());

CREATE POLICY "Users can delete their own comments" 
    ON comments FOR DELETE USING (user_id = auth.uid());

-- Reading progress policies
CREATE POLICY "Users can view their own reading progress" 
    ON reading_progress FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Users can manage their reading progress" 
    ON reading_progress FOR ALL USING (user_id = auth.uid());

-- Library policies
CREATE POLICY "Users can view their own library" 
    ON user_library FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Users can manage their library" 
    ON user_library FOR ALL USING (user_id = auth.uid());

-- Point transactions policies
CREATE POLICY "Users can view their own transactions" 
    ON point_transactions FOR SELECT USING (user_id = auth.uid());

-- Purchases policies
CREATE POLICY "Users can view their own purchases" 
    ON purchases FOR SELECT USING (user_id = auth.uid());

-- Notifications policies
CREATE POLICY "Users can view their own notifications" 
    ON notifications FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Users can update their own notifications" 
    ON notifications FOR UPDATE USING (user_id = auth.uid());

-- ============================================
-- FUNCTIONS & TRIGGERS
-- ============================================

-- Update timestamps function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create update triggers for all tables with updated_at
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_novels_updated_at BEFORE UPDATE ON novels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chapters_updated_at BEFORE UPDATE ON chapters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_characters_updated_at BEFORE UPDATE ON characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_comments_updated_at BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reading_lists_updated_at BEFORE UPDATE ON reading_lists
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscriptions_updated_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Update novel chapter count
CREATE OR REPLACE FUNCTION update_novel_chapter_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE novels 
    SET total_chapters = (
        SELECT COUNT(*) FROM chapters 
        WHERE novel_id = COALESCE(NEW.novel_id, OLD.novel_id) 
        AND is_published = TRUE 
        AND deleted_at IS NULL
    ),
    last_updated_at = NOW()
    WHERE id = COALESCE(NEW.novel_id, OLD.novel_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_chapter_count 
    AFTER INSERT OR UPDATE OR DELETE ON chapters
    FOR EACH ROW EXECUTE FUNCTION update_novel_chapter_count();

-- Update novel average rating
CREATE OR REPLACE FUNCTION update_novel_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE novels 
    SET average_rating = (
        SELECT AVG(rating)::DECIMAL(3,2) FROM ratings 
        WHERE novel_id = COALESCE(NEW.novel_id, OLD.novel_id)
    ),
    rating_count = (
        SELECT COUNT(*) FROM ratings 
        WHERE novel_id = COALESCE(NEW.novel_id, OLD.novel_id)
    )
    WHERE id = COALESCE(NEW.novel_id, OLD.novel_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_novel_rating 
    AFTER INSERT OR UPDATE OR DELETE ON ratings
    FOR EACH ROW EXECUTE FUNCTION update_novel_rating();

-- Update comment counts
CREATE OR REPLACE FUNCTION update_comment_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.novel_id IS NOT NULL THEN
        UPDATE novels SET total_comments = (
            SELECT COUNT(*) FROM comments 
            WHERE novel_id = NEW.novel_id AND is_deleted = FALSE
        ) WHERE id = NEW.novel_id;
    END IF;

    IF NEW.chapter_id IS NOT NULL THEN
        UPDATE chapters SET total_comments = (
            SELECT COUNT(*) FROM comments 
            WHERE chapter_id = NEW.chapter_id AND is_deleted = FALSE
        ) WHERE id = NEW.chapter_id;
    END IF;

    IF NEW.parent_id IS NOT NULL THEN
        UPDATE comments SET replies_count = (
            SELECT COUNT(*) FROM comments 
            WHERE parent_id = NEW.parent_id AND is_deleted = FALSE
        ) WHERE id = NEW.parent_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_comment_counts 
    AFTER INSERT OR UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_comment_counts();

-- Update search vector
CREATE OR REPLACE FUNCTION update_novel_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.tags, ' '), '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.genres, ' '), '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_search_vector 
    BEFORE INSERT OR UPDATE ON novels
    FOR EACH ROW EXECUTE FUNCTION update_novel_search_vector();

-- Handle new user signup
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO profiles (id, username, email, role, referral_code)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1) || '_' || substr(md5(random()::text), 1, 6)),
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'role', 'reader'),
        upper(substr(md5(random()::text), 1, 8))
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- Update follower/following counts on profiles when author_follows changes
CREATE OR REPLACE FUNCTION update_follow_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE profiles SET followers_count = followers_count + 1 WHERE id = NEW.author_id;
        UPDATE profiles SET following_count = following_count + 1 WHERE id = NEW.follower_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE profiles SET followers_count = GREATEST(followers_count - 1, 0) WHERE id = OLD.author_id;
        UPDATE profiles SET following_count = GREATEST(following_count - 1, 0) WHERE id = OLD.follower_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER update_follow_counts
    AFTER INSERT OR DELETE ON author_follows
    FOR EACH ROW EXECUTE FUNCTION update_follow_counts();

-- ============================================
-- SEED DATA
-- ============================================

-- Genres
INSERT INTO genres (name, slug, description, color, display_order) VALUES
('Romance', 'romance', 'Love stories and romantic relationships', '#FF6B9D', 1),
('Fantasy', 'fantasy', 'Magical worlds and supernatural elements', '#9B59B6', 2),
('Science Fiction', 'sci-fi', 'Futuristic technology and space exploration', '#3498DB', 3),
('Mystery', 'mystery', 'Puzzles, crimes, and investigations', '#E74C3C', 4),
('Thriller', 'thriller', 'Suspenseful and exciting narratives', '#F39C12', 5),
('Horror', 'horror', 'Scary stories and supernatural horror', '#2C3E50', 6),
('Adventure', 'adventure', 'Exciting journeys and quests', '#27AE60', 7),
('Historical', 'historical', 'Stories set in the past', '#D4A574', 8),
('Comedy', 'comedy', 'Humorous and funny stories', '#F1C40F', 9),
('Drama', 'drama', 'Emotional and character-driven stories', '#E67E22', 10),
('Action', 'action', 'Fast-paced and action-packed', '#C0392B', 11),
('Young Adult', 'young-adult', 'Stories for teenage readers', '#1ABC9C', 12);

-- Point packages
INSERT INTO point_packages (google_play_product_id, name, description, points_amount, price, bonus_points, display_order) VALUES
('points_100', '100 Points', 'Basic point package', 100, 0.99, 0, 1),
('points_500', '500 Points', 'Popular choice', 500, 4.49, 25, 2),
('points_1000', '1000 Points', 'Best value', 1000, 7.99, 100, 3),
('points_2500', '2500 Points', 'Power reader pack', 2500, 17.99, 300, 4),
('points_5000', '5000 Points', 'Ultimate package', 5000, 29.99, 750, 5);

-- App config
INSERT INTO app_config (key, value, description) VALUES
('guest_free_chapters', '3', 'Number of free chapters for guest users'),
('points_per_dollar', '101', 'Points given per dollar spent (includes bonus)'),
('max_upload_size_mb', '2', 'Maximum upload size in MB'),
('comment_cooldown_seconds', '30', 'Cooldown between comments'),
('daily_checkin_points', '10', 'Points awarded for daily check-in'),
('referral_bonus_points', '100', 'Points awarded for successful referral'),
('author_royalty_percentage', '70', 'Percentage of revenue authors receive'),
('min_payout_amount', '50', 'Minimum amount for author payout'),
('moderation_enabled', 'true', 'Whether content moderation is enabled'),
('ai_moderation_enabled', 'false', 'Whether AI moderation is enabled');

-- Banned words (sample)
INSERT INTO banned_words (word, category, severity, action) VALUES
('spam', 'general', 'low', 'flag'),
('hate', 'general', 'medium', 'flag'),
('abuse', 'general', 'high', 'block');

-- Achievements
INSERT INTO achievements (name, description, requirement_type, requirement_value, points_reward) VALUES
('First Steps', 'Read your first chapter', 'chapters_read', 1, 10),
('Bookworm', 'Read 10 chapters', 'chapters_read', 10, 50),
('Avid Reader', 'Read 100 chapters', 'chapters_read', 100, 200),
('Novel Explorer', 'Start reading 5 different novels', 'novels_started', 5, 25),
('Completionist', 'Complete 3 novels', 'novels_completed', 3, 100),
('7-Day Streak', 'Read for 7 consecutive days', 'reading_streak', 7, 50),
('30-Day Streak', 'Read for 30 consecutive days', 'reading_streak', 30, 200),
('Commenter', 'Leave 10 comments', 'comments_posted', 10, 25),
('Reviewer', 'Rate 5 novels', 'novels_rated', 5, 25),
('Supporter', 'Tip an author', 'tips_given', 1, 50),
('Collector', 'Add 10 novels to library', 'library_additions', 10, 25),
('Early Bird', 'Join within first month of launch', 'account_age_days', 30, 100);

-- ============================================
-- VIEWS FOR CONVENIENCE
-- ============================================

-- Novel details view
CREATE VIEW novel_details AS
SELECT 
    n.*,
    p.username as author_username,
    p.display_name as author_display_name,
    p.avatar_url as author_avatar_url,
    p.is_verified as author_is_verified
FROM novels n
JOIN profiles p ON n.author_id = p.id
WHERE n.deleted_at IS NULL;

-- User reading stats view
CREATE VIEW user_reading_stats AS
SELECT 
    rp.user_id,
    COUNT(DISTINCT rp.novel_id) as novels_read,
    COUNT(DISTINCT rp.chapter_id) as chapters_read,
    SUM(rp.total_reading_time) as total_reading_time,
    COUNT(DISTINCT CASE WHEN rp.is_completed THEN rp.novel_id END) as novels_completed
FROM reading_progress rp
GROUP BY rp.user_id;

-- Top spenders view
CREATE VIEW top_spenders AS
SELECT 
    p.id,
    p.username,
    p.display_name,
    p.avatar_url,
    p.total_spent,
    RANK() OVER (ORDER BY p.total_spent DESC) as rank
FROM profiles p
WHERE p.total_spent > 0
ORDER BY p.total_spent DESC;

-- Author earnings view
CREATE VIEW author_earnings AS
SELECT 
    n.author_id,
    p.username,
    p.display_name,
    COUNT(DISTINCT n.id) as total_novels,
    SUM(n.total_views) as total_views,
    SUM(pur.amount) as total_revenue,
    SUM(CASE WHEN pur.purchased_at >= DATE_TRUNC('month', NOW()) THEN pur.amount ELSE 0 END) as monthly_revenue
FROM novels n
JOIN profiles p ON n.author_id = p.id
LEFT JOIN purchases pur ON pur.novel_id = n.id AND pur.status = 'completed'
WHERE n.is_published = TRUE
GROUP BY n.author_id, p.username, p.display_name;