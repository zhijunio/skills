INSERT INTO oauth2_client_registration_template (
    provider_id, provider_type, client_name, login_button_label, login_button_icon,
    authorization_uri, token_uri, scopes, user_info_uri, user_name_attribute_name,
    client_authentication_method, issuer_uri, jwk_set_uri, subject_attribute_name,
    display_name_attribute_name, email_attribute_name, description
) VALUES (
    'dex', 'OIDC', 'Dex', '使用 Dex 登录', 'D',
    'http://localhost:5556/dex/auth', 'http://localhost:5556/dex/token', 'openid profile email',
    'http://localhost:5556/dex/userinfo', 'sub', 'client_secret_basic',
    'http://localhost:5556/dex', 'http://localhost:5556/dex/keys',
    'sub', 'name', 'email', '本地 Docker Dex 示例；选择模板后填入 sample-web 客户端凭据'
)
ON CONFLICT (provider_id) DO NOTHING;
