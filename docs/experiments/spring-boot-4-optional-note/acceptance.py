"""Exercise document note compatibility over HTTP."""
from http.cookiejar import CookieJar
import json
import sys
import unittest
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import HTTPCookieProcessor, HTTPRedirectHandler, Request, build_opener

DOCUMENT = "/api/documents/1"
PREFERENCES = "/api/preferences"
OMITTED = object()


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


class NoteAcceptance(unittest.TestCase):
    def authenticated(self, user):
        client = Client(BASE)
        client.login(user)
        return client

    def patch(self, client, payload, csrf=None):
        headers = {"Content-Type": "application/json", **(csrf or {})}
        return client.request("PATCH", DOCUMENT, headers, json.dumps(payload).encode())

    def document(self, client):
        status, _, body = client.request("GET", DOCUMENT)
        self.assertEqual(status, 200)
        return json.loads(body)

    def assert_document(self, client, title, note=OMITTED):
        body = self.document(client)
        self.assert_shape(body, title, note)
        return body

    def assert_shape(self, body, title, note=OMITTED):
        self.assertEqual(body.get("title"), title)
        if note is OMITTED:
            self.assertNotIn("note", body)
            self.assertEqual(set(body), {"title"})
        else:
            self.assertEqual(body.get("note"), note)
            self.assertEqual(set(body), {"title", "note"})

    def test_01_old_requests_keep_shape_and_note_rules_are_distinct(self):
        alice = self.authenticated("alice")
        self.assert_document(alice, "Alice draft")

        status, _, body = self.patch(alice, {"title": "Renamed"}, alice.csrf())
        self.assertEqual(status, 200)
        self.assert_shape(json.loads(body), "Renamed")
        self.assert_document(alice, "Renamed")

        status, _, body = self.patch(alice, {"title": "Noted", "note": "hello"}, alice.csrf())
        self.assertEqual(status, 200)
        self.assert_shape(json.loads(body), "Noted", "hello")
        self.assert_document(alice, "Noted", "hello")

        status, _, body = self.patch(alice, {"title": "Title only"}, alice.csrf())
        self.assertEqual(status, 200)
        written = json.loads(body)
        self.assertEqual(written.get("title"), "Title only")
        if "note" in written:
            self.assertEqual(written.get("note"), "hello")
        self.assert_document(alice, "Title only", "hello")

        status, _, body = self.patch(alice, {"title": "Cleared", "note": None}, alice.csrf())
        self.assertEqual(status, 200)
        self.assert_shape(json.loads(body), "Cleared")
        self.assert_document(alice, "Cleared")

        status, _, body = self.patch(alice, {"title": "Empty", "note": ""}, alice.csrf())
        self.assertEqual(status, 200)
        self.assert_shape(json.loads(body), "Empty", "")
        self.assert_document(alice, "Empty", "")

        status, _, _ = self.patch(alice, {"title": "Still empty"}, alice.csrf())
        self.assertEqual(status, 200)
        self.assert_document(alice, "Still empty", "")

        exact = "n" * 40
        status, _, body = self.patch(alice, {"title": "Bounded", "note": exact}, alice.csrf())
        self.assertEqual(status, 200)
        self.assert_shape(json.loads(body), "Bounded", exact)
        before = self.assert_document(alice, "Bounded", exact)

        status, headers, _ = self.patch(alice, {"title": "Too long", "note": exact + "x"}, alice.csrf())
        self.assertEqual(status, 400)
        self.assertIn("json", headers.get("Content-Type", ""))
        self.assertEqual(self.document(alice), before)

        status, headers, _ = self.patch(alice, {"note": "missing title"}, alice.csrf())
        self.assertEqual(status, 400)
        self.assertIn("json", headers.get("Content-Type", ""))
        self.assertEqual(self.document(alice), before)

    def test_02_denials_leave_state_and_preferences_stay_unchanged(self):
        alice = self.authenticated("alice")
        before = self.document(alice)

        bob = self.authenticated("bob")
        status, _, _ = self.patch(bob, {"title": "Wrong owner", "note": "nope"}, bob.csrf())
        self.assertEqual(status, 404)
        self.assertEqual(self.document(alice), before)

        status, _, _ = self.patch(alice, {"title": "Missing token"}, None)
        self.assertEqual(status, 403)
        self.assertEqual(self.document(alice), before)

        anonymous = Client(BASE)
        status, _, _ = self.patch(anonymous, {"title": "Anonymous"}, anonymous.csrf())
        self.assertEqual(status, 401)
        self.assertEqual(self.document(alice), before)

        token = alice.csrf()
        status, _, body = alice.request("PATCH", PREFERENCES, {
            "Content-Type": "application/json", **token,
        }, json.dumps({"theme": "dark"}).encode())
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(body), {"theme": "dark"})
        status, _, body = alice.request("GET", PREFERENCES)
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(body), {"theme": "dark"})
        self.assertEqual(self.document(alice), before)


if __name__ == "__main__":
    BASE = sys.argv.pop(1)
    unittest.main(verbosity=2)
