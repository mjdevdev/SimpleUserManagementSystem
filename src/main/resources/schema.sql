CREATE TABLE IF NOT EXISTS users (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(100) NULL,
    nickname        VARCHAR(100) NULL,
    email           VARCHAR(100) NULL,
    avatar_url      VARCHAR(500) NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    `admin`         TINYINT(1)   NOT NULL DEFAULT 0,
    oauth2_provider VARCHAR(20)  NULL,
    oauth2_handle   VARCHAR(100) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login      DATETIME     NULL,
    last_logoff     DATETIME     NULL,
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_oauth2 (oauth2_provider, oauth2_handle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
