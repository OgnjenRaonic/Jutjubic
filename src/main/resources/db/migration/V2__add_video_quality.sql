-- Add quality column to videos; default MEDIUM
ALTER TABLE videos ADD COLUMN quality varchar(255) NOT NULL DEFAULT 'MEDIUM';
