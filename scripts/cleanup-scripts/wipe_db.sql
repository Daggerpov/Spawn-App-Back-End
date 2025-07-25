-- This will drop ALL tables in the database
SET FOREIGN_KEY_CHECKS = 0;

-- Generate DROP statements for all tables
SET @tables = NULL;
SELECT GROUP_CONCAT(table_name) INTO @tables
FROM information_schema.tables
WHERE table_schema = DATABASE();

SET @tables = CONCAT('DROP TABLE IF EXISTS ', REPLACE(@tables, ',', ', '));
PREPARE stmt FROM @tables;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

-- DANIEL, NOTE TO SELF:
-- 1. connect to redis via. Railway connection command, (Raw redis-cli command)
-- 2. run `FLUSHDB` to clear the database