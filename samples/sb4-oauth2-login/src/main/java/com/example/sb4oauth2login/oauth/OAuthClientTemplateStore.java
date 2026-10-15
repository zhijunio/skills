package com.example.sb4oauth2login.oauth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
class OAuthClientTemplateStore {
    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    OAuthClientTemplateStore(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    List<OAuthClientTemplateSummary> findAll() {
        return jdbc.sql("""
                        SELECT provider_id, provider_type, client_name, login_button_label, login_button_icon,
                               authorization_uri, token_uri, scopes,
                               user_info_uri, user_name_attribute_name, client_authentication_method,
                               issuer_uri, jwk_set_uri, subject_attribute_name, display_name_attribute_name,
                               email_attribute_name, authorization_parameters::text AS authorization_parameters,
                               description
                        FROM oauth2_client_registration_template
                        ORDER BY client_name
                        """).query(this::mapRow).list();
    }

    private OAuthClientTemplateSummary mapRow(ResultSet rs, int row) throws SQLException {
        String parameters = rs.getString("authorization_parameters");
        Map<String, String> authParameters = parameters == null || parameters.isBlank()
                ? Map.of()
                : jsonMapper.readValue(parameters, new TypeReference<Map<String, String>>() {});
        return new OAuthClientTemplateSummary(
                rs.getString("provider_id"),
                rs.getString("provider_type"),
                rs.getString("client_name"),
                rs.getString("login_button_label"),
                rs.getString("login_button_icon"),
                rs.getString("authorization_uri"),
                rs.getString("token_uri"),
                rs.getString("scopes"),
                rs.getString("user_info_uri"),
                rs.getString("user_name_attribute_name"),
                rs.getString("client_authentication_method"),
                rs.getString("issuer_uri"),
                rs.getString("jwk_set_uri"),
                rs.getString("subject_attribute_name"),
                rs.getString("display_name_attribute_name"),
                rs.getString("email_attribute_name"),
                authParameters,
                rs.getString("description"));
    }
}
