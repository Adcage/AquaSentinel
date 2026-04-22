from app.services.drowning_rule_service import DrowningRuleEvaluator
from app.services.tracker_service import TrackedObject


def _tracked_object(posture_score: float, thermal_score: float) -> TrackedObject:
    return TrackedObject(
        track_id="track_1",
        x_min=10.0,
        y_min=10.0,
        x_max=120.0,
        y_max=90.0,
        confidence=0.94,
        label="person",
        extra_json={
            "posture_score": posture_score,
            "thermal_score": thermal_score,
        },
    )


def test_drowning_rule_requires_duration_before_trigger():
    evaluator = DrowningRuleEvaluator(
        min_duration_sec=2.0,
        posture_threshold=0.7,
        thermal_threshold=0.8,
        cooldown_sec=5.0,
    )

    first = evaluator.evaluate(_tracked_object(0.9, 0.95), timestamp=10.0)
    second = evaluator.evaluate(_tracked_object(0.9, 0.95), timestamp=11.5)
    third = evaluator.evaluate(_tracked_object(0.9, 0.95), timestamp=12.2)

    assert first.triggered is False
    assert second.triggered is False
    assert third.triggered is True
    assert third.duration_abnormal is True


def test_drowning_rule_respects_cooldown():
    evaluator = DrowningRuleEvaluator(
        min_duration_sec=1.0,
        posture_threshold=0.6,
        thermal_threshold=0.8,
        cooldown_sec=3.0,
    )

    first = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=10.0)
    triggered = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=11.2)
    blocked_by_cooldown = evaluator.evaluate(
        _tracked_object(0.95, 0.95), timestamp=12.0
    )
    triggered_again = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=14.5)

    assert first.triggered is False
    assert triggered.triggered is True
    assert blocked_by_cooldown.triggered is False
    assert triggered_again.triggered is True


def test_drowning_rule_tolerates_single_brief_dip_without_reset():
    evaluator = DrowningRuleEvaluator(
        min_duration_sec=3.0,
        posture_threshold=0.7,
        thermal_threshold=0.8,
        cooldown_sec=5.0,
    )

    first = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=10.0)
    second = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=11.0)
    dip = evaluator.evaluate(_tracked_object(0.65, 0.95), timestamp=11.4)
    third = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=12.2)
    fourth = evaluator.evaluate(_tracked_object(0.95, 0.95), timestamp=13.6)

    assert first.triggered is False
    assert second.triggered is False
    assert dip.triggered is False
    assert third.triggered is False
    assert fourth.triggered is True
