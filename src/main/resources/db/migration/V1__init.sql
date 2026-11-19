-- M1 初始化：用户、问答记录、问答来源
CREATE TABLE sys_user (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    nickname      VARCHAR(50)   NOT NULL DEFAULT '',
    role          VARCHAR(20)   NOT NULL DEFAULT 'USER',
    status        TINYINT       NOT NULL DEFAULT 1,
    created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                  ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE qa_record (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    question   VARCHAR(1000) NOT NULL,
    answer     MEDIUMTEXT    NULL,
    status     VARCHAR(20)   NOT NULL,
    latency_ms INT           NULL,
    deleted    TINYINT       NOT NULL DEFAULT 0,
    created_at DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_qa_record_user_time (user_id, deleted, created_at),
    KEY idx_qa_record_status (status),
    CONSTRAINT fk_qa_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE qa_source (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    qa_record_id BIGINT        NOT NULL,
    doc_id       VARCHAR(64)   NOT NULL,
    source_file  VARCHAR(255)  NOT NULL DEFAULT '',
    page         INT           NOT NULL DEFAULT 0,
    chunk_text   VARCHAR(1000) NOT NULL DEFAULT '',
    similarity   DOUBLE        NULL,
    rank_no      INT           NOT NULL,
    PRIMARY KEY (id),
    KEY idx_qa_source_record (qa_record_id),
    KEY idx_qa_source_doc (doc_id),
    CONSTRAINT fk_qa_source_record FOREIGN KEY (qa_record_id)
        REFERENCES qa_record (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;