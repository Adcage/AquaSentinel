from pathlib import Path

from app import create_app


def test_sqlite_file_and_tables_are_bootstrapped_automatically(tmp_path):
    db_file = tmp_path / "nested" / "app.db"
    db_uri = f"sqlite:///{db_file.as_posix()}"

    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": db_uri,
            "ENABLED_MODULES": "health",
        }
    )
    client = app.test_client()

    response = client.get("/health")

    assert response.status_code == 200
    assert Path(db_file).exists()
