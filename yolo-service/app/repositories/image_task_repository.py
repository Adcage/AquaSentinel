from __future__ import annotations

from datetime import datetime

from app.core.extensions import db
from app.models.image_task import ImageTask


class ImageTaskRepository:
    def create(self, payload: dict) -> ImageTask:
        item = ImageTask(**payload)
        db.session.add(item)
        return item

    def get_by_id(self, item_id: int) -> ImageTask | None:
        return db.session.get(ImageTask, item_id)

    def update_fields(self, item: ImageTask, payload: dict) -> ImageTask:
        for key, value in payload.items():
            setattr(item, key, value)
        return item

    def delete(self, item: ImageTask):
        db.session.delete(item)

    def list_paginated(
        self, page: int, per_page: int, status: str = ""
    ) -> tuple[list[ImageTask], int]:
        query = db.session.query(ImageTask)
        if status:
            query = query.filter(ImageTask.status == status)
        total = query.count()
        items = (
            query.order_by(ImageTask.id.desc())
            .offset((page - 1) * per_page)
            .limit(per_page)
            .all()
        )
        return items, total

    def batch_query(
        self,
        ids: list[int],
        statuses: list[str],
        start_at: datetime | None,
        end_at: datetime | None,
    ) -> list[ImageTask]:
        query = db.session.query(ImageTask)
        if ids:
            query = query.filter(ImageTask.id.in_(ids))
        if statuses:
            query = query.filter(ImageTask.status.in_(statuses))
        if start_at is not None:
            query = query.filter(ImageTask.created_at >= start_at)
        if end_at is not None:
            query = query.filter(ImageTask.created_at <= end_at)
        return query.order_by(ImageTask.id.desc()).all()
