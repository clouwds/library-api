-- Sample data for the library-api schema (author, book).
-- Run manually with psql when you want test data, e.g.:
--   psql -h localhost -U postgres -d library_db -f sample-data.sql
-- Not named data.sql on purpose: Spring Boot would auto-run that on every
-- startup, and these inserts aren't idempotent (isbn has no unique
-- constraint yet, but repeated runs would still duplicate rows).

INSERT INTO author (id, name) VALUES
    (nextval('author_seq'), 'George Orwell'),
    (nextval('author_seq'), 'Ursula K. Le Guin'),
    (nextval('author_seq'), 'Isaac Asimov');

INSERT INTO book (id, title, genre, publication_year, available, isbn, author_id) VALUES
    (nextval('book_seq'), '1984', 'Dystopian', 1949, true, '9780451524935',
        (SELECT id FROM author WHERE name = 'George Orwell')),
    (nextval('book_seq'), 'Animal Farm', 'Satire', 1945, true, '9780451526342',
        (SELECT id FROM author WHERE name = 'George Orwell')),
    (nextval('book_seq'), 'The Left Hand of Darkness', 'Science Fiction', 1969, true, '9780441478125',
        (SELECT id FROM author WHERE name = 'Ursula K. Le Guin')),
    (nextval('book_seq'), 'A Wizard of Earthsea', 'Fantasy', 1968, false, '9780553262506',
        (SELECT id FROM author WHERE name = 'Ursula K. Le Guin')),
    (nextval('book_seq'), 'Foundation', 'Science Fiction', 1951, true, '9780553293357',
        (SELECT id FROM author WHERE name = 'Isaac Asimov')),
    (nextval('book_seq'), 'I, Robot', 'Science Fiction', 1950, false, '9780553294385',
        (SELECT id FROM author WHERE name = 'Isaac Asimov'));
