"""CasaNotify TV protocol and platform constants."""

from homeassistant.const import Platform

DOMAIN = "casanotify_tv"
PORT = 8765
PLATFORMS = [
    Platform.BINARY_SENSOR,
    Platform.BUTTON,
    Platform.NOTIFY,
    Platform.NUMBER,
    Platform.SELECT,
    Platform.SENSOR,
    Platform.SWITCH,
    Platform.TEXT,
    Platform.TIME,
]
THEMES = ["lime", "ocean", "ember", "rose", "paper"]
POSITIONS = [
    "top_left",
    "top_center",
    "top_right",
    "center_left",
    "center",
    "center_right",
    "bottom_left",
    "bottom_center",
    "bottom_right",
]
ICONS = [
    "home",
    "bell",
    "door",
    "camera",
    "light",
    "check",
    "warning",
    "info",
    "sensor",
    "motion",
    "temperature",
    "humidity",
    "phone",
    "delivery",
    "chat",
    "battery",
    "wifi",
    "lock",
    "smoke",
    "water",
    "alarm",
]
TONES = ["soft", "doorbell", "chime", "pulse", "alarm", "digital"]
BACKDROPS = ["none", "custom"]
UI_THEMES = ["system", "light", "dark"]
ANIMATIONS = ["slide", "fade", "none"]
PALETTES = {
    "lime": ("#18231D", "#F3F8F2", "#A3E635"),
    "ocean": ("#122330", "#F1F7FC", "#58C6F5"),
    "ember": ("#2B2118", "#FFF8F0", "#FFBE65"),
    "rose": ("#301C29", "#FFF3F7", "#F78FAE"),
    "paper": ("#F5F4ED", "#18241B", "#34754B"),
}
DEFAULTS = {
    "tone": "soft",
    "backdrop": "none",
    "theme": "lime",
    "icon": "bell",
    "position": "top_right",
    "animation": "slide",
    "duration": 10,
    "width": 380,
    "text_size": 18,
    "opacity": 0.8,
    "radius": 20,
    "margin": 28,
    "volume": 50,
    "sound": False,
    "speak": False,
    "progress": True,
}


def resolved_defaults(config):
    """Reflect native Android defaults, including theme-specific colors."""
    values = DEFAULTS | config.get("defaults", {})
    palette = PALETTES.get(values["theme"], PALETTES["lime"])
    return dict(zip(("background", "text_color", "accent"), palette)) | values
