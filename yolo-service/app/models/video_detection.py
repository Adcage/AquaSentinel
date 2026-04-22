from __future__ import annotations

from datetime import datetime

from sqlalchemy import DateTime, Float, ForeignKey, Integer, JSON, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.extensions import db


class VideoDetection(db.Model):
    __tablename__ = "video_detection"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    video_task_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("video_task.id"), nullable=False
    )
    frame_index: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    timestamp_sec: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    label: Mapped[str] = mapped_column(String(32), nullable=False)
    confidence: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    x_min: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    y_min: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    x_max: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    y_max: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    extra_json: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=db.func.now()
    )
