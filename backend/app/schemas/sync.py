from __future__ import annotations

import base64
import hashlib
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

EXPORT_SYNC_ALGORITHM: Literal["XChaCha20-Poly1305"] = "XChaCha20-Poly1305"


class EncryptedSyncEnvelopeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    schema_version: int = Field(gt=0)
    envelope_id: str = Field(min_length=1)
    device_id: str = Field(min_length=1)
    record_id: str = Field(min_length=1)
    record_kind: str = Field(min_length=1)
    manifest_version: int = Field(gt=0)
    key_version: int = Field(gt=0)
    algorithm: Literal["XChaCha20-Poly1305"] = EXPORT_SYNC_ALGORITHM
    nonce_base64: str = Field(min_length=1)
    ciphertext_base64: str = Field(min_length=1)
    ciphertext_sha256: str = Field(pattern=r"^[a-fA-F0-9]{64}$")

    @field_validator("nonce_base64", "ciphertext_base64")
    @classmethod
    def validate_base64(cls, value: str) -> str:
        decoded = cls._decode_base64(value)
        if not decoded:
            raise ValueError("value must decode to non-empty bytes")
        return value

    @model_validator(mode="after")
    def validate_ciphertext_checksum(self) -> EncryptedSyncEnvelopeRequest:
        ciphertext = self._decode_base64(self.ciphertext_base64)
        checksum = hashlib.sha256(ciphertext).hexdigest()
        if checksum != self.ciphertext_sha256.lower():
            raise ValueError("ciphertext checksum mismatch")
        return self

    @staticmethod
    def _decode_base64(value: str) -> bytes:
        try:
            return base64.b64decode(value.encode("ascii"), validate=True)
        except Exception as exc:  # noqa: BLE001 - pydantic converts this into a validation error.
            raise ValueError("value must be valid base64") from exc


class EncryptedSyncEnvelopeAccepted(BaseModel):
    accepted: bool
    envelope_id: str
    schema_version: int
    manifest_version: int
