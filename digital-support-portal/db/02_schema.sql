-- 02_schema.sql
-- Tables, indexes and permissions. Safe to run twice.
-- The app uses spring.jpa.hibernate.ddl-auto=validate, so these tables must match the entities
-- (UserEntity, AuditEventEntity). If you change an entity, add a new numbered script.

USE TicketingSystem;
GO

-- ---------------------------------------------------------------- users
IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
CREATE TABLE dbo.users (
                           id             BIGINT IDENTITY(1,1) PRIMARY KEY,
                           username       VARCHAR(100)  NOT NULL,
                           password_hash  VARCHAR(MAX)  NOT NULL,
        role           VARCHAR(20)   NOT NULL,
        created_at     DATETIME2(6)  NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at     DATETIME2(6)  NOT NULL DEFAULT SYSUTCDATETIME(),
        enabled        BIT           NOT NULL CONSTRAINT df_users_enabled DEFAULT 1,

        CONSTRAINT uq_users_username UNIQUE (username),
        CONSTRAINT ck_users_role CHECK (role IN ('AGENT', 'ADMIN', 'REVIEWER', 'SRE', 'SYSTEM'))
    );
END
GO

-- ---------------------------------------------------------------- audit_event
IF OBJECT_ID('dbo.audit_event', 'U') IS NULL
BEGIN
CREATE TABLE dbo.audit_event (
                                 id          BIGINT IDENTITY(1,1) PRIMARY KEY,
                                 action      VARCHAR(50)  NOT NULL,
                                 user_id     BIGINT       NULL,
                                 username    VARCHAR(100) NULL,
                                 role        VARCHAR(20)  NULL,
                                 resource    VARCHAR(255) NULL,
                                 outcome     VARCHAR(20)  NOT NULL,

                                 created_at  DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX ix_audit_event_created_at ON dbo.audit_event (created_at DESC);
CREATE INDEX ix_audit_event_user_id    ON dbo.audit_event (user_id, created_at DESC);
END
GO

-- ---------------------------------------------------------------- permissions for the app login
GRANT SELECT, INSERT, UPDATE ON dbo.users       TO portal_app;
GRANT SELECT, INSERT         ON dbo.audit_event TO portal_app;   -- app can add audit rows, not edit or delete them
GO