from __future__ import annotations

from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Integer, JSON, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.extensions import db


class ImageReport(db.Model):
    __tablename__ = "image_report"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    image_task_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("image_task.id"), nullable=False
    )
    report_name: Mapped[str] = mapped_column(String(255), nullable=False)
    relative_path: Mapped[str] = mapped_column(String(512), nullable=False)
    content_type: Mapped[str] = mapped_column(String(64), nullable=False, default="")
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="SUCCESS")
    summary_json: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=db.func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=db.func.now(), onupdate=db.func.now()
    )
