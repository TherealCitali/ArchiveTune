#!/usr/bin/env python3
"""Replace Android vector drawables with Solar Broken icons (CC BY 4.0, 480 Design)."""

from __future__ import annotations

import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

SOLAR_JSON = Path("/tmp/solar/solar.json")
DRAWABLE = Path("/home/user/ArchiveTune/app/src/main/res/drawable")

SKIP = {
    "about_appbar.xml",
    "about_splash.xml",
    "app_icon_small.xml",
    "small_icon.xml",
}

# Existing drawable stem -> Solar icon name without -broken suffix
MAP = {
    "account": "user-circle",
    "add": "add-circle",
    "add_circle": "add-circle",
    "ai": "magic-stick-3",
    "album": "album",
    "all_inclusive": "infinite",
    "alternate_email": "mention-circle",
    "android_cell": "smartphone-2",
    "animation": "rewind-forward",
    "arrow_back": "alt-arrow-left",
    "arrow_downward": "alt-arrow-down",
    "arrow_forward": "alt-arrow-right",
    "arrow_top_left": "arrow-left-up",
    "arrow_upward": "alt-arrow-up",
    "artist": "user",
    "auto_awesome": "stars",
    "backup": "cloud-upload",
    "bedtime": "moon-sleep",
    "block": "forbidden",
    "bluetooth": "bluetooth",
    "blur_off": "eye-closed",
    "blur_on": "radial-blur",
    "bolt": "bolt",
    "bookmark": "bookmark",
    "bookmark_filled": "bookmark",
    "buttons": "widget-2",
    "cached": "refresh",
    "calendar_today": "calendar",
    "check": "check-read",
    "clear_all": "broom",
    "close": "close-circle",
    "coffee": "cup",
    "contrast": "palette-round",
    "copy": "copy",
    "dark_mode": "moon",
    "delete": "trash-bin-trash",
    "delete_history": "history",
    "deselect": "record-circle",
    "desktop_windows": "monitor",
    "discord": "dialog-2",
    "discover_tune": "compass",
    "done": "check-circle",
    "download": "download",
    "drag_handle": "hamburger-menu",
    "edit": "pen",
    "equalizer": "tuning-2",
    "error": "danger-triangle",
    "expand_less": "alt-arrow-up",
    "expand_more": "alt-arrow-down",
    "experiment": "test-tube",
    "explicit": "danger-circle",
    "explore_outlined": "compass-big",
    "fast_forward": "rewind-forward",
    "favorite": "heart",
    "favorite_border": "heart",
    "filter_alt": "filter",
    "fire": "fire",
    "format_align_center": "align-horizontal-center",
    "format_align_left": "align-left",
    "format_paint": "palette",
    "github": "code-circle",
    "gradient": "palette-2",
    "graphic_eq": "soundwave",
    "grid_view": "widget-4",
    "hide_image": "gallery-remove",
    "history": "history",
    "home_filled": "home-smile",
    "home_outlined": "home-smile",
    "ic_download": "download-minimalistic",
    "ic_music": "music-note-2",
    "ic_pause_white": "pause",
    "image": "gallery",
    "info": "info-circle",
    "input": "login-2",
    "integration": "widget",
    "join": "users-group-rounded",
    "kick": "user-minus",
    "language": "global",
    "leave": "logout-2",
    "library_add": "add-folder",
    "library_add_check": "folder-check",
    "library_filled": "library",
    "library_music": "music-library-2",
    "library_outlined": "library",
    "link": "link",
    "list": "list",
    "listening": "headphones-round",
    "location_on": "map-point",
    "lock": "lock",
    "lock_open": "lock-unlocked",
    "login": "login-3",
    "logout": "logout-3",
    "lyrics": "document-text",
    "manage_search": "magnifier",
    "mic": "microphone-3",
    "mix": "tuning",
    "more_horiz": "menu-dots",
    "more_vert": "menu-dots",
    "motion_photos_on": "play-circle",
    "multi_user": "users-group-two-rounded",
    "music_note": "music-note-3",
    "nav_bar": "slider-minimalistic-horizontal",
    "navigate_next": "alt-arrow-right",
    "new_release": "medal-ribbons-star",
    "newspaper": "notebook",
    "offline": "wi-fi-router-minimalistic",
    "palette": "palette",
    "pause": "pause",
    "person": "user",
    "play": "play",
    "playlist_add": "playlist-minimalistic-2",
    "playlist_import": "import",
    "playlist_local": "folder-with-files",
    "playlist_online": "playlist",
    "playlist_play": "playlist-2",
    "queue_music": "playlist-minimalistic-3",
    "radio": "podcast",
    "radio_button_checked": "check-circle",
    "radio_button_unchecked": "record",
    "remove": "minus-circle",
    "repeat": "repeat",
    "repeat_on": "repeat",
    "repeat_one": "repeat-one",
    "repeat_one_on": "repeat-one-minimalistic",
    "replay": "refresh-circle",
    "restore": "history-2",
    "screenshot": "screencast",
    "search": "magnifier",
    "search_off": "minimalistic-magnifier",
    "security": "shield-check",
    "select_all": "check-square",
    "settings": "settings",
    "share": "share",
    "shortcut_library": "library",
    "shortcut_music_recognition": "soundwave-circle",
    "shortcut_search": "magnifier",
    "shuffle": "shuffle",
    "shuffle_on": "shuffle",
    "skip_next": "skip-next",
    "skip_previous": "skip-previous",
    "sliders": "slider-vertical",
    "slow_motion_video": "playback-speed",
    "snippet_folder": "folder",
    "speed": "forward",
    "star": "star",
    "stats": "chart-2",
    "status": "graph-up",
    "storage": "server",
    "style": "palette-2",
    "subscribe": "bell",
    "subscribed": "bell-bing",
    "swipe": "sort",
    "sync": "refresh",
    "tab": "window-frame",
    "telegram": "plain-2",
    "text_fields": "text-field",
    "timer": "stopwatch",
    "token": "key",
    "translate": "translation-2",
    "trending_up": "graph-up",
    "tune": "tuning-4",
    "update": "refresh-circle",
    "vibration": "smartphone-vibration",
    "visibility_off": "eye-closed",
    "volume_off": "volume-cross",
    "volume_up": "volume-loud",
    "waves": "soundwave",
    "website": "global",
    "wifi_proxy": "wi-fi-router-round",
}


def resolve_icon(data: dict, name: str) -> dict | None:
    icons = data["icons"]
    aliases = data.get("aliases") or {}
    seen = set()
    while name and name not in icons:
        if name in seen:
            return None
        seen.add(name)
        alias = aliases.get(name)
        if not alias:
            return None
        name = alias.get("parent") or ""
    return icons.get(name)


def circle_to_path(cx: float, cy: float, r: float) -> str:
    return (
        f"M {cx - r},{cy} "
        f"a {r},{r} 0 1,0 {2 * r},0 "
        f"a {r},{r} 0 1,0 {-2 * r},0"
    )


def inherit(parent: dict, el: ET.Element) -> dict:
    out = dict(parent)
    for k, v in el.attrib.items():
        out[k] = v
    return out


def collect_paths(el: ET.Element, inherited: dict, acc: list[dict]) -> None:
    tag = el.tag.split("}")[-1]
    attrs = inherit(inherited, el)
    if tag == "g":
        for child in list(el):
            collect_paths(child, attrs, acc)
        return
    if tag == "path":
        d = attrs.get("d")
        if d:
            acc.append({**attrs, "d": d})
        return
    if tag == "circle":
        try:
            cx, cy, r = float(attrs["cx"]), float(attrs["cy"]), float(attrs["r"])
        except (KeyError, ValueError):
            return
        acc.append({**attrs, "d": circle_to_path(cx, cy, r)})
        return
    if tag == "ellipse":
        try:
            cx, cy = float(attrs["cx"]), float(attrs["cy"])
            rx, ry = float(attrs["rx"]), float(attrs["ry"])
        except (KeyError, ValueError):
            return
        acc.append(
            {
                **attrs,
                "d": (
                    f"M {cx - rx},{cy} "
                    f"a {rx},{ry} 0 1,0 {2 * rx},0 "
                    f"a {rx},{ry} 0 1,0 {-2 * rx},0"
                ),
            }
        )
        return
    for child in list(el):
        collect_paths(child, attrs, acc)


def path_xml(p: dict) -> str:
    fill = p.get("fill", "none")
    stroke = p.get("stroke", "none")
    if fill in ("currentColor", "#000", "#000000", "black"):
        fill_color = "@android:color/white"
    elif fill in (None, "none", ""):
        fill_color = "@android:color/transparent"
    else:
        fill_color = "@android:color/white"
    stroke_color = None
    if stroke in ("currentColor", "#000", "#000000", "black") or (
        stroke not in (None, "none", "") and stroke != "none"
    ):
        stroke_color = "@android:color/white"
    sw = p.get("stroke-width") or p.get("strokeWidth") or "1.5"
    cap = p.get("stroke-linecap") or p.get("strokeLinecap")
    join = p.get("stroke-linejoin") or p.get("strokeLinejoin")
    attrs = [
        f'        android:fillColor="{fill_color}"',
        f'        android:pathData="{p["d"]}"',
    ]
    if stroke_color:
        attrs.append(f'        android:strokeColor="{stroke_color}"')
        attrs.append(f'        android:strokeWidth="{sw}"')
        if cap:
            attrs.append(f'        android:strokeLineCap="{cap}"')
        if join:
            attrs.append(f'        android:strokeLineJoin="{join}"')
    return "    <path\n" + "\n".join(attrs) + " />"


def to_vector(body: str, width: int = 24, height: int = 24) -> str:
    wrapped = f'<svg xmlns="http://www.w3.org/2000/svg">{body}</svg>'
    root = ET.fromstring(wrapped)
    acc: list[dict] = []
    collect_paths(root, {"fill": "none", "stroke": "currentColor", "stroke-width": "1.5"}, acc)
    paths = "\n".join(path_xml(p) for p in acc)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{width}"
    android:viewportHeight="{height}">
{paths}
</vector>
"""


def main() -> None:
    data = json.loads(SOLAR_JSON.read_text())
    missing = []
    written = 0
    for xml_name in sorted(DRAWABLE.glob("*.xml")):
        if xml_name.name in SKIP:
            continue
        stem = xml_name.stem
        solar = MAP.get(stem)
        if not solar:
            missing.append(stem)
            continue
        key = solar if solar.endswith("-broken") else f"{solar}-broken"
        icon = resolve_icon(data, key)
        if not icon:
            missing.append(f"{stem}->{key}")
            continue
        body = icon["body"]
        w = icon.get("width") or data.get("width") or 24
        h = icon.get("height") or data.get("height") or 24
        xml_name.write_text(to_vector(body, int(w), int(h)), encoding="utf-8")
        written += 1
    print(f"wrote {written}")
    print("missing", missing)


if __name__ == "__main__":
    main()
