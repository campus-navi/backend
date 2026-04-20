CREATE TABLE official_source
(
    id              BIGSERIAL PRIMARY KEY,
    university_id   BIGINT REFERENCES university (id),
    campus_id       BIGINT REFERENCES campus (id),
    college_id      BIGINT REFERENCES college (id),
    department_id   BIGINT REFERENCES department (id),
    category        VARCHAR(50)  NOT NULL,
    source_type     VARCHAR(20)  NOT NULL,
    parser_type     VARCHAR(50)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    list_url        VARCHAR(500) NOT NULL,
    last_crawled_at TIMESTAMP,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE TABLE official_post
(
    id            BIGSERIAL PRIMARY KEY,
    source_id     BIGINT       NOT NULL REFERENCES official_source (id),
    university_id BIGINT REFERENCES university (id),
    campus_id     BIGINT REFERENCES campus (id),
    college_id    BIGINT REFERENCES college (id),
    department_id BIGINT REFERENCES department (id),
    category      VARCHAR(50)  NOT NULL,
    original_id   VARCHAR(200) NOT NULL,
    title         VARCHAR(500) NOT NULL,
    plain_text    TEXT,
    html_content  TEXT,
    source_url    VARCHAR(500) NOT NULL,
    published_at  TIMESTAMP,
    crawled_at    TIMESTAMP    NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    UNIQUE (source_id, original_id)
);

CREATE TABLE official_attachment
(
    id            BIGSERIAL PRIMARY KEY,
    post_id       BIGINT       NOT NULL REFERENCES official_post (id),
    original_name VARCHAR(500) NOT NULL,
    s3_key        VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    is_image      BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order    SMALLINT     NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);