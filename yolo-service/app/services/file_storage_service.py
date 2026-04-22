from __future__ import annotations

from datetime import datetime
from hashlib import sha256
from pathlib import Path
from uuid import uuid4

from flask import current_app

from app.core.errors import BusinessError


def _storage_root() -> Path:
    configured_root = str(current_app.config.get("STORAGE_ROOT", "instance/storage"))
    root = Path(configured_root)
    if not root.is_absolute():
        root = Path.cwd() / root
    root.mkdir(parents=True, exist_ok=True)
    return root


def build_storage_path(category: str, filename: str) -> Path:
    safe_name = Path(filename or "").name or "file.bin"
    target = _storage_root() / category / safe_name
    target.parent.mkdir(parents=True, exist_ok=True)
    return target


def save_bytes(
    category: str, filename: str, content: bytes, content_type: str = ""
) -> dict:
    target = build_storage_path(category, filename)
    target.write_bytes(content)
    return {
        "path": target.as_posix(),
        "size": len(content),
        "sha256": sha256(content).hexdigest(),
        "content_type": content_type,
    }


def parse_scene_mapping() -> dict[str, str]:
    raw = str(current_app.config.get("UPLOAD_SCENE_DIRS", "default:uploads"))
    mapping: dict[str, str] = {}
    for item in raw.split(","):
        pair = item.strip()
        if not pair or ":" not in pair:
            continue
        scene, scene_dir = pair.split(":", 1)
        scene_name = scene.strip()
        normalized_dir = scene_dir.strip().replace("\\", "/").strip("/")
        if scene_name and normalized_dir:
            mapping[scene_name] = normalized_dir

    if "default" not in mapping:
        mapping["default"] = "uploads"
    return mapping


def resolve_scene_dir(scene: str) -> str:
    mapping = parse_scene_mapping()
    scene_value = (scene or "default").strip() or "default"
    scene_dir = mapping.get(scene_value)
    if scene_dir is None:
        raise BusinessError("unknown upload scene", status_code=400)
    return scene_dir


def save_upload_bytes(
    scene: str, original_name: str, content: bytes, content_type: str
) -> dict:
    scene_value = (scene or "default").strip() or "default"
    scene_dir = resolve_scene_dir(scene_value)
    extension = Path(original_name or "").suffix.lower()
    stored_name = f"{uuid4().hex}{extension}"
    relative_path = (
        Path(scene_dir)
        / f"{datetime.now().year:04d}"
        / f"{datetime.now().month:02d}"
        / stored_name
    )
    target = _storage_root() / relative_path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)

    return {
        "scene": scene_value,
        "original_name": original_name,
        "stored_name": stored_name,
        "relative_path": relative_path.as_posix(),
        "size": len(content),
        "sha256": sha256(content).hexdigest(),
        "content_type": content_type,
    }


def resolve_storage_path(path_text: str) -> Path:
    path = Path(path_text)
    if path.is_absolute():
        return path
    return (Path.cwd() / path).resolve()


def delete_storage_file(path_text: str | None) -> bool:
    if not path_text:
        return False
    try:
        path = Path(path_text)
        # If it's not absolute, resolve it relative to the storage root
        if not path.is_absolute():
            path = _storage_root() / path

        # Ensure it's resolved to avoid any .. tricks
        path = path.resolve()

        # Security check: ensure the resolved path is still within the storage root
        if not str(path).startswith(str(_storage_root().resolve())):
            return False

        if path.exists() and path.is_file():
            path.unlink()
            return True
    except Exception:
        pass
    return False
