from __future__ import annotations

import colorsys
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "brand" / "source" / "bettamind-logo-master.png"

BRAND_GENERATED = ROOT / "brand" / "generated"
COMPOSE_DRAWABLE = ROOT / "shared" / "src" / "commonMain" / "composeResources" / "drawable"
ANDROID_RES = ROOT / "androidApp" / "src" / "main" / "res"
IOS_ASSETS = ROOT / "iosApp" / "iosApp" / "Assets.xcassets"

PALETTE = {
    "primary": (14, 90, 122),
    "secondary": (47, 125, 87),
    "accent": (166, 200, 58),
    "background": (247, 250, 248),
    "dark_background": (15, 20, 18),
    "on_primary": (255, 255, 255),
    "high_contrast": (0, 50, 74),
}


def is_background_pixel(r: int, g: int, b: int) -> bool:
    _, saturation, value = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return value > 0.80 and saturation < 0.12


def source_with_alpha() -> Image.Image:
    image = Image.open(SOURCE).convert("RGBA")
    output = Image.new("RGBA", image.size, (0, 0, 0, 0))
    source_pixels = image.load()
    output_pixels = output.load()

    for y in range(image.height):
        for x in range(image.width):
            r, g, b, _ = source_pixels[x, y]
            if is_background_pixel(r, g, b):
                continue
            output_pixels[x, y] = (r, g, b, 255)

    return output


def crop_alpha(image: Image.Image) -> Image.Image:
    bbox = image.getbbox()
    if bbox is None:
        raise ValueError("The source logo did not produce any visible pixels.")
    return image.crop(bbox)


def split_logo(image: Image.Image) -> tuple[Image.Image, Image.Image]:
    full = crop_alpha(image)
    mark = crop_alpha(image.crop((0, 0, image.width, 870)))
    return full, mark


def tint(image: Image.Image, rgb: tuple[int, int, int]) -> Image.Image:
    tinted = Image.new("RGBA", image.size, (*rgb, 0))
    alpha = image.getchannel("A")
    tinted.putalpha(alpha)
    return tinted


def fit(
    image: Image.Image,
    size: int,
    padding_ratio: float,
    background: tuple[int, int, int] | None = None,
) -> Image.Image:
    mode = "RGB" if background else "RGBA"
    fill = background if background else (0, 0, 0, 0)
    canvas = Image.new(mode, (size, size), fill)
    max_size = int(size * (1 - padding_ratio * 2))
    scale = min(max_size / image.width, max_size / image.height)
    resized = image.resize(
        (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
        Image.Resampling.LANCZOS,
    )
    left = (size - resized.width) // 2
    top = (size - resized.height) // 2
    if mode == "RGB":
        canvas.paste(resized, (left, top), resized)
    else:
        canvas.alpha_composite(resized, (left, top))
    return canvas


def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def generate_android(mark: Image.Image) -> None:
    save(fit(mark, 432, 0.24), ANDROID_RES / "drawable-nodpi" / "ic_launcher_foreground.png")
    save(fit(tint(mark, PALETTE["primary"]), 432, 0.24), ANDROID_RES / "drawable-nodpi" / "ic_launcher_monochrome.png")

    notification_sizes = {
        "mdpi": 24,
        "hdpi": 36,
        "xhdpi": 48,
        "xxhdpi": 72,
        "xxxhdpi": 96,
    }
    notification = tint(mark, PALETTE["on_primary"])
    for density, size in notification_sizes.items():
        save(fit(notification, size, 0.12), ANDROID_RES / f"drawable-{density}" / "ic_notification.png")

    write_text(
        ANDROID_RES / "mipmap-anydpi-v26" / "ic_launcher.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:color="@color/bettamind_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
""",
    )
    write_text(
        ANDROID_RES / "mipmap-anydpi-v26" / "ic_launcher_round.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:color="@color/bettamind_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
""",
    )
    write_text(
        ANDROID_RES / "values" / "colors.xml",
        """<resources>
    <color name="bettamind_launcher_background">#F7FAF8</color>
</resources>
""",
    )


def generate_ios(mark: Image.Image, full: Image.Image) -> None:
    background = PALETTE["background"]
    appicon = IOS_ASSETS / "AppIcon.appiconset"
    entries = [
        ("iphone", "20x20", "2x", 40),
        ("iphone", "20x20", "3x", 60),
        ("iphone", "29x29", "2x", 58),
        ("iphone", "29x29", "3x", 87),
        ("iphone", "40x40", "2x", 80),
        ("iphone", "40x40", "3x", 120),
        ("iphone", "60x60", "2x", 120),
        ("iphone", "60x60", "3x", 180),
        ("ipad", "20x20", "1x", 20),
        ("ipad", "20x20", "2x", 40),
        ("ipad", "29x29", "1x", 29),
        ("ipad", "29x29", "2x", 58),
        ("ipad", "40x40", "1x", 40),
        ("ipad", "40x40", "2x", 80),
        ("ipad", "76x76", "1x", 76),
        ("ipad", "76x76", "2x", 152),
        ("ipad", "83.5x83.5", "2x", 167),
        ("ios-marketing", "1024x1024", "1x", 1024),
    ]
    images = []
    for idiom, logical_size, scale, pixels in entries:
        filename = f"bettamind-appicon-{idiom}-{logical_size.replace('.', '_')}@{scale}.png"
        save(fit(mark, pixels, 0.18, background), appicon / filename)
        images.append(
            {
                "idiom": idiom,
                "size": logical_size,
                "scale": scale,
                "filename": filename,
            }
        )

    save_asset_image_set(IOS_ASSETS / "BettamindMark.imageset", "bettamind-mark", mark)
    save_asset_image_set(IOS_ASSETS / "BettamindLockup.imageset", "bettamind-lockup", full)

    write_text(
        appicon / "Contents.json",
        json.dumps({"images": images, "info": {"author": "xcode", "version": 1}}, indent=2) + "\n",
    )


def save_asset_image_set(folder: Path, name: str, image: Image.Image) -> None:
    images = []
    for scale, pixels in [("1x", 256), ("2x", 512), ("3x", 768)]:
        filename = f"{name}@{scale}.png"
        save(fit(image, pixels, 0.10), folder / filename)
        images.append({"idiom": "universal", "scale": scale, "filename": filename})
    write_text(folder / "Contents.json", json.dumps({"images": images, "info": {"author": "xcode", "version": 1}}, indent=2) + "\n")


def generate_shared_drawables(mark: Image.Image, full: Image.Image) -> None:
    save(fit(mark, 384, 0.10), COMPOSE_DRAWABLE / "bettamind_mark.png")
    save(fit(full, 720, 0.08), COMPOSE_DRAWABLE / "bettamind_lockup.png")


def generate_brand_masters(mark: Image.Image, full: Image.Image) -> None:
    save(fit(full, 1600, 0.08), BRAND_GENERATED / "bettamind-lockup-transparent.png")
    save(fit(mark, 1024, 0.10), BRAND_GENERATED / "bettamind-mark-transparent.png")
    save(fit(tint(mark, PALETTE["primary"]), 1024, 0.10), BRAND_GENERATED / "bettamind-mark-primary.png")
    save(fit(tint(mark, PALETTE["on_primary"]), 1024, 0.10), BRAND_GENERATED / "bettamind-mark-white.png")
    save(fit(mark, 1024, 0.18, PALETTE["background"]), BRAND_GENERATED / "bettamind-store-master.png")


def main() -> None:
    logo = source_with_alpha()
    full, mark = split_logo(logo)
    generate_brand_masters(mark, full)
    generate_shared_drawables(mark, full)
    generate_android(mark)
    generate_ios(mark, full)


if __name__ == "__main__":
    main()
