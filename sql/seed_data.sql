BEGIN TRANSACTION;

-- Countries
INSERT INTO countries (name, code2, region, capital, population) VALUES
('United States', 'US', 'North America', 'Washington, D.C.', 331000000),
('Canada',        'CA', 'North America', 'Ottawa',            38000000),
('United Kingdom','GB', 'Europe',         'London',            67000000);

-- Universities
INSERT INTO universities (name, country, alpha_two_code, domain, web_page) VALUES
('University of Pennsylvania', 'United States', 'US', 'upenn.edu', 'https://www.upenn.edu'),
('University of Toronto',      'Canada',        'CA', 'utoronto.ca', 'https://www.utoronto.ca'),
('University of Oxford',       'United Kingdom','GB', 'ox.ac.uk', 'https://www.ox.ac.uk');

COMMIT;
