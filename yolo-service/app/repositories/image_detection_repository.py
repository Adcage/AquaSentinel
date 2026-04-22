from __future__ import annotations

from app.core.extensions import db
from app.models.image_detection import ImageDetection


class ImageDetectionRepository:
    def create(self, payload: dict) -> ImageDetection:
        item = ImageDetection(**payload)
        db.session.add(item)
        return item

    def get_by_id(self, item_id: int) -> ImageDetection | None:
        return db.session.get(ImageDetection, item_id)

    def update_fields(self, item: ImageDetection, payload: dict) -> ImageDetection:
        for key, value in payload.items():
            setattr(item, key, value)
        return item

    def delete(self, item: ImageDetection):
        db.session.delete(item)

    def list_paginated(
        self,
        page: int,
        per_page: int,
        image_task_id: int | None = None,
        label: str = "",
    ) -> tuple[list[ImageDetection], int]:
        query = db.session.query(ImageDetection)
        if image_task_id is not None:
            query = query.filter(ImageDetection.image_task_id == image_task_id)
        if label:
            query = query.filter(ImageDetection.label == label)
        total = query.count()
        items = (
            query.order_by(ImageDetection.id.desc())
            .offset((page - 1) * per_page)
            .limit(per_page)
            .all()
        )
        return items, total

    def batch_query(
        self,
        ids: list[int],
        image_task_ids: list[int],
        labels: list[str],
    ) -> list[ImageDetection]:
        query = db.session.query(ImageDetection)
        if ids:
            query = query.filter(ImageDetection.id.in_(ids))
        if image_task_ids:
            query = query.filter(ImageDetection.image_task_id.in_(image_task_ids))
        if labels:
            query = query.filter(ImageDetection.label.in_(labels))
        return query.order_by(ImageDetection.id.desc()).all()
