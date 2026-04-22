from pathlib import Path


def test_readme_contains_quick_start_checklist():
    text = Path("README.md").read_text(encoding="utf-8")
    assert "10 分钟上手" in text
    assert "ENABLED_MODULES" in text
    assert "flask-smorest" in text


def test_env_example_contains_mysql_and_wechat_placeholders():
    text = Path(".env.example").read_text(encoding="utf-8")
    assert "DATABASE_URL" in text
    assert "WECHAT_APP_ID" in text
    assert "WECHAT_MCH_ID" in text
