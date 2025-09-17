DROP TABLE IF EXISTS countries;
CREATE TABLE countries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    code2 TEXT NOT NULL,
    region TEXT,
    capital TEXT,
    population INTEGER
);