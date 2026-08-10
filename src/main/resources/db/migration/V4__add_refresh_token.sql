CREATE SEQUENCE refresh_token_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE refresh_token (
    id          BIGINT NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    issued_at   TIMESTAMP NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL,
    member_id   BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (token_hash),
    FOREIGN KEY (member_id) REFERENCES member (id)
);
