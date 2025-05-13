-- Script to update all events with a default icon
-- To run:
-- psql -U username -d database_name -f update_event_icon_defaults.sql
-- Begin transaction
BEGIN;
-- Update all events that have a null icon or empty string
UPDATE event
SET icon = '⭐️'
WHERE icon IS NULL
    OR icon = '';
-- Update all events that still don't have a color_hex_code
UPDATE event
SET color_hex_code = (
        -- Randomly select one of the predefined color codes
        CASE
            FLOOR(RANDOM() * 4)::INTEGER
            WHEN 0 THEN '#00A676'
            WHEN 1 THEN '#FF7620'
            WHEN 2 THEN '#06AED5'
            WHEN 3 THEN '#FE5E6E'
        END
    )
WHERE color_hex_code IS NULL
    OR color_hex_code = '';
-- Update the last_updated timestamp to current time
UPDATE event
SET last_updated = NOW()
WHERE icon = '⭐️'
    OR (
        color_hex_code IN ('#00A676', '#FF7620', '#06AED5', '#FE5E6E')
    );
-- Commit transaction
COMMIT;
-- Output summary
SELECT COUNT(*) AS "Total Events",
    COUNT(
        CASE
            WHEN icon = '⭐️' THEN 1
        END
    ) AS "Events with Default Icon",
    COUNT(
        CASE
            WHEN color_hex_code IN ('#00A676', '#FF7620', '#06AED5', '#FE5E6E') THEN 1
        END
    ) AS "Events with Default Colors";