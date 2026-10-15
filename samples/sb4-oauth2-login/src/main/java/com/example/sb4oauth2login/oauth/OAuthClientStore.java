package com.example.sb4oauth2login.oauth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
class OAuthClientStore {
    private static final String COLUMNS =
            "registration_id, provider_type, client_name, login_button_label, login_button_icon, client_id, encrypted_client_secret, "
                    + "client_authentication_method, redirect_uri, scopes, authorization_uri, token_uri, user_info_uri, "
                    + "user_name_attribute_name, issuer_uri, jwk_set_uri, authorization_parameters::text AS authorization_parameters, "
                    + "subject_attribute_name, display_name_attribute_name, email_attribute_name, enabled";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    OAuthClientStore(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    List<OAuthClientConfiguration> findAll() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM oauth2_client ORDER BY client_name, registration_id")
                .query(this::mapRow)
                .list();
    }

    Optional<OAuthClientConfiguration> findByRegistrationId(String registrationId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM oauth2_client WHERE registration_id = :registrationId")
                .param("registrationId", registrationId)
                .query(this::mapRow)
                .optional();
    }

    Optional<OAuthClientConfiguration> findByRegistrationIdForUpdate(String registrationId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM oauth2_client WHERE registration_id = :registrationId FOR UPDATE")
                .param("registrationId", registrationId)
                .query(this::mapRow)
                .optional();
    }

    void insert(OAuthClientConfiguration client) {
        save(client, false);
    }

    void update(OAuthClientConfiguration client) {
        save(client, true);
    }

    private void save(OAuthClientConfiguration client, boolean updateOnConflict) {
        String upsert = updateOnConflict ? """
                        ON CONFLICT (registration_id) DO UPDATE SET
                            client_name = EXCLUDED.client_name,
                            provider_type = EXCLUDED.provider_type,
                            login_button_label = EXCLUDED.login_button_label,
                            login_button_icon = EXCLUDED.login_button_icon,
                            client_id = EXCLUDED.client_id,
                            encrypted_client_secret = EXCLUDED.encrypted_client_secret,
                            client_authentication_method = EXCLUDED.client_authentication_method,
                            authorization_grant_type = EXCLUDED.authorization_grant_type,
                            redirect_uri = EXCLUDED.redirect_uri,
                            scopes = EXCLUDED.scopes,
                            authorization_uri = EXCLUDED.authorization_uri,
                            token_uri = EXCLUDED.token_uri,
                            user_info_uri = EXCLUDED.user_info_uri,
                            user_name_attribute_name = EXCLUDED.user_name_attribute_name,
                            issuer_uri = EXCLUDED.issuer_uri,
                            jwk_set_uri = EXCLUDED.jwk_set_uri,
                            authorization_parameters = EXCLUDED.authorization_parameters,
                            subject_attribute_name = EXCLUDED.subject_attribute_name,
                            display_name_attribute_name = EXCLUDED.display_name_attribute_name,
                            email_attribute_name = EXCLUDED.email_attribute_name,
                            enabled = EXCLUDED.enabled,
                            updated_at = CURRENT_TIMESTAMP
                        """ : "";
        jdbc.sql("""
                        INSERT INTO oauth2_client (
                            registration_id, provider_type, client_name, login_button_label, login_button_icon, client_id, encrypted_client_secret,
                            client_authentication_method, authorization_grant_type, redirect_uri, scopes,
                            authorization_uri, token_uri, user_info_uri, user_name_attribute_name,
                            issuer_uri, jwk_set_uri, authorization_parameters, subject_attribute_name,
                            display_name_attribute_name, email_attribute_name, enabled
                        ) VALUES (
                            :registrationId, :providerType, :clientName, :loginButtonLabel, :loginButtonIcon, :clientId, :encryptedClientSecret,
                            :clientAuthenticationMethod, 'authorization_code', :redirectUri, :scopes,
                            :authorizationUri, :tokenUri, :userInfoUri, :userNameAttributeName,
                            :issuerUri, :jwkSetUri, CAST(:authorizationParameters AS jsonb), :subjectAttributeName,
                            :displayNameAttributeName, :emailAttributeName, :enabled
                        )
                        """ + upsert)
                .param("registrationId", client.registrationId())
                .param("providerType", client.providerType())
                .param("clientName", client.clientName())
                .param("loginButtonLabel", client.loginButtonLabel())
                .param("loginButtonIcon", client.loginButtonIcon())
                .param("clientId", client.clientId())
                .param("encryptedClientSecret", client.encryptedClientSecret())
                .param("clientAuthenticationMethod", client.clientAuthenticationMethod())
                .param("redirectUri", client.redirectUri())
                .param("scopes", client.scopes())
                .param("authorizationUri", client.authorizationUri())
                .param("tokenUri", client.tokenUri())
                .param("userInfoUri", client.userInfoUri())
                .param("userNameAttributeName", client.userNameAttributeName())
                .param("issuerUri", client.issuerUri())
                .param("jwkSetUri", client.jwkSetUri())
                .param("authorizationParameters", writeJson(client.authorizationParameters()))
                .param("subjectAttributeName", client.subjectAttributeName())
                .param("displayNameAttributeName", client.displayNameAttributeName())
                .param("emailAttributeName", client.emailAttributeName())
                .param("enabled", client.enabled())
                .update();
    }

    boolean delete(String registrationId) {
        return jdbc.sql("DELETE FROM oauth2_client WHERE registration_id = :registrationId")
                        .param("registrationId", registrationId)
                        .update()
                == 1;
    }

    private OAuthClientConfiguration mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new OAuthClientConfiguration(
                resultSet.getString("registration_id"),
                resultSet.getString("provider_type"),
                resultSet.getString("client_name"),
                resultSet.getString("login_button_label"),
                resultSet.getString("login_button_icon"),
                resultSet.getString("client_id"),
                resultSet.getString("encrypted_client_secret"),
                resultSet.getString("client_authentication_method"),
                resultSet.getString("redirect_uri"),
                resultSet.getString("scopes"),
                resultSet.getString("authorization_uri"),
                resultSet.getString("token_uri"),
                resultSet.getString("user_info_uri"),
                resultSet.getString("user_name_attribute_name"),
                resultSet.getString("issuer_uri"),
                resultSet.getString("jwk_set_uri"),
                readAuthorizationParameters(resultSet.getString("authorization_parameters")),
                resultSet.getString("subject_attribute_name"),
                resultSet.getString("display_name_attribute_name"),
                resultSet.getString("email_attribute_name"),
                resultSet.getBoolean("enabled"));
    }

    private String writeJson(Map<String, String> values) {
        return jsonMapper.writeValueAsString(values == null ? Map.of() : values);
    }

    private Map<String, String> readAuthorizationParameters(String json) {
        if (json == null || json.isBlank()) return Map.of();
        return jsonMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    }
}
