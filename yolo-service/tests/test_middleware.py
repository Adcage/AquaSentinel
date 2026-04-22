from app import create_app


def test_response_contains_request_id_and_process_time():
    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
        }
    )
    client = app.test_client()

    response = client.get("/health")
    assert response.status_code == 200
    assert "X-Request-ID" in response.headers
    assert "X-Process-Time" in response.headers


def test_request_id_can_be_propagated_from_header():
    app = create_app(
        {
            "TESTING": True,
            "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
            "ENABLED_MODULES": "health",
        }
    )
    client = app.test_client()

    response = client.get("/health", headers={"X-Request-ID": "req-demo-1"})
    assert response.status_code == 200
    assert response.headers["X-Request-ID"] == "req-demo-1"
