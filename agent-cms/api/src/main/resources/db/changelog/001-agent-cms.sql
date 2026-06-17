--liquibase formatted sql
--changeset cosmic:agent-cms-core
CREATE TABLE agent_cms_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role_name VARCHAR(32) NOT NULL DEFAULT 'OWNER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE agent_cms_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    entity_key VARCHAR(180) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    reason VARCHAR(500) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_cms_audit_user FOREIGN KEY (actor_user_id) REFERENCES agent_cms_users(id) ON DELETE SET NULL
);
