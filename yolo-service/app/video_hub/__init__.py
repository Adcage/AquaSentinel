from app.video_hub.registry import VideoHubRegistry
from app.video_hub.webrtc_session import WebrtcSessionManager

video_hub_registry = VideoHubRegistry()
webrtc_session_manager = WebrtcSessionManager(video_hub_registry)
