-- Seed default admin user
-- Password: ForgeOps@Change_Me_Now!
-- BCrypt hash (cost 10)
INSERT INTO users (id, username, email, password_hash, full_name, is_active, must_change_password)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@forgeops.local',
    '$2b$10$fhmR7Jwt7wIrIqRQerbZ5eMuPjNJ3KMyZ9GlK1yua5X0Sm4rIobAG',
    'Administrator',
    TRUE,
    TRUE
);

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin';
