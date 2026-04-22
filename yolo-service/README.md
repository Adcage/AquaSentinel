# AI Service

## 10 分钟上手

1. 创建并激活 Python 3.10+ 虚拟环境。
2. 安装依赖：`pip install -r requirements.txt`。
3. 复制 `.env.example` 并按需修改配置。
4. 启动服务：`python main.py`。

## 当前服务边界（最小推理引擎）

- 对外保留：`/health`、`/engine/tasks/start`、`/engine/tasks/stop`、`/engine/tasks/{task_code}`、`/engine/tasks/model/switch`
- 核心职责：流任务生命周期管理、YOLO 推理 + 跟踪 + 三维溺水判定、HMAC 回调 Spring
- 非核心历史接口（图像/视频识别平台、报表、支付等）默认不对外注册

## 配置说明

- `ENABLED_MODULES` 默认固定为 `health`（不再扩展历史业务模块）。
- 通过 `MODEL_VERSION` / `MODEL_VERSION_PATHS_JSON` 管理模型版本。
- 通过 `CALLBACK_URL`、`CALLBACK_KEY`、`CALLBACK_SECRET` 配置回调验签参数。
- API 文档由 `flask-smorest` 提供，默认可通过 `/docs` 与 `/openapi.json` 访问。
