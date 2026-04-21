-- ============================================================
-- RUN THIS IN YOUR SUPABASE SQL EDITOR
-- Fixes HTTP 403 on review submission by adding INSERT policy
-- ============================================================

-- Allow authenticated users to read all ratings (for display)
CREATE POLICY "Anyone can view ratings"
    ON ratings FOR SELECT
    USING (true);

-- Allow users to insert their own rating
CREATE POLICY "Users can insert their own ratings"
    ON ratings FOR INSERT
    WITH CHECK (user_id = auth.uid());

-- Allow users to update their own rating
CREATE POLICY "Users can update their own ratings"
    ON ratings FOR UPDATE
    USING (user_id = auth.uid());

-- Allow users to delete their own rating
CREATE POLICY "Users can delete their own ratings"
    ON ratings FOR DELETE
    USING (user_id = auth.uid());
