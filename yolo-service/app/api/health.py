from flask import current_app
from flask_smorest import Blueprint

from app.api.schemas import ResponseEnvelopeSchema
from app.core.response import success_payload

blp = Blueprint("health", __name__, description="Health checks")


@blp.route("/health", methods=["GET"])
@blp.response(200, ResponseEnvelopeSchema)
def health_check():
    enabled_modules = current_app.extensions.get("enabled_modules", [])
    return success_payload(
        {
            "status": "ok",
            "enabled_modules": enabled_modules,
        }
    )
