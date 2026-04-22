import main


def test_run_with_dev_flag_enables_hot_reload(monkeypatch):
    captured = {}

    def fake_run(**kwargs):
        captured.update(kwargs)

    monkeypatch.setattr(main.app, "run", fake_run)

    main.run(["--dev"])

    assert captured["debug"] is True
    assert captured["use_reloader"] is True


def test_run_without_dev_flag_disables_hot_reload(monkeypatch):
    captured = {}

    def fake_run(**kwargs):
        captured.update(kwargs)

    monkeypatch.setattr(main.app, "run", fake_run)

    main.run([])

    assert captured["debug"] is False
    assert captured["use_reloader"] is False
