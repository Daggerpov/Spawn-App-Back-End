-- Script to update all Activities with a default icon
-- To run:
-- mysql -u username -p database_name < update_Activity_icon_defaults.sql

-- Begin transaction
START TRANSACTION;

-- Update all Activities that have a null icon or empty string
UPDATE Activity
SET icon = '⭐️'
WHERE icon IS NULL
    OR icon = '';

-- Update all Activities that still don't have a color_hex_code
UPDATE Activity
SET color_hex_code = (
        -- Randomly select one of the predefined color codes
        CASE
            FLOOR(RAND() * 4)
            WHEN 0 THEN '#00A676'
            WHEN 1 THEN '#FF7620'
            WHEN 2 THEN '#06AED5'
            WHEN 3 THEN '#FE5E6E'
        END
    )
WHERE color_hex_code IS NULL
    OR color_hex_code = '';

-- Update the last_updated timestamp to current time
UPDATE Activity
SET last_updated = NOW()
WHERE icon = '⭐️'
    OR (
        color_hex_code IN ('#00A676', '#FF7620', '#06AED5', '#FE5E6E')
    );

-- Commit transaction
COMMIT;