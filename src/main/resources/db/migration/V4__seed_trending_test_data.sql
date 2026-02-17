-- Seed script za testiranje trending performansi
-- Kreira 1000 videa i 10000 video_view zapisa

-- 1. Kreiraj test korisnika koji će biti autori videa
INSERT INTO users (email, username, password, enabled, created_at)
VALUES ('trend_author@test.com', 'trend_author', '$2a$10$slYQmyNdGzin7olVLc1DO.QmCiUBu6f0Hc0gC4g0G2c6v3yYQZMGu', true, NOW())
ON CONFLICT DO NOTHING;

-- 2. Kreiraj 1000 videa sa raznovrsnim tagovima i komentarima
INSERT INTO videos (author_id, title, description, view_count, comment_count, created_at, thumbnail_path, video_path, quality)
SELECT
    (SELECT id FROM users WHERE username = 'trend_author' LIMIT 1),
    'Trending Video ' || i,
    'Description for trending video ' || i || ' with sample content',
    FLOOR(RANDOM() * 500)::bigint,  -- Random view count 0-500
    FLOOR(RANDOM() * 100)::bigint,  -- Random comment count 0-100
    NOW() - INTERVAL '1 day' * (FLOOR(RANDOM() * 7)::int),  -- Created within last 7 days
    '/thumbnails/video_' || i || '.jpg',
    '/videos/video_' || i || '.mp4',
    'MEDIUM'::varchar
FROM generate_series(1, 1000) AS t(i)
WHERE NOT EXISTS (
    SELECT 1 FROM videos WHERE title = 'Trending Video ' || t.i
);

-- 3. Ubaci tagove za videa (max 3 taga po videu)
INSERT INTO video_tags (video_id, tag)
SELECT v.id, tags[FLOOR(RANDOM() * ARRAY_LENGTH(tags, 1) + 1)::int]
FROM (
    SELECT id FROM videos ORDER BY id LIMIT 1000
) v,
LATERAL UNNEST(ARRAY[
    'action', 'comedy', 'drama', 'music', 'sports',
    'tutorial', 'vlog', 'gaming', 'cooking', 'travel',
    'technology', 'nature', 'fitness', 'education', 'entertainment'
]) tags
WHERE RANDOM() < 0.6  -- 60% chance da svideo dobije ovaj tag
ON CONFLICT DO NOTHING;

-- 4. Kreiraj 10000 video_view zapisa sa stvarnim lokacijama
-- Lokacije su grupirane po gradovima (Beograd, Novi Sad, Nis, itd.)
INSERT INTO video_views (video_id, viewer_lat, viewer_lon, source, cell_lat, cell_lon, geohash, viewed_at)
SELECT
    (SELECT id FROM videos ORDER BY RANDOM() LIMIT 1),
    beograd_lat + (RANDOM() - 0.5) * 0.05,  -- Beograd ± 0.025 deg
    beograd_lon + (RANDOM() - 0.5) * 0.05,
    'GPS',
    FLOOR((beograd_lat + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    FLOOR((beograd_lon + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    SUBSTRING(MD5(RANDOM()::text), 1, 5),  -- Mock geohash
    NOW() - INTERVAL '1 hour' * FLOOR(RANDOM() * 24)::int
FROM generate_series(1, 3000) t(i),
LATERAL (SELECT 45.2671::double precision AS beograd_lat, 19.8335::double precision AS beograd_lon)

UNION ALL

SELECT
    (SELECT id FROM videos ORDER BY RANDOM() LIMIT 1),
    novi_sad_lat + (RANDOM() - 0.5) * 0.05,  -- Novi Sad ± 0.025 deg
    novi_sad_lon + (RANDOM() - 0.5) * 0.05,
    'GPS',
    FLOOR((novi_sad_lat + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    FLOOR((novi_sad_lon + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    SUBSTRING(MD5(RANDOM()::text), 1, 5),
    NOW() - INTERVAL '1 hour' * FLOOR(RANDOM() * 24)::int
FROM generate_series(1, 2500) t(i),
LATERAL (SELECT 45.2517::double precision AS novi_sad_lat, 19.8369::double precision AS novi_sad_lon)

UNION ALL

SELECT
    (SELECT id FROM videos ORDER BY RANDOM() LIMIT 1),
    nis_lat + (RANDOM() - 0.5) * 0.05,  -- Nis ± 0.025 deg
    nis_lon + (RANDOM() - 0.5) * 0.05,
    'GPS',
    FLOOR((nis_lat + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    FLOOR((nis_lon + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    SUBSTRING(MD5(RANDOM()::text), 1, 5),
    NOW() - INTERVAL '1 hour' * FLOOR(RANDOM() * 24)::int
FROM generate_series(1, 2500) t(i),
LATERAL (SELECT 43.3209::double precision AS nis_lat, 21.8954::double precision AS nis_lon)

UNION ALL

SELECT
    (SELECT id FROM videos ORDER BY RANDOM() LIMIT 1),
    kragujevac_lat + (RANDOM() - 0.5) * 0.05,  -- Kragujevac ± 0.025 deg
    kragujevac_lon + (RANDOM() - 0.5) * 0.05,
    'GPS',
    FLOOR((kragujevac_lat + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    FLOOR((kragujevac_lon + (RANDOM() - 0.5) * 0.05) / 0.01)::int,
    SUBSTRING(MD5(RANDOM()::text), 1, 5),
    NOW() - INTERVAL '1 hour' * FLOOR(RANDOM() * 24)::int
FROM generate_series(1, 2000) t(i),
LATERAL (SELECT 44.0165::double precision AS kragujevac_lat, 20.9093::double precision AS kragujevac_lon)

ON CONFLICT DO NOTHING;

-- Ispis statistike
SELECT 'Trending Test Data Loaded!' as status,
       (SELECT COUNT(*) FROM videos) as video_count,
       (SELECT COUNT(*) FROM video_views) as view_count;
