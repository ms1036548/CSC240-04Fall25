DROP TABLE IF EXISTS universities;
CREATE TABLE universities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    country TEXT NOT NULL,
    alpha_two_code TEXT,
    domain TEXT,
    web_page TEXT
);