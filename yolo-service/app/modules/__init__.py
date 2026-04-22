from __future__ import annotations

from dataclasses import dataclass
import importlib


@dataclass(frozen=True)
class ModuleSpec:
    factory_path: str
    requires: tuple[str, ...] = ()


MODULE_REGISTRY: dict[str, ModuleSpec] = {
    "health": ModuleSpec("app.modules.health:create_module"),
}


def _parse_enabled_modules(raw: str) -> list[str]:
    modules: list[str] = []
    for part in raw.split(","):
        item = part.strip()
        if item and item not in modules:
            modules.append(item)
    return modules


def _load_factory(factory_path: str):
    module_path, attr_name = factory_path.split(":", maxsplit=1)
    module = importlib.import_module(module_path)
    return getattr(module, attr_name)


def register_enabled_modules(app) -> list[str]:
    raw_value = app.config.get("ENABLED_MODULES", "health")
    enabled_modules = _parse_enabled_modules(raw_value)

    for module_name in enabled_modules:
        if module_name not in MODULE_REGISTRY:
            raise RuntimeError(f"Unknown module: '{module_name}'")

    enabled_set = set(enabled_modules)
    for module_name in enabled_modules:
        spec = MODULE_REGISTRY[module_name]
        for required_module in spec.requires:
            if required_module not in enabled_set:
                raise RuntimeError(
                    f"Module '{module_name}' depends on '{required_module}'"
                )

    for module_name in enabled_modules:
        spec = MODULE_REGISTRY[module_name]
        factory = _load_factory(spec.factory_path)
        factory(app)

    app.extensions["enabled_modules"] = enabled_modules
    return enabled_modules
