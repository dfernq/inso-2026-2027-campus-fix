CREATE TABLE IF NOT EXISTS categories (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    active boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS issues (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporter_id bigint NOT NULL REFERENCES users (id),
    category_id bigint NOT NULL REFERENCES categories (id),
    title text NOT NULL CHECK (char_length(btrim(title)) BETWEEN 1 AND 120),
    description text NOT NULL CHECK (char_length(btrim(description)) BETWEEN 1 AND 2000),
    location text NOT NULL CHECK (char_length(btrim(location)) BETWEEN 1 AND 120),
    status text NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN'))
);

CREATE INDEX IF NOT EXISTS issues_reporter_id_idx ON issues (reporter_id, id);

INSERT INTO categories (name)
SELECT name FROM (VALUES ('Electrical'), ('Facilities'), ('Plumbing')) AS defaults(name)
WHERE NOT EXISTS (SELECT 1 FROM categories)
ON CONFLICT (name) DO NOTHING;
