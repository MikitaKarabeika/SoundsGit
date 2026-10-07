ALTER TABLE access_level
    DROP CONSTRAINT chk_access_level_type;

ALTER TABLE access_level
    ADD CONSTRAINT chk_access_level_type CHECK (level IN ('ADMIN', 'CLIENT', 'MODERATOR'));

CREATE TABLE moderator (
    id UUID NOT NULL,
    CONSTRAINT pk_moderator PRIMARY KEY (id),
    CONSTRAINT fk_moderator_access_level FOREIGN KEY (id)
        REFERENCES access_level (id) ON DELETE CASCADE
);

CREATE TABLE moderation_report (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_id UUID,
    last_modified_by_id UUID,
    last_operation_type VARCHAR(20),
    reporter_account_id UUID NOT NULL,
    reported_account_id UUID NOT NULL,
    reporter_login VARCHAR(64) NOT NULL,
    reported_login VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    CONSTRAINT pk_moderation_report PRIMARY KEY (id),
    CONSTRAINT fk_moderation_report_reporter FOREIGN KEY (reporter_account_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_report_reported FOREIGN KEY (reported_account_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_report_created_by FOREIGN KEY (created_by_id)
        REFERENCES account (id),
    CONSTRAINT fk_moderation_report_modified_by FOREIGN KEY (last_modified_by_id)
        REFERENCES account (id),
    CONSTRAINT chk_moderation_report_category
        CHECK (category IN ('HARASSMENT', 'IMPERSONATION', 'SPAM', 'COPYRIGHT', 'OTHER'))
);

CREATE INDEX idx_moderation_report_created_at ON moderation_report (created_at);
CREATE INDEX idx_moderation_report_reported_account ON moderation_report (reported_account_id);

CREATE TABLE moderation_decision (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_id UUID,
    last_modified_by_id UUID,
    last_operation_type VARCHAR(20),
    report_id UUID NOT NULL,
    moderator_account_id UUID NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    CONSTRAINT pk_moderation_decision PRIMARY KEY (id),
    CONSTRAINT uq_moderation_decision_report UNIQUE (report_id),
    CONSTRAINT fk_moderation_decision_report FOREIGN KEY (report_id)
        REFERENCES moderation_report (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_decision_moderator FOREIGN KEY (moderator_account_id)
        REFERENCES account (id),
    CONSTRAINT fk_moderation_decision_created_by FOREIGN KEY (created_by_id)
        REFERENCES account (id),
    CONSTRAINT fk_moderation_decision_modified_by FOREIGN KEY (last_modified_by_id)
        REFERENCES account (id),
    CONSTRAINT chk_moderation_decision_outcome CHECK (outcome IN ('CONFIRMED', 'DISMISSED'))
);

CREATE INDEX idx_moderation_decision_created_at ON moderation_decision (created_at);
