-- ============================================================================
-- V1: Create Activity Service Tables
-- ============================================================================
-- This migration creates the tables owned by the activity-service:
--   activity, activity_type, activity_user, location,
--   chat_message, chat_message_like
--
-- NOTE: The `user` table is NOT created here - it lives in the monolith's
-- database. During the migration period, the activity-service points to the
-- same MySQL instance so foreign keys to `user` still work. Once the databases
-- are fully separated, FK constraints to `user` will be removed.
-- ============================================================================

-- Location table (must be created before activity due to FK)
CREATE TABLE IF NOT EXISTS location (
    id BINARY(16) NOT NULL,
    name VARCHAR(200),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Activity Type table
CREATE TABLE IF NOT EXISTS activity_type (
    id BINARY(16) NOT NULL,
    title VARCHAR(255),
    creator_id BINARY(16) NOT NULL,
    order_num INTEGER,
    icon VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '⭐',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT UK_activity_type_creator_order UNIQUE (creator_id, order_num),
    CONSTRAINT FK_activity_type_creator FOREIGN KEY (creator_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Activity Type - Associated Friends join table
CREATE TABLE IF NOT EXISTS activity_type_associated_friends (
    activity_type_id BINARY(16) NOT NULL,
    associated_friends_id BINARY(16) NOT NULL,
    PRIMARY KEY (activity_type_id, associated_friends_id),
    CONSTRAINT FK_ataf_activity_type FOREIGN KEY (activity_type_id) REFERENCES activity_type(id),
    CONSTRAINT FK_ataf_user FOREIGN KEY (associated_friends_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Activity table
CREATE TABLE IF NOT EXISTS activity (
    id BINARY(16) NOT NULL,
    title VARCHAR(255),
    start_time DATETIME(6),
    end_time DATETIME(6),
    icon VARCHAR(255),
    color_hex_code VARCHAR(255),
    activity_type_id BINARY(16),
    location_id BINARY(16) NOT NULL,
    note VARCHAR(255),
    participant_limit INTEGER,
    creator_id BINARY(16) NOT NULL,
    created_at DATETIME(6),
    last_updated DATETIME(6),
    client_timezone VARCHAR(255),
    PRIMARY KEY (id),
    INDEX idx_activity_creator_id (creator_id),
    INDEX idx_activity_start_time (start_time),
    INDEX idx_activity_end_time (end_time),
    INDEX idx_activity_last_updated (last_updated),
    INDEX idx_activity_created_at (created_at),
    CONSTRAINT FK_activity_type FOREIGN KEY (activity_type_id) REFERENCES activity_type(id) ON DELETE SET NULL,
    CONSTRAINT FK_activity_location FOREIGN KEY (location_id) REFERENCES location(id),
    CONSTRAINT FK_activity_creator FOREIGN KEY (creator_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Activity User (participants/invites) table
CREATE TABLE IF NOT EXISTS activity_user (
    activity_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (activity_id, user_id),
    INDEX idx_activity_id (activity_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status),
    INDEX idx_activity_status (activity_id, status),
    CONSTRAINT FK_au_activity FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE,
    CONSTRAINT FK_au_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat Message table
CREATE TABLE IF NOT EXISTS chat_message (
    id BINARY(16) NOT NULL,
    content VARCHAR(1000),
    timestamp DATETIME(6),
    user_id BINARY(16) NOT NULL,
    activity_id BINARY(16) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT FK_cm_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT FK_cm_activity FOREIGN KEY (activity_id) REFERENCES activity(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat Message Like table
CREATE TABLE IF NOT EXISTS chat_message_like (
    chat_message_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    PRIMARY KEY (chat_message_id, user_id),
    CONSTRAINT FK_cml_chat_message FOREIGN KEY (chat_message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
    CONSTRAINT FK_cml_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
