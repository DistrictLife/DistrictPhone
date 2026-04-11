"""
Minimal SVG -> PNG converter using only Pillow (no Cairo/native deps).
Supports the subset used by district-phone icons:
  - <svg width=N height=N viewBox="0 0 N N">
  - <rect width rx ry fill>
  - <circle cx cy r fill>
  - <text x y font-size fill text-anchor>LABEL</text>
  - <line>, <path> are ignored (not needed for our icons)

Usage:
    python scripts/svg_to_png.py <src_dir> <out_dir> [--size 32]
"""

import argparse
import glob
import os
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

NS = "http://www.w3.org/2000/svg"


def parse_color(value):
    """Convert SVG color string to RGBA tuple."""
    if value is None or value == "none":
        return None
    value = value.strip()
    if value.startswith("#"):
        h = value[1:]
        if len(h) == 3:
            h = h[0]*2 + h[1]*2 + h[2]*2
        r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
        return (r, g, b, 255)
    named = {
        "white": (255, 255, 255, 255), "black": (0, 0, 0, 255),
        "red": (255, 0, 0, 255), "green": (0, 128, 0, 255),
        "blue": (0, 0, 255, 255), "transparent": (0, 0, 0, 0),
    }
    return named.get(value, (128, 128, 128, 255))


def render_svg(svg_path, out_path, size):
    tree = ET.parse(svg_path)
    root = tree.getroot()

    # Source dimensions from viewBox or width/height
    vb = root.get("viewBox", "0 0 32 32").split()
    src_w, src_h = float(vb[2]), float(vb[3])
    scale_x = size / src_w
    scale_y = size / src_h

    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    def sx(v): return float(v) * scale_x
    def sy(v): return float(v) * scale_y

    tag_rect  = f"{{{NS}}}rect"  if root.tag.startswith("{") else "rect"
    tag_text  = f"{{{NS}}}text"  if root.tag.startswith("{") else "text"
    tag_circle= f"{{{NS}}}circle" if root.tag.startswith("{") else "circle"

    for elem in root.iter():
        local = elem.tag.split("}")[-1] if "}" in elem.tag else elem.tag

        if local == "rect":
            x  = sx(elem.get("x", "0"))
            y  = sy(elem.get("y", "0"))
            w  = sx(elem.get("width", "0"))
            h  = sy(elem.get("height", "0"))
            rx = sx(elem.get("rx", elem.get("ry", "0")))
            fill   = parse_color(elem.get("fill"))
            stroke = parse_color(elem.get("stroke"))
            if fill:
                draw.rounded_rectangle(
                    [x, y, x + w - 1, y + h - 1],
                    radius=rx, fill=fill,
                    outline=stroke
                )

        elif local == "circle":
            cx = sx(elem.get("cx", "0"))
            cy = sy(elem.get("cy", "0"))
            r  = sx(elem.get("r",  "0"))
            fill = parse_color(elem.get("fill"))
            if fill:
                draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)

        elif local == "text":
            x         = sx(elem.get("x", "0"))
            y         = sy(elem.get("y", "0"))
            font_size = int(float(elem.get("font-size", "10")) * min(scale_x, scale_y))
            fill      = parse_color(elem.get("fill", "black"))
            anchor    = elem.get("text-anchor", "start")  # start | middle | end
            text      = (elem.text or "").strip()
            if not text or not fill:
                continue
            try:
                font = ImageFont.truetype("arial.ttf", font_size)
            except IOError:
                font = ImageFont.load_default()
            # Bounding box for alignment
            bbox = draw.textbbox((0, 0), text, font=font)
            tw = bbox[2] - bbox[0]
            th = bbox[3] - bbox[1]
            if anchor == "middle":
                x -= tw / 2
            elif anchor == "end":
                x -= tw
            y -= th  # baseline -> top-left
            draw.text((x, y), text, fill=fill, font=font)

    img.save(out_path, "PNG")


def main():
    parser = argparse.ArgumentParser(description="Convert SVG icons to PNG using Pillow")
    parser.add_argument("src_dir", help="Source directory containing .svg files")
    parser.add_argument("out_dir", help="Output directory for .png files")
    parser.add_argument("--size", type=int, default=32, help="Output size in pixels (default 32)")
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    svgs = glob.glob(os.path.join(args.src_dir, "*.svg"))

    if not svgs:
        print(f"No SVG files found in {args.src_dir}")
        return

    for svg_path in svgs:
        name = os.path.splitext(os.path.basename(svg_path))[0]
        out_path = os.path.join(args.out_dir, name + ".png")
        try:
            render_svg(svg_path, out_path, args.size)
            print(f"OK  {name}.svg -> {name}.png")
        except Exception as e:
            print(f"ERR {name}.svg : {e}")


if __name__ == "__main__":
    main()
