-- Migration to add ErrorLog table for system error monitoring
CREATE TABLE error_log (
    id BINARY(16) NOT NULL PRIMARY KEY,
    error_message VARCHAR(1000) NOT NULL,
    stack_trace TEXT,
    source_class VARCHAR(500) NOT NULL,
    source_method VARCHAR(200) NOT NULL,
    line_number INT NOT NULL,
    error_level VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    resolved_at TIMESTAMP(6),
    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'PENDING',
    admin_comment TEXT,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    user_context VARCHAR(1000)
);

-- Create indexes for better query performance
CREATE INDEX idx_error_log_status ON error_log(status);
CREATE INDEX idx_error_log_occurred_at ON error_log(occurred_at);
CREATE INDEX idx_error_log_email_sent ON error_log(email_sent);
CREATE INDEX idx_error_log_error_level ON error_log(error_level);
CREATE INDEX idx_error_log_source_class ON error_log(source_class);

-- Create compound index for duplicate detection
CREATE INDEX idx_error_log_duplicate_detection ON error_log(source_class, source_method, line_number, occurred_at);

