from __future__ import annotations

from app.core.extensions import db
from app.models.image_report import ImageReport


class ImageReportRepository:
    def create(self, payload: dict) -> ImageReport:
        item = ImageReport(**payload)
        db.session.add(item)
        return item

    def get_by_id(self, item_id: int) -> ImageReport | None:
        return db.session.get(ImageReport, item_id)

    def update_fields(self, item: ImageReport, payload: dict) -> ImageReport:
        for key, value in payload.items():
            setattr(item, key, value)
        return item

    def delete(self, item: ImageReport):
        db.session.delete(item)

    def list_paginated(
        self,
        page: int,
        per_page: int,
        image_task_id: int | None = None,
        status: str = "",
    ) -> tuple[list[ImageReport], int]:
        query = db.session.query(ImageReport)
        if image_task_id is not None:
            query = query.filter(ImageReport.image_task_id == image_task_id)
        if status:
            query = query.filter(ImageReport.status == status)
        total = query.count()
        items = (
            query.order_by(ImageReport.id.desc())
            .offset((page - 1) * per_page)
            .limit(per_page)
            .all()
        )
        return items, total

    def batch_query(
        self,
        ids: list[int],
        image_task_ids: list[int],
        statuses: list[str],
    ) -> list[ImageReport]:
        query = db.session.query(ImageReport)
        if ids:
            query = query.filter(ImageReport.id.in_(ids))
        if image_task_ids:
            query = query.filter(ImageReport.image_task_id.in_(image_task_ids))
        if statuses:
            query = query.filter(ImageReport.status.in_(statuses))
        return query.order_by(ImageReport.id.desc()).all()
