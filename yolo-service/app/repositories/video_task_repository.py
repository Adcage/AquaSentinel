from __future__ import annotations

from datetime import datetime

from app.core.extensions import db
from app.models.video_task import VideoTask


class VideoTaskRepository:
    def create(self, payload: dict) -> VideoTask:
        item = VideoTask(**payload)
        db.session.add(item)
        return item

    def get_by_id(self, item_id: int) -> VideoTask | None:
        return db.session.get(VideoTask, item_id)

    def update_fields(self, item: VideoTask, payload: dict) -> VideoTask:
        for key, value in payload.items():
            setattr(item, key, value)
        return item

    def delete(self, item: VideoTask):
        db.session.delete(item)

    def list_paginated(
        self, page: int, per_page: int, status: str = ""
    ) -> tuple[list[VideoTask], int]:
        query = db.session.query(VideoTask)
        if status:
            query = query.filter(VideoTask.status == status)
        total = query.count()
        items = (
            query.order_by(VideoTask.id.desc())
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
    ) -> list[VideoTask]:
        query = db.session.query(VideoTask)
        if ids:
            query = query.filter(VideoTask.id.in_(ids))
        if statuses:
            query = query.filter(VideoTask.status.in_(statuses))
        if start_at is not None:
            query = query.filter(VideoTask.created_at >= start_at)
        if end_at is not None:
            query = query.filter(VideoTask.created_at <= end_at)
        return query.order_by(VideoTask.id.desc()).all()
