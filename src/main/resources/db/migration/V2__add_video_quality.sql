
-- Add video_quality column to videos for video quality tracking
ALTER TABLE videos ADD COLUMN IF NOT EXISTS quality varchar(255) DEFAULT 'MEDIUM';

