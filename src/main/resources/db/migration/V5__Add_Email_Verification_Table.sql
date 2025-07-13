-- Migration to add EmailVerification table
CREATE TABLE email_verification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    send_attempts INTEGER DEFAULT 0,
    last_send_attempt_at TIMESTAMP,
    next_send_attempt_at TIMESTAMP,
    check_attempts INTEGER DEFAULT 0,
    last_check_attempt_at TIMESTAMP,
    next_check_attempt_at TIMESTAMP,
    verification_code VARCHAR(6),
    email VARCHAR(255),
    code_expires_at TIMESTAMP,
    user_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for faster lookups by user_id
CREATE INDEX idx_email_verification_user_id ON email_verification(user_id);

-- Create index for faster lookups by email
CREATE INDEX idx_email_verification_email ON email_verification(email); 