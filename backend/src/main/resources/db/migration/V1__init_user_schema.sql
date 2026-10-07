/* ==============================================================================
   DATABASE INITIALIZATION SCRIPT: USER MANAGEMENT & SECURITY
   Target Database: PostgreSQL
   Scope: Accounts, Access Levels, Personal Data, Audit Logs, Security Tokens
============================================================================== */

/* ==============================================================================
   1. CORE USER & ACCESS LEVEL TABLES
============================================================================== */

-- TABLE: account
CREATE TABLE account (
                         id                    UUID                        NOT NULL,
                         version               BIGINT                      NOT NULL,
                         created_at            TIMESTAMP WITH TIME ZONE    NOT NULL,
                         modified_at           TIMESTAMP WITH TIME ZONE    NOT NULL,
                         created_by_id         UUID,
                         last_modified_by_id   UUID,
                         last_operation_type   VARCHAR(20),
                         login                 VARCHAR(64)                 NOT NULL,
                         password              VARCHAR(64)                 NOT NULL,
                         active                BOOLEAN                     NOT NULL,
                         verified              BOOLEAN                     NOT NULL,
                         deleted               BOOLEAN                     NOT NULL DEFAULT FALSE,
                         language              VARCHAR(16)                 NOT NULL DEFAULT 'EN',
                         timezone              VARCHAR(64)                 NOT NULL DEFAULT 'UTC',
                         failed_attempts       INTEGER                     NOT NULL DEFAULT 0,
                         lock_until            TIMESTAMP WITH TIME ZONE,
                         last_login_at         TIMESTAMP WITH TIME ZONE,
                         required_new_password BOOLEAN                     NOT NULL DEFAULT FALSE,
                         two_fa_enabled        BOOLEAN                     NOT NULL DEFAULT FALSE,
                         CONSTRAINT pk_account PRIMARY KEY (id),
                         CONSTRAINT uq_account_login UNIQUE (login)
);

-- TABLE: personal_data
CREATE TABLE personal_data (
                               id      UUID        NOT NULL,
                               name    VARCHAR(32) NOT NULL,
                               surname VARCHAR(32) NOT NULL,
                               email   VARCHAR(128) NOT NULL,
                               theme   VARCHAR(32) DEFAULT 'LIGHT',
                               CONSTRAINT pk_personal_data PRIMARY KEY (id),
                               CONSTRAINT uq_personal_data_email UNIQUE (email),
                               CONSTRAINT chk_personal_data_theme CHECK (theme IS NULL OR theme IN ('LIGHT', 'DARK'))
);

-- TABLE: access_level
CREATE TABLE access_level (
                              id                  UUID                        NOT NULL,
                              account_id          UUID                        NOT NULL,
                              level               VARCHAR(31)                 NOT NULL,
                              active              BOOLEAN                     NOT NULL,
                              version             BIGINT                      NOT NULL,
                              created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                              modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                              created_by_id       UUID,
                              last_modified_by_id UUID,
                              last_operation_type VARCHAR(20),
                              CONSTRAINT pk_access_level PRIMARY KEY (id),
                              CONSTRAINT chk_access_level_type CHECK (level IN ('ADMIN', 'CLIENT'))
);
-- SUB-TABLES FOR ACCESS_LEVEL INHERITANCE (JOINED Strategy)
CREATE TABLE admin (
                       id UUID NOT NULL,
                       CONSTRAINT pk_admin PRIMARY KEY (id)
);

CREATE TABLE client (
                        id UUID NOT NULL,
                        CONSTRAINT pk_client PRIMARY KEY (id)
);


/* ==============================================================================
   2. SECURITY & AUTHENTICATION TOKENS
============================================================================== */

-- TABLE: password_reset_token
CREATE TABLE password_reset_token (
                                      id                  UUID                        NOT NULL,
                                      account_id          UUID                        NOT NULL,
                                      token               VARCHAR(255)                NOT NULL,
                                      expires_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                      version             BIGINT                      NOT NULL,
                                      created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                      modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                      created_by_id       UUID,
                                      last_modified_by_id UUID,
                                      last_operation_type VARCHAR(20),
                                      CONSTRAINT pk_password_reset_token PRIMARY KEY (id),
                                      CONSTRAINT uq_password_reset_token_token UNIQUE (token)
);

-- TABLE: verification_token
CREATE TABLE verification_token (
                                    id                  UUID                        NOT NULL,
                                    account_id          UUID                        NOT NULL,
                                    token               VARCHAR(255)                NOT NULL,
                                    expires_date        TIMESTAMP WITH TIME ZONE    NOT NULL,
                                    reminder_sent       BOOLEAN                     NOT NULL,
                                    version             BIGINT                      NOT NULL,
                                    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                    modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                    created_by_id       UUID,
                                    last_modified_by_id UUID,
                                    last_operation_type VARCHAR(20),
                                    CONSTRAINT pk_verification_token PRIMARY KEY (id),
                                    CONSTRAINT uc_verificationtoken_account UNIQUE (account_id),
                                    CONSTRAINT uc_verificationtoken_token UNIQUE (token)
);

-- TABLE: authentication_token
CREATE TABLE authentication_token (
                                      id                  UUID                        NOT NULL,
                                      account_id          UUID                        NOT NULL,
                                      auth_session_id     UUID                        NOT NULL,
                                      token               VARCHAR(64)                 NOT NULL,
                                      expires_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                      version             BIGINT                      NOT NULL,
                                      created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                      modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                      created_by_id       UUID,
                                      last_modified_by_id UUID,
                                      last_operation_type VARCHAR(20),
                                      CONSTRAINT pk_authentication_token PRIMARY KEY (id),
                                      CONSTRAINT uq_auth_token_session_id UNIQUE (auth_session_id)
);

-- TABLE: email_change_token
CREATE TABLE email_change_token (
                                    id                  UUID                        NOT NULL,
                                    account_id          UUID                        NOT NULL,
                                    token               VARCHAR(255)                NOT NULL,
                                    old_email           VARCHAR(255)                NOT NULL,
                                    new_email           VARCHAR(255)                NOT NULL,
                                    expires_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                    version             BIGINT                      NOT NULL,
                                    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                    modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                    created_by_id       UUID,
                                    last_modified_by_id UUID,
                                    last_operation_type VARCHAR(20),
                                    CONSTRAINT pk_email_change_token PRIMARY KEY (id),
                                    CONSTRAINT uc_emailchangetoken_token UNIQUE (token)
);

-- TABLE: activation_token
CREATE TABLE activation_token (
                                  id                  UUID                        NOT NULL,
                                  account_id          UUID                        NOT NULL,
                                  token               VARCHAR(255)                NOT NULL,
                                  expires_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                  version             BIGINT                      NOT NULL,
                                  created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                  modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                  created_by_id       UUID,
                                  last_modified_by_id UUID,
                                  last_operation_type VARCHAR(20),
                                  CONSTRAINT pk_activation_token PRIMARY KEY (id),
                                  CONSTRAINT uq_activation_token_token UNIQUE (token),
                                  CONSTRAINT uq_activation_token_account UNIQUE (account_id)
);

-- TABLE: login_code
CREATE TABLE login_code (
                            id                  UUID                        NOT NULL,
                            account_id          UUID                        NOT NULL,
                            code                VARCHAR(64)                 NOT NULL,
                            auth_session_id     UUID                        NOT NULL,
                            expires_date        TIMESTAMP WITH TIME ZONE    NOT NULL,
                            version             BIGINT                      NOT NULL,
                            created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                            modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                            created_by_id       UUID,
                            last_modified_by_id UUID,
                            last_operation_type VARCHAR(20),
                            CONSTRAINT pk_login_code PRIMARY KEY (id),
                            CONSTRAINT uq_login_code_account UNIQUE (account_id),
                            CONSTRAINT uq_login_code_session_id UNIQUE (auth_session_id)
);

-- TABLE: google_account
CREATE TABLE google_account (
                                id                  UUID                        NOT NULL,
                                account_id          UUID                        NOT NULL,
                                google_id           VARCHAR(255)                NOT NULL,
                                version             BIGINT                      NOT NULL,
                                created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                created_by_id       UUID,
                                last_modified_by_id UUID,
                                last_operation_type VARCHAR(20),
                                CONSTRAINT pk_google_account PRIMARY KEY (id),
                                CONSTRAINT uc_google_account_account UNIQUE (account_id),
                                CONSTRAINT uc_google_account_google UNIQUE (google_id)
);


/* ==============================================================================
   3. AUDITING & HISTORY TABLES
============================================================================== */

-- TABLE: login_audit
CREATE TABLE login_audit (
                             id                      UUID                        NOT NULL,
                             account_id              UUID                        NOT NULL,
                             last_login_at           TIMESTAMP WITH TIME ZONE,
                             last_successful_auth_ip VARCHAR(45),
                             last_failed_auth_time   TIMESTAMP WITH TIME ZONE,
                             last_failed_auth_ip     VARCHAR(45),
                             version                 BIGINT                      NOT NULL,
                             created_at              TIMESTAMP WITH TIME ZONE    NOT NULL,
                             modified_at             TIMESTAMP WITH TIME ZONE    NOT NULL,
                             created_by_id           UUID,
                             last_modified_by_id     UUID,
                             last_operation_type     VARCHAR(20),
                             CONSTRAINT pk_login_audit PRIMARY KEY (id),
                             CONSTRAINT uc_login_audit_account UNIQUE (account_id)
);

-- TABLE: password_history
CREATE TABLE password_history (
                                  id                  UUID                        NOT NULL,
                                  account_id          UUID                        NOT NULL,
                                  password_hash       VARCHAR(64),
                                  version             BIGINT                      NOT NULL,
                                  created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                  modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                  created_by_id       UUID,
                                  last_modified_by_id UUID,
                                  last_operation_type VARCHAR(20),
                                  CONSTRAINT pk_password_history PRIMARY KEY (id)
);

CREATE TABLE error_translation (
                                   id                  UUID                        NOT NULL,
                                   version             BIGINT                      NOT NULL,
                                   created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                                   modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                                   created_by_id       UUID,
                                   last_modified_by_id UUID,
                                   last_operation_type VARCHAR(20),
                                   original_text       VARCHAR(255)                NOT NULL,
                                   language_code       VARCHAR(2)                  NOT NULL,
                                   translated_text     VARCHAR(255)                NOT NULL,
                                   CONSTRAINT pk_error_translation PRIMARY KEY (id),
                                   CONSTRAINT uq_original_text_language_code UNIQUE (original_text, language_code)
);

-- TABLE: opinion
CREATE TABLE opinion (
                         id                  UUID                        NOT NULL,
                         version             BIGINT                      NOT NULL,
                         created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
                         modified_at         TIMESTAMP WITH TIME ZONE    NOT NULL,
                         created_by_id       UUID,
                         last_modified_by_id UUID,
                         last_operation_type VARCHAR(20),
                         score               INTEGER                     NOT NULL,
                         comment             VARCHAR(255),
                         client_id           UUID                        NOT NULL,
    -- meal_id          UUID                        NOT NULL, -- Раскомментировать при появлении Meal
                         CONSTRAINT pk_opinion PRIMARY KEY (id),
                         CONSTRAINT chk_opinion_score CHECK (score >= 1 AND score <= 5)
    -- CONSTRAINT uq_client_meal UNIQUE (client_id, meal_id) -- Раскомментировать при появлении Meal
);


/* ==============================================================================
   4. INDEXES
============================================================================== */

CREATE INDEX idx_auth_token_account_id ON authentication_token (account_id);


/* ==============================================================================
   5. FOREIGN KEY CONSTRAINTS
============================================================================== */

-- Personal Data FK
ALTER TABLE personal_data
    ADD CONSTRAINT fk_personal_data_account FOREIGN KEY (id) REFERENCES account (id) ON DELETE CASCADE;

-- Access Levels FKs
ALTER TABLE access_level
    ADD CONSTRAINT fk_access_level_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

ALTER TABLE admin
    ADD CONSTRAINT fk_admin_access_level FOREIGN KEY (id) REFERENCES access_level (id) ON DELETE CASCADE;

ALTER TABLE client
    ADD CONSTRAINT fk_client_access_level FOREIGN KEY (id) REFERENCES access_level (id) ON DELETE CASCADE;

-- Security Tokens FKs
ALTER TABLE password_reset_token
    ADD CONSTRAINT fk_password_reset_token_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

ALTER TABLE verification_token
    ADD CONSTRAINT fk_verification_token_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

ALTER TABLE authentication_token
    ADD CONSTRAINT fk_auth_token_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

ALTER TABLE email_change_token
    ADD CONSTRAINT fk_email_change_token_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

ALTER TABLE google_account
    ADD CONSTRAINT fk_google_account_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

-- Audit & History FKs
ALTER TABLE login_audit
    ADD CONSTRAINT fk_login_audit_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

ALTER TABLE password_history
    ADD CONSTRAINT fk_password_history_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

-- FKs for AbstractEntity Audit fields (created_by_id & last_modified_by_id)
ALTER TABLE account ADD CONSTRAINT fk_account_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE account ADD CONSTRAINT fk_account_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE access_level ADD CONSTRAINT fk_access_level_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE access_level ADD CONSTRAINT fk_access_level_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE password_reset_token ADD CONSTRAINT fk_pwd_reset_token_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE password_reset_token ADD CONSTRAINT fk_pwd_reset_token_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE verification_token ADD CONSTRAINT fk_verif_token_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE verification_token ADD CONSTRAINT fk_verif_token_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE authentication_token ADD CONSTRAINT fk_auth_token_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE authentication_token ADD CONSTRAINT fk_auth_token_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE email_change_token ADD CONSTRAINT fk_email_token_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE email_change_token ADD CONSTRAINT fk_email_token_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE google_account ADD CONSTRAINT fk_google_acc_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE google_account ADD CONSTRAINT fk_google_acc_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE login_audit ADD CONSTRAINT fk_login_audit_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE login_audit ADD CONSTRAINT fk_login_audit_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);

ALTER TABLE password_history ADD CONSTRAINT fk_pwd_history_created_by FOREIGN KEY (created_by_id) REFERENCES account (id);
ALTER TABLE password_history ADD CONSTRAINT fk_pwd_history_last_modified_by FOREIGN KEY (last_modified_by_id) REFERENCES account (id);


/* ==============================================================================
   6. PERMISSIONS (GRANTS)
============================================================================== */

GRANT ALL ON SCHEMA public TO PUBLIC;

-- MOK
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "${mok_user}";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO "${mok_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "${mok_user}";

-- MOF
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "${mof_user}";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO "${mof_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "${mof_user}";