from __future__ import annotations

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[3]
PUBLIC_ASSETS = ROOT / "apps" / "website" / "public" / "assets"
BRAND = ROOT / "brand" / "generated"

BACKGROUND = (247, 250, 248)
PRIMARY = (14, 90, 122)
WHITE = (255, 255, 255)


def crop_alpha(image: Image.Image) -> Image.Image:
    box = image.getbbox()
    if box is None:
        raise ValueError("Image has no visible alpha content.")
    return image.crop(box)


def resize_width(image: Image.Image, width: int) -> Image.Image:
    ratio = width / image.width
    height = max(1, round(image.height * ratio))
    return image.resize((width, height), Image.Resampling.LANCZOS)


def fit_square(
    image: Image.Image,
    size: int,
    padding: float,
    background: tuple[int, int, int] | None = None,
) -> Image.Image:
    mode = "RGB" if background else "RGBA"
    fill = background if background else (0, 0, 0, 0)
    canvas = Image.new(mode, (size, size), fill)
    max_size = int(size * (1 - padding * 2))
    scale = min(max_size / image.width, max_size / image.height)
    resized = image.resize(
        (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
        Image.Resampling.LANCZOS,
    )
    left = (size - resized.width) // 2
    top = (size - resized.height) // 2
    if background:
        canvas.paste(resized, (left, top), resized)
    else:
        canvas.alpha_composite(resized, (left, top))
    return canvas


def tint_alpha(image: Image.Image, rgb: tuple[int, int, int]) -> Image.Image:
    output = Image.new("RGBA", image.size, (*rgb, 0))
    output.putalpha(image.getchannel("A"))
    return output


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def main() -> None:
    lockup = crop_alpha(Image.open(BRAND / "bettamind-lockup-transparent.png").convert("RGBA"))
    mark = crop_alpha(Image.open(BRAND / "bettamind-mark-transparent.png").convert("RGBA"))
    store = Image.open(BRAND / "bettamind-store-master.png").convert("RGBA")

    save_png(resize_width(lockup, 720), PUBLIC_ASSETS / "bettamind-logo-web.png")
    save_png(tint_alpha(resize_width(lockup, 720), WHITE), PUBLIC_ASSETS / "bettamind-logo-white-web.png")
    save_png(fit_square(mark, 512, 0.06), PUBLIC_ASSETS / "bettamind-mark-web.png")
    save_png(fit_square(tint_alpha(mark, WHITE), 512, 0.06), PUBLIC_ASSETS / "bettamind-mark-white-web.png")
    save_png(fit_square(store, 256, 0.0, BACKGROUND), PUBLIC_ASSETS / "bettamind-app-icon-web.png")

    og = Image.new("RGB", (1200, 630), BACKGROUND)
    mark_background = fit_square(tint_alpha(mark, PRIMARY), 760, 0.04)
    mark_background.putalpha(mark_background.getchannel("A").point(lambda alpha: int(alpha * 0.08)))
    og.paste(mark_background, (575, -40), mark_background)
    og_lockup = resize_width(lockup, 650)
    left = 88
    top = (630 - og_lockup.height) // 2
    og.paste(og_lockup, (left, top), og_lockup)
    save_png(og, PUBLIC_ASSETS / "bettamind-og.png")


if __name__ == "__main__":
    main()
