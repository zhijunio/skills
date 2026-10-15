package com.example.sb4oauth2login.oauth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class OAuthClientAuditStore {
    private final JdbcClient jdbc;

    OAuthClientAuditStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void record(String registrationId, String action, String actor, String changedFields, boolean secretChanged) {
        jdbc.sql("""
                        INSERT INTO oauth2_client_audit (
                            registration_id, action, actor, changed_fields, client_secret_changed
                        ) VALUES (:registrationId, :action, :actor, :changedFields, :secretChanged)
                        """)
                .param("registrationId", registrationId)
                .param("action", action)
                .param("actor", actor)
                .param("changedFields", changedFields)
                .param("secretChanged", secretChanged)
                .update();
    }

    List<OAuthClientAuditRecord> findRecent() {
        return jdbc.sql("""
                        SELECT id, registration_id, action, actor, changed_fields,
                               client_secret_changed, occurred_at
                        FROM oauth2_client_audit
                        ORDER BY occurred_at DESC, id DESC
                        LIMIT 100
                        """).query(this::mapRow).list();
    }

    private OAuthClientAuditRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new OAuthClientAuditRecord(
                resultSet.getLong("id"),
                resultSet.getString("registration_id"),
                resultSet.getString("action"),
                resultSet.getString("actor"),
                resultSet.getString("changed_fields"),
                resultSet.getBoolean("client_secret_changed"),
                resultSet.getTimestamp("occurred_at").toInstant());
    }
}
