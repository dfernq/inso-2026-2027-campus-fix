CREATE TABLE IF NOT EXISTS users (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username text NOT NULL UNIQUE,
    role text NOT NULL CHECK (role IN ('REGULAR', 'ADMIN')),
    password_salt bytea NOT NULL CHECK (octet_length(password_salt) = 16),
    password_hash bytea NOT NULL CHECK (octet_length(password_hash) = 32)
);

-- Local development accounts. Hashes use PBKDF2-HMAC-SHA256 with 600,000 iterations.
INSERT INTO users (username, role, password_salt, password_hash) VALUES
    ('alice', 'REGULAR', decode('07ba51a60a5ccaef877fe0d9b5210716', 'hex'),
        decode('6f4f10417b74051806ea7a992d1443c5074f0dc5de6566c37864ba800d0b4a44', 'hex')),
    ('bob', 'REGULAR', decode('3fd1354da47f81ede5b019f5b866b59e', 'hex'),
        decode('48654a6eaa7953fc9d9990f40f59c29d769f48fe65ccd9d40caec6e29d491864', 'hex')),
    ('admin', 'ADMIN', decode('a421272275b19203b59896f391e62202', 'hex'),
        decode('bcfca93dfe9e197d7e1af3fd2fe15b9900c3d5d09739e62c148e7533e4969a6c', 'hex'))
ON CONFLICT (username) DO NOTHING;
