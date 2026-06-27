-- DevOps Training Lab 1 - Initial Database Setup
CREATE TABLE IF NOT EXISTS app_info (
    id SERIAL PRIMARY KEY,
    key VARCHAR(100) NOT NULL,
    value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_info (key, value) VALUES
    ('lab', 'DevOps Training Lab 1'),
    ('stack', 'Java Spring Boot + Maven + PostgreSQL'),
    ('version', '1.0.0');
