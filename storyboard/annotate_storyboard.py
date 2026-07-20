# -*- coding: utf-8 -*-
import os, math
from PIL import Image, ImageDraw, ImageFont

DIR = r"C:\Users\sergi\OneDrive\Documentos\Reciclame\storyboard"
OUT = os.path.join(DIR, "annotated")
os.makedirs(OUT, exist_ok=True)

ACCENT_SOLID = (255, 92, 53)
NAVY = (27, 58, 92)
WHITE = (255, 255, 255)

def load_font(size, bold=True):
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for c in candidates:
        if os.path.exists(c):
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()

def draw_circle(draw, box, width=7):
    draw.ellipse(box, outline=ACCENT_SOLID, width=width)

def draw_badge(img, draw, pos, number, font):
    x, y = pos
    r = 22
    odraw = ImageDraw.Draw(img)
    odraw.ellipse([x-r, y-r, x+r, y+r], fill=ACCENT_SOLID)
    bbox = draw.textbbox((0, 0), str(number), font=font)
    tw, th = bbox[2]-bbox[0], bbox[3]-bbox[1]
    odraw.text((x-tw/2, y-th/2-bbox[1]), str(number), font=font, fill=WHITE)

def draw_label(img, draw, pos, text, font):
    x, y = pos
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2]-bbox[0], bbox[3]-bbox[1]
    pad = 12
    box = [x-pad, y-pad, x+tw+pad, y+th+pad]
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    odraw = ImageDraw.Draw(overlay)
    odraw.rounded_rectangle(box, radius=10, fill=(27, 58, 92, 235))
    img.alpha_composite(overlay)
    draw = ImageDraw.Draw(img)
    draw.text((x, y-2), text, font=font, fill=WHITE)

def annotate(name, shapes, font_size=30):
    src = os.path.join(DIR, f"{name}.webp")
    img = Image.open(src).convert("RGBA")
    draw = ImageDraw.Draw(img)
    font = load_font(font_size)
    for s in shapes:
        if s[0] == "circle":
            draw_circle(draw, s[1], width=s[2] if len(s) > 2 else 7)
        elif s[0] == "badge":
            draw_badge(img, draw, s[1], s[2], font)
        elif s[0] == "label":
            draw_label(img, draw, s[1], s[2], font)
            draw = ImageDraw.Draw(img)
    img.convert("RGB").save(os.path.join(OUT, f"{name}.png"), quality=95)
    print("saved", name)

W, H = 1917, 862   # login images
Wd, Hd = 1900, 870  # dashboard

# --- PASO 1: login_vacio -> 3 campos señalados con numeritos ---
annotate("login_vacio", [
    ("circle", (57, 302, 748, 353), 6),
    ("badge", (35, 327), "1"),
    ("circle", (57, 397, 748, 448), 6),
    ("badge", (35, 422), "2"),
    ("circle", (57, 569, 748, 638), 6),
    ("badge", (35, 603), "3"),
    ("label", (800, 780), "1. Correo corporativo   2. Contraseña   3. Iniciar sesión"),
])

# --- PASO 2: login_completado -> foco solo en el botón (CTA) ---
annotate("login_completado", [
    ("circle", (57, 569, 748, 638), 8),
    ("label", (800, 780), "Datos verificados: un clic para entrar"),
])

# --- PASO 3: dashboard_admin -> resultado, bienvenida + sidebar ---
annotate("dashboard_admin", [
    ("circle", (335, 120, 1090, 195), 6),
    ("circle", (1690, 15, 1860, 65), 5),
    ("label", (335, 220), "Acceso concedido: panel según tu rol"),
])

print("DONE")
