import base64
import hashlib

from fastapi.testclient import TestClient

from app.main import create_app


def test_sync_endpoint_accepts_ciphertext_only_envelope() -> None:
    client = TestClient(create_app())
    payload = encrypted_payload()

    response = client.post("/sync/envelopes", json=payload)

    assert response.status_code == 202
    assert response.json() == {
        "accepted": True,
        "envelope_id": "envelope-1",
        "schema_version": 1,
        "manifest_version": 1,
    }


def test_sync_endpoint_rejects_plaintext_fields() -> None:
    client = TestClient(create_app())
    payload = encrypted_payload()
    payload["plaintext"] = "journal text must not reach the backend"

    response = client.post("/sync/envelopes", json=payload)

    assert response.status_code == 422


def test_sync_endpoint_rejects_checksum_mismatch() -> None:
    client = TestClient(create_app())
    payload = encrypted_payload()
    payload["ciphertext_sha256"] = "0" * 64

    response = client.post("/sync/envelopes", json=payload)

    assert response.status_code == 422


def encrypted_payload() -> dict[str, object]:
    ciphertext = b"encrypted bytes only"
    return {
        "schema_version": 1,
        "envelope_id": "envelope-1",
        "device_id": "phone-a",
        "record_id": "daily-1",
        "record_kind": "DailyTool",
        "manifest_version": 1,
        "key_version": 1,
        "algorithm": "XChaCha20-Poly1305",
        "nonce_base64": base64.b64encode(b"nonce").decode("ascii"),
        "ciphertext_base64": base64.b64encode(ciphertext).decode("ascii"),
        "ciphertext_sha256": hashlib.sha256(ciphertext).hexdigest(),
    }
