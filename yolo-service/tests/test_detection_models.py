import pytest

from app import create_app
from app.core.errors import BusinessError
from app.models.detection_common import ensure_detection_label, get_label_list


def test_detection_label_whitelist():
    app = create_app({"TESTING": True, "ENABLED_MODULES": "health"})
    with app.app_context():
        labels = get_label_list()
    assert "drowning" in labels
    assert "swimming" in labels


def test_detection_label_validation_rejects_unknown_label():
    app = create_app({"TESTING": True, "ENABLED_MODULES": "health"})
    with app.app_context():
        with pytest.raises(BusinessError):
            ensure_detection_label("plane")
