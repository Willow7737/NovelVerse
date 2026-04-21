-- ===============================
-- NOVEL COVERS BUCKET
-- ===============================

CREATE POLICY "novel_covers_public_read"
ON storage.objects
FOR SELECT
USING (bucket_id = 'novel-covers');

CREATE POLICY "novel_covers_authenticated_insert"
ON storage.objects
FOR INSERT
WITH CHECK (
  bucket_id = 'novel-covers' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "novel_covers_authenticated_update"
ON storage.objects
FOR UPDATE
USING (
  bucket_id = 'novel-covers' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
  bucket_id = 'novel-covers' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "novel_covers_authenticated_delete"
ON storage.objects
FOR DELETE
USING (
  bucket_id = 'novel-covers' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- ===============================
-- CHARACTER AVATARS BUCKET
-- ===============================

CREATE POLICY "character_avatars_public_read"
ON storage.objects
FOR SELECT
USING (bucket_id = 'character-avatars');

CREATE POLICY "character_avatars_authenticated_insert"
ON storage.objects
FOR INSERT
WITH CHECK (
  bucket_id = 'character-avatars' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "character_avatars_authenticated_update"
ON storage.objects
FOR UPDATE
USING (
  bucket_id = 'character-avatars' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
  bucket_id = 'character-avatars' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "character_avatars_authenticated_delete"
ON storage.objects
FOR DELETE
USING (
  bucket_id = 'character-avatars' AND
  auth.uid()::text = owner_id AND
  (storage.foldername(name))[1] = auth.uid()::text
);

-- ===============================
-- USER AVATARS BUCKET
-- ===============================

CREATE POLICY "user_avatars_public_read"
ON storage.objects
FOR SELECT
USING (bucket_id = 'user-avatars');

CREATE POLICY "user_avatars_authenticated_insert"
ON storage.objects
FOR INSERT
WITH CHECK (
  bucket_id = 'user-avatars' AND
  auth.uid()::text = owner_id AND
  name = auth.uid()::text
);

CREATE POLICY "user_avatars_authenticated_update"
ON storage.objects
FOR UPDATE
USING (
  bucket_id = 'user-avatars' AND
  auth.uid()::text = owner_id AND
  name = auth.uid()::text
)
WITH CHECK (
  bucket_id = 'user-avatars' AND
  auth.uid()::text = owner_id AND
  name = auth.uid()::text
);

CREATE POLICY "user_avatars_authenticated_delete"
ON storage.objects
FOR DELETE
USING (
  bucket_id = 'user-avatars' AND
  auth.uid()::text = owner_id AND
  name = auth.uid()::text
);