from app.services.tracker_service import DeepSortTracker


def _detection(x_min: float, y_min: float, x_max: float, y_max: float):
    return {
        "label": "person",
        "confidence": 0.92,
        "x_min": x_min,
        "y_min": y_min,
        "x_max": x_max,
        "y_max": y_max,
        "extra_json": {},
    }


def test_simple_tracker_reuses_track_id_for_overlapping_boxes():
    tracker = DeepSortTracker(force_simple=True, iou_threshold=0.2, max_age_sec=2.0)

    first = tracker.update([_detection(10, 10, 110, 110)], timestamp=1.0)
    second = tracker.update([_detection(14, 14, 114, 114)], timestamp=1.1)

    assert len(first) == 1
    assert len(second) == 1
    assert first[0].track_id == second[0].track_id


def test_simple_tracker_creates_new_track_for_far_boxes():
    tracker = DeepSortTracker(force_simple=True, iou_threshold=0.2, max_age_sec=2.0)

    first = tracker.update([_detection(10, 10, 110, 110)], timestamp=1.0)
    second = tracker.update([_detection(260, 260, 360, 360)], timestamp=1.1)

    assert len(first) == 1
    assert len(second) == 1
    assert first[0].track_id != second[0].track_id
