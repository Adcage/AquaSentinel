from __future__ import annotations

from app.core.extensions import db
from app.models.video_detection import VideoDetection


class VideoDetectionRepository:
    def create(self, payload: dict) -> VideoDetection:
        item = VideoDetection(**payload)
        db.session.add(item)
        return item

    def get_by_id(self, item_id: int) -> VideoDetection | None:
        return db.session.get(VideoDetection, item_id)

    def update_fields(self, item: VideoDetection, payload: dict) -> VideoDetection:
        for key, value in payload.items():
            setattr(item, key, value)
        return item

    def delete(self, item: VideoDetection):
        db.session.delete(item)

    def list_paginated(
        self,
        page: int,
        per_page: int,
        video_task_id: int | None = None,
        frame_index: int | None = None,
        label: str = "",
    ) -> tuple[list[VideoDetection], int]:
        query = db.session.query(VideoDetection)
        if video_task_id is not None:
            query = query.filter(VideoDetection.video_task_id == video_task_id)
        if frame_index is not None:
            query = query.filter(VideoDetection.frame_index == frame_index)
        if label:
            query = query.filter(VideoDetection.label == label)
        total = query.count()
        items = (
            query.order_by(VideoDetection.id.desc())
            .offset((page - 1) * per_page)
            .limit(per_page)
            .all()
        )
        return items, total

    def batch_query(
        self,
        ids: list[int],
        video_task_ids: list[int],
        labels: list[str],
    ) -> list[VideoDetection]:
        query = db.session.query(VideoDetection)
        if ids:
            query = query.filter(VideoDetection.id.in_(ids))
        if video_task_ids:
            query = query.filter(VideoDetection.video_task_id.in_(video_task_ids))
        if labels:
            query = query.filter(VideoDetection.label.in_(labels))
        return query.order_by(VideoDetection.id.desc()).all()

    def list_by_task_and_frame(
        self,
        video_task_id: int,
        frame_index: int,
    ) -> list[VideoDetection]:
        return (
            db.session.query(VideoDetection)
            .filter(VideoDetection.video_task_id == video_task_id)
            .filter(VideoDetection.frame_index == frame_index)
            .order_by(VideoDetection.id.asc())
            .all()
        )
