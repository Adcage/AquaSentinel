# README 与文档目录整理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 整理 AquaSentinel 文档目录，将稳定技术文档和开发过程文档分离，并重写 README 以突出项目能力且不出现简历相关表述。

**Architecture:** 文档按用途分为 architecture、deployment、hardware、troubleshooting 与 archive。README 只引用稳定技术文档，过程计划、阶段总结和 superpowers 资料统一归档。

**Tech Stack:** Markdown、DrawIO、Git、shell 文件迁移、链接校验。

---

### Task 1: 重建 docs 目录分类

**Files:**
- Move: `docs/architecture/system-architecture.md` -> `docs/architecture/system-architecture.md`
- Move: `docs/architecture/architecture-diagram-notes.md` -> `docs/architecture/architecture-diagram-notes.md`
- Move: `docs/architecture/tech-stack.md` -> `docs/architecture/tech-stack.md`
- Move: `docs/deployment/core-stack-quickstart.md` -> `docs/deployment/core-stack-quickstart.md`
- Move: `docs/hardware/stm32-ptz-guide.md` -> `docs/hardware/stm32-ptz-guide.md`
- Move: `docs/hardware/hardware-design-spec.md` -> `docs/hardware/hardware-design-spec.md`
- Move: `docs/hardware/hardware-integration-guide.md` -> `docs/hardware/hardware-integration-guide.md`
- Move: `docs/hardware/hardware-pinout-cross-reference.md` -> `docs/hardware/hardware-pinout-cross-reference.md`
- Move: `docs/hardware/wiring-guide.md` -> `docs/hardware/wiring-guide.md`
- Move: `docs/troubleshooting/webrtc-direct-connect-troubleshooting.md` -> `docs/troubleshooting/webrtc-direct-connect-troubleshooting.md`
- Move: `docs/troubleshooting/ptz-debug-fix-report.md` -> `docs/troubleshooting/ptz-debug-fix-report.md`
- Move process docs into `docs/archive/`

- [ ] **Step 1: Create target folders**

Run: `mkdir -p docs/architecture docs/deployment docs/hardware docs/troubleshooting docs/archive/plans docs/archive/summaries docs/archive/superpowers/specs docs/archive/superpowers/plans`

Expected: target folders exist.

- [ ] **Step 2: Move documents**

Run non-interactive `mv` commands for the file mapping above.

Expected: old process-oriented folders no longer contain stable technical documents.

### Task 2: 修正文档内部链接

**Files:**
- Modify: `docs/hardware/hardware-design-spec.md`
- Modify: `docs/hardware/hardware-pinout-cross-reference.md`

- [ ] **Step 1: Update relative references**

Change references from old `docs/规划文档/...` paths to new `docs/hardware/...` paths.

Expected: hardware docs link to the new file locations.

### Task 3: 改写 README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Remove résumé-oriented section**

Delete the `简历描述参考` section and any first-person project packaging language.

- [ ] **Step 2: Rewrite highlights and navigation**

Use project capability language: endpoint safety loop, video gateway decoupling, AI inference, realtime delivery, Web/Android/hardware collaboration, and engineering governance.

- [ ] **Step 3: Keep README links stable**

Point only to stable technical docs under `docs/architecture`、`docs/deployment` and `docs/hardware`.

### Task 4: 验证整理结果

**Files:**
- Read-only verification across `README.md` and `docs/**/*.md`

- [ ] **Step 1: Search forbidden wording**

Run: search README for `简历|本人负责|面试|答辩`.

Expected: no match in README.

- [ ] **Step 2: Check git status**

Run: `git status --short`.

Expected: only intended README and docs moves/updates appear.
