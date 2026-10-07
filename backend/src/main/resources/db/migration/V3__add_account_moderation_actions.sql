CREATE TABLE moderation_action (
    id UUID NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_id UUID,
    last_modified_by_id UUID,
    last_operation_type VARCHAR(20),
    account_id UUID NOT NULL,
    actor_account_id UUID NOT NULL,
    report_id UUID,
    action_type VARCHAR(24) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_moderation_action PRIMARY KEY (id),
    CONSTRAINT fk_moderation_action_account FOREIGN KEY (account_id)
        REFERENCES account (id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_action_actor FOREIGN KEY (actor_account_id)
        REFERENCES account (id),
    CONSTRAINT fk_moderation_action_report FOREIGN KEY (report_id)
        REFERENCES moderation_report (id) ON DELETE SET NULL,
    CONSTRAINT uq_moderation_action_report UNIQUE (report_id),
    CONSTRAINT chk_moderation_action_type
        CHECK (action_type IN ('WARNING', 'TIMEOUT', 'PERMANENT_BAN', 'LIFT')),
    CONSTRAINT chk_moderation_action_expiry CHECK (
        (action_type = 'TIMEOUT' AND expires_at IS NOT NULL)
        OR (action_type <> 'TIMEOUT' AND expires_at IS NULL)
    ),
    CONSTRAINT fk_moderation_action_created_by FOREIGN KEY (created_by_id)
        REFERENCES account (id),
    CONSTRAINT fk_moderation_action_modified_by FOREIGN KEY (last_modified_by_id)
        REFERENCES account (id)
);

CREATE INDEX idx_moderation_action_account_created
    ON moderation_action (account_id, created_at DESC, id DESC);
