# Battlefield Recognition Backend Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在现有 Flask 模板中实现战场目标识别后端（不使用 MySQL），支持图片识别、视频上传识别、历史记录管理、图片识别报告导出，并为每张业务表提供增删改查、分页、批量查询接口。

**Architecture:** 采用模板既有分层（API/Service/Repository/Model），核心业务直接在主应用注册 Blueprint，不做可选模块。文件（图片、视频、报告）走本地存储目录，结构化元数据走 SQLite（可切换其他 SQLAlchemy 支持的数据库）。识别结果按“任务主表 + 明细表”设计，保障可追溯、可分页、可批量查询。

**Tech Stack:** Flask 3.x, SQLAlchemy 2.x, Marshmallow 3.x, flask-smorest, pytest, sqlite

---

## 业务规则（先落规则再编码）

1. 识别类别白名单固定为：`['Tank', 'car', 'civil', 'drone', 'soldier', 'truck']`，接口入参和落库结果都必须校验。
2. 图片识别与视频识别任务均保留原始文件路径、状态、摘要字段，支持历史回看。
3. 上传视频必须持久化存储（本地文件系统），数据库仅保存元数据与相对路径。
4. 无真值标注时，不返回 FP/FN/Precision/Recall，仅返回速度与计数类指标。
5. 每张新建业务表必须有：
   - 单条创建（Create）
   - 按 ID 查询（Read）
   - 单条更新（Update）
   - 单条删除（Delete）
   - 列表分页查询（page/per_page）
   - 批量查询（ids/status/date range 至少一种组合）
6. 批量查询接口使用 `POST /batch-query`，避免 URL 过长问题。
7. 所有接口返回统一响应包结构（`success_payload`），异常统一抛 `BusinessError`。

## 开发与验证执行策略（按你的要求）

1. 先完成所有代码与测试文件编写，再统一执行编译/测试验证。
2. 开发过程中不做“每完成一个文件就跑一次测试”。
3. 统一验证阶段一次性执行：
   - `python -m pytest -v`
   - `python -m ruff check .`
4. 若统一验证失败，再集中修复后重新整体验证，直到通过。

---

### Task 1: 定义领域模型与枚举常量

**Files:**
- Create: `app/models/detection_common.py`
- Create: `app/models/image_task.py`
- Create: `app/models/image_detection.py`
- Create: `app/models/image_report.py`
- Create: `app/models/video_task.py`
- Create: `app/models/video_detection.py`
- Modify: `app/models/__init__.py`
- Test: `tests/test_detection_models.py`

**Step 1: Write the failing test**

```python
def test_detection_label_whitelist():
    from app.models.detection_common import DETECTION_LABELS
    assert DETECTION_LABELS == ["Tank", "car", "civil", "drone", "soldier", "truck"]
```

**Step 2: Write minimal implementation**

```python
DETECTION_LABELS = ["Tank", "car", "civil", "drone", "soldier", "truck"]
```

并补齐 5 张业务表的字段（任务主表 + 明细表 + 报告表）。

**Step 3: 自检任务完成状态**

确认本任务涉及文件均已完成：模型、常量、测试文件。

---

### Task 2: 实现图片任务 Repository（全 CRUD + 分页 + 批量查询）

**Files:**
- Create: `app/repositories/image_task_repository.py`
- Create: `app/repositories/image_detection_repository.py`
- Create: `app/repositories/image_report_repository.py`
- Test: `tests/test_image_repositories.py`

**Step 1: Write the failing test**

```python
def test_image_task_repository_supports_crud_and_pagination():
    # create -> get_by_id -> update -> delete -> list_paginated -> batch_query
    assert True
```

**Step 2: Write minimal implementation**

实现 Repository 方法：
- `create`
- `get_by_id`
- `update_fields`
- `delete`
- `list_paginated(page, per_page, filters)`
- `batch_query(ids, statuses, start_at, end_at)`

**Step 3: 自检任务完成状态**

确认图片相关 Repository 的 CRUD、分页、批量查询能力均已实现。

---

### Task 3: 实现视频任务 Repository（全 CRUD + 分页 + 批量查询）

**Files:**
- Create: `app/repositories/video_task_repository.py`
- Create: `app/repositories/video_detection_repository.py`
- Test: `tests/test_video_repositories.py`

**Step 1: Write the failing test**

```python
def test_video_task_repository_supports_crud_and_batch_query():
    assert True
```

**Step 2: Write minimal implementation**

按 Task 2 同等能力实现视频相关 Repository。

**Step 3: 自检任务完成状态**

确认视频相关 Repository 的 CRUD、分页、批量查询能力均已实现。

---

### Task 4: 实现图片识别 Service（上传、识别、历史、报告）

**Files:**
- Create: `app/services/image_recognition_service.py`
- Modify: `app/services/file_upload_service.py`
- Test: `tests/test_image_recognition_service.py`

**Step 1: Write the failing test**

```python
def test_create_image_task_and_save_detections():
    # 上传图片 -> 创建 image_task -> 保存 image_detection 明细
    assert False
```

**Step 2: Write minimal implementation**

Service 需要提供：
- `create_image_task_from_upload`
- `get_image_task_detail`
- `update_image_task`
- `delete_image_task`
- `list_image_tasks_paginated`
- `batch_query_image_tasks`
- `generate_image_report`

并实现标签白名单校验。

**Step 3: 自检任务完成状态**

确认图片识别 Service、标签白名单校验、报告生成逻辑已完成。

---

### Task 5: 实现视频识别 Service（上传视频存储、任务状态、历史）

**Files:**
- Create: `app/services/video_recognition_service.py`
- Modify: `app/services/file_storage_service.py`
- Test: `tests/test_video_recognition_service.py`

**Step 1: Write the failing test**

```python
def test_upload_video_and_persist_video_task():
    # 上传视频后可在历史中查到，并包含文件路径
    assert False
```

**Step 2: Write minimal implementation**

Service 需要提供：
- `create_video_task_from_upload`
- `get_video_task_detail`
- `update_video_task`
- `delete_video_task`
- `list_video_tasks_paginated`
- `batch_query_video_tasks`
- `append_video_detections`

并约束视频格式与大小。

**Step 3: 自检任务完成状态**

确认视频上传持久化、任务状态流转、历史查询逻辑已完成。

---

### Task 6: 实现图片识别 API（CRUD + 分页 + 批量查询 + 报告导出）

**Files:**
- Create: `app/api/image_recognition.py`
- Modify: `app/__init__.py`
- Test: `tests/test_image_recognition_api.py`

**Step 1: Write the failing test**

```python
def test_image_task_crud_and_batch_query_api():
    # /recognition/images/* 端点全流程
    assert False
```

**Step 2: Write minimal implementation**

API 至少包含：
- `POST /recognition/images/tasks`
- `GET /recognition/images/tasks/<int:task_id>`
- `PUT /recognition/images/tasks/<int:task_id>`
- `DELETE /recognition/images/tasks/<int:task_id>`
- `GET /recognition/images/tasks`
- `POST /recognition/images/tasks/batch-query`
- `POST /recognition/images/tasks/<int:task_id>/report`

**Step 3: 自检任务完成状态**

确认图片识别 API 已覆盖 CRUD、分页、批量查询、报告导出。

---

### Task 7: 实现视频识别 API（CRUD + 分页 + 批量查询 + 上传）

**Files:**
- Create: `app/api/video_recognition.py`
- Modify: `app/__init__.py`
- Test: `tests/test_video_recognition_api.py`

**Step 1: Write the failing test**

```python
def test_video_task_upload_crud_and_pagination_api():
    # /recognition/videos/* 端点全流程
    assert False
```

**Step 2: Write minimal implementation**

API 至少包含：
- `POST /recognition/videos/tasks`
- `GET /recognition/videos/tasks/<int:task_id>`
- `PUT /recognition/videos/tasks/<int:task_id>`
- `DELETE /recognition/videos/tasks/<int:task_id>`
- `GET /recognition/videos/tasks`
- `POST /recognition/videos/tasks/batch-query`

**Step 3: 自检任务完成状态**

确认视频识别 API 已覆盖 CRUD、分页、批量查询、上传入口。

---

### Task 8: 补充配置项与系统级测试

**Files:**
- Modify: `app/core/config.py`
- Modify: `.env.example`
- Create: `tests/test_recognition_end_to_end.py`

**Step 1: Write the failing test**

```python
def test_recognition_end_to_end_flow():
    # 图片上传识别 -> 历史查询 -> 报告导出 -> 视频上传 -> 视频历史分页
    assert False
```

**Step 2: Write minimal implementation**

新增配置：
- `RECOGNITION_IMAGE_SCENE_DIR`
- `RECOGNITION_VIDEO_SCENE_DIR`
- `RECOGNITION_REPORT_SCENE_DIR`
- `VIDEO_ALLOWED_EXTENSIONS`
- `MAX_VIDEO_SIZE_MB`

**Step 3: 自检任务完成状态**

确认新增配置项、`.env.example`、系统级测试已全部落地。

---

## API 最小交付清单（供前端联调）

1. 图片任务：`/recognition/images/tasks`（CRUD + 分页 + batch-query）
2. 图片检测明细：`/recognition/images/tasks/<id>/detections`（查询）
3. 图片报告：`/recognition/images/tasks/<id>/report`（创建 + 查询）
4. 视频任务：`/recognition/videos/tasks`（CRUD + 分页 + batch-query）
5. 视频检测明细：`/recognition/videos/tasks/<id>/detections`（查询）

---

## 验收标准

1. 新建业务表均具备 CRUD、分页、批量查询接口。
2. 类别白名单校验生效，不在白名单内直接返回 400。
3. 上传视频可持久化保存，历史可查到文件元数据。
4. 图片识别报告可导出并回查。
5. `python -m pytest -v` 全部通过。

---

## 统一验证阶段（最后一次性执行）

1. 运行：`python -m pytest -v`
2. 运行：`python -m ruff check .`
3. 若失败：集中修复所有失败项后，重复 1-2，直到全部通过。
