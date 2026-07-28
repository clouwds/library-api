CREATE SEQUENCE author_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE book_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE member_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE loan_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE author (
    id   BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE book (
    id                BIGINT NOT NULL,
    title             VARCHAR(255) NOT NULL,
    genre             VARCHAR(255) NOT NULL,
    publication_year  INTEGER NOT NULL,
    available         BOOLEAN NOT NULL,
    isbn              VARCHAR(255) NOT NULL,
    version           INTEGER NOT NULL,
    author_id         BIGINT,
    PRIMARY KEY (id),
    UNIQUE (isbn),
    FOREIGN KEY (author_id) REFERENCES author (id)
);

CREATE TABLE member (
    id         BIGINT NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE loan (
    id          BIGINT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date    DATE NOT NULL,
    return_date DATE,
    book_id     BIGINT,
    member_id   BIGINT,
    PRIMARY KEY (id),
    FOREIGN KEY (book_id) REFERENCES book (id),
    FOREIGN KEY (member_id) REFERENCES member (id)
);
