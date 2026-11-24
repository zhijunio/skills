"""Exercise the portal over HTTP with sessions managed by a cookie jar."""
from http.cookiejar import CookieJar
import json
import sys
import unittest
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import HTTPCookieProcessor, HTTPRedirectHandler, Request, build_opener

ALLOWED = "https://editor.example.test"
OTHER = "https://other.example.test"
DOCUMENT = "/api/documents/1"
PREFERENCES = "/api/preferences"


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Client:
    def __init__(self, base):
        self.base = base
        self.opener = build_opener(HTTPCookieProcessor(CookieJar()), NoRedirect())

    def request(self, method, path, headers=None, body=None):
        request = Request(self.base + path, data=body, headers=headers or {}, method=method)
        try:
            response = self.opener.open(request, timeout=8)
        except HTTPError as error:
            response = error
        with response:
            return response.code, response.headers, response.read()

    def csrf(self):
        status, _, body = self.request("GET", "/api/csrf")
        if status != 200:
            raise AssertionError(f"GET /api/csrf returned {status}")
        token = json.loads(body)
        return {token["headerName"]: token["token"]}

    def login(self, username):
        token = self.csrf()
        form = urlencode({"username": username, "password": "demo-password"}).encode()
        status, headers, _ = self.request("POST", "/login", {
            "Content-Type": "application/x-www-form-urlencoded", **token,
        }, form)
        if status != 302 or headers.get("Location", "").endswith("/login?error"):
            raise AssertionError(f"Login failed for {username}: {status} {headers.get('Location')}")


class PortalAcceptance(unittest.TestCase):
    def authenticated(self, user):
        client = Client(BASE)
        client.login(user)
        return client

    def preflight(self, origin=ALLOWED, path=DOCUMENT, method="PATCH", headers="content-type, x-csrf-token"):
        return Client(BASE).request("OPTIONS", path, {
            "Origin": origin, "Access-Control-Request-Method": method, "Access-Control-Request-Headers": headers,
        })

    def patch(self, client, path, payload, csrf=None, origin=None):
        headers = {"Content-Type": "application/json", **(csrf or {})}
        if origin:
            headers["Origin"] = origin
        return client.request("PATCH", path, headers, json.dumps(payload).encode())

    def state(self, client, path):
        status, _, body = client.request("GET", path)
        self.assertEqual(status, 200)
        return json.loads(body)

    def test_01_editor_preflight_allows_exact_origin_credentials_method_and_headers(self):
        status, headers, _ = self.preflight()
        self.assertIn(status, (200, 204))
        self.assertEqual(headers.get("Access-Control-Allow-Origin"), ALLOWED)
        self.assertEqual(headers.get("Access-Control-Allow-Credentials"), "true")
        methods = {item.strip().upper() for item in headers.get("Access-Control-Allow-Methods", "").split(",")}
        self.assertEqual(methods, {"PATCH"})
        names = {item.strip().lower() for item in headers.get("Access-Control-Allow-Headers", "").split(",")}
        self.assertTrue({"content-type", "x-csrf-token"}.issubset(names))

    def test_02_preflight_does_not_grant_other_origins_methods_or_routes(self):
        for options in (
            {"origin": OTHER}, {"origin": "null"}, {"method": "GET"}, {"method": "POST"},
            {"path": PREFERENCES},
            {"path": "/api/documents/1/nested"},
        ):
            with self.subTest(options=options):
                _, headers, _ = self.preflight(**options)
                self.assertIsNone(headers.get("Access-Control-Allow-Origin"))

    def test_03_owner_writes_with_session_and_csrf(self):
        alice = self.authenticated("alice")
        status, headers, body = self.patch(alice, DOCUMENT, {"title": "Allowed cross-origin title"},
                                           csrf=alice.csrf(), origin=ALLOWED)
        self.assertEqual(status, 200)
        self.assertEqual(headers.get("Access-Control-Allow-Origin"), ALLOWED)
        self.assertEqual(headers.get("Access-Control-Allow-Credentials"), "true")
        self.assertEqual(json.loads(body), {"title": "Allowed cross-origin title"})
        self.assertEqual(self.state(alice, DOCUMENT), {"title": "Allowed cross-origin title"})

    def test_04_missing_invalid_csrf_and_anonymous_caller_leave_state_unchanged(self):
        alice = self.authenticated("alice")
        before = self.state(alice, DOCUMENT)
        for csrf in (None, {"X-CSRF-TOKEN": "invalid"}):
            with self.subTest(csrf=csrf):
                status, _, _ = self.patch(alice, DOCUMENT, {"title": "CSRF bypass"},
                                          csrf=csrf, origin=ALLOWED)
                self.assertEqual(status, 403)
                self.assertEqual(self.state(alice, DOCUMENT), before)
        anonymous = Client(BASE)
        status, _, _ = self.patch(anonymous, DOCUMENT, {"title": "Anonymous edit"},
                                  csrf=anonymous.csrf(), origin=ALLOWED)
        self.assertEqual(status, 401)
        self.assertEqual(self.state(alice, DOCUMENT), before)

    def test_05_same_role_non_owner_is_denied_without_changes(self):
        alice = self.authenticated("alice")
        bob = self.authenticated("bob")
        before = self.state(alice, DOCUMENT)
        status, _, _ = self.patch(bob, DOCUMENT, {"title": "Wrong owner"},
                                  csrf=bob.csrf(), origin=ALLOWED)
        self.assertEqual(status, 404)
        self.assertEqual(self.state(alice, DOCUMENT), before)

    def test_06_adjacent_interface_and_same_origin_writes_keep_existing_policy(self):
        alice = self.authenticated("alice")
        before = self.state(alice, PREFERENCES)
        status, _, _ = self.patch(alice, PREFERENCES, {"theme": "dark"})
        self.assertEqual(status, 403)
        self.assertEqual(self.state(alice, PREFERENCES), before)
        status, headers, body = self.patch(alice, PREFERENCES, {"theme": "dark"}, csrf=alice.csrf())
        self.assertEqual(status, 200)
        self.assertIsNone(headers.get("Access-Control-Allow-Origin"))
        self.assertEqual(json.loads(body), {"theme": "dark"})
        self.assertEqual(self.state(alice, PREFERENCES), {"theme": "dark"})
        status, _, _ = self.patch(alice, DOCUMENT, {"title": "Same-origin title"}, csrf=alice.csrf())
        self.assertEqual(status, 200)
        self.assertEqual(self.state(alice, DOCUMENT), {"title": "Same-origin title"})


if __name__ == "__main__":
    BASE = sys.argv.pop(1)
    unittest.main(verbosity=2)
