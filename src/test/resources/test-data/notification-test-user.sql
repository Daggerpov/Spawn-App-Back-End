-- Insert test user for notification controller tests
INSERT INTO user (id, username, name, email, bio, password, verified, date_created, last_updated)
VALUES (
    '12345678-1234-1234-1234-123456789012',
    'notificationtestuser',
    'Notification Test User',
    'notificationtest@example.com',
    'Test bio for notification tests',
    'password123',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
); 