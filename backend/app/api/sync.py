from fastapi import APIRouter, status

from app.schemas.sync import EncryptedSyncEnvelopeAccepted, EncryptedSyncEnvelopeRequest

router = APIRouter(prefix="/sync", tags=["sync"])


@router.post(
    "/envelopes",
    response_model=EncryptedSyncEnvelopeAccepted,
    status_code=status.HTTP_202_ACCEPTED,
)
def accept_encrypted_sync_envelope(
    envelope: EncryptedSyncEnvelopeRequest,
) -> EncryptedSyncEnvelopeAccepted:
    return EncryptedSyncEnvelopeAccepted(
        accepted=True,
        envelope_id=envelope.envelope_id,
        schema_version=envelope.schema_version,
        manifest_version=envelope.manifest_version,
    )
