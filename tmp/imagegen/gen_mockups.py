import math, os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# --- Constants ---
W, H = 1024, 1536  # 3x scale of 390x844
S = W / 390.0      # scale factor ~2.626
BG_TOP = (7, 17, 30)      # #07111E
BG_BOT = (13, 27, 42)     # #0D1B2A
PRIMARY = (10, 132, 255)   # #0A84FF
DESTRUCT = (255, 108, 124) # #FF6C7C
SUCCESS = (66, 216, 160)   # #42D8A0
WHITE = (255, 255, 255)
MUTED_GRAY = (107, 114, 128) # #6B7280
LIGHT_GRAY = (160, 168, 180) # #A0A8B4
GLASS_BG = (255, 255, 255, 15)   # rgba ~6%
GLASS_BORDER = (255, 255, 255, 20) # rgba ~8%
GLASS_LIGHT = (255, 255, 255, 8)
CARD_BG = (20, 30, 50, 180)
DARK_SURFACE = (42, 47, 58)  # #2A2F3A
MUTED_BLUE = (91, 155, 213)  # #5B9BD5

OUT = r"D:\Users\Downloads\develop\slidecleaner\output\imagegen"
os.makedirs(OUT, exist_ok=True)

def sc(v):
    """Scale a value."""
    return int(v * S)

def find_font(size):
    """Try to find a CJK-capable font."""
    candidates = [
        r"C:\Windows\Fonts\msyh.ttc",      # Microsoft YaHei
        r"C:\Windows\Fonts\msyhbd.ttc",     # YaHei Bold
        r"C:\Windows\Fonts\simhei.ttf",     # SimHei
        r"C:\Windows\Fonts\segoeui.ttf",    # Segoe UI
        r"C:\Windows\Fonts\arial.ttf",
    ]
    for f in candidates:
        if os.path.exists(f):
            try:
                return ImageFont.truetype(f, size)
            except:
                pass
    return ImageFont.load_default()

def find_font_bold(size):
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc",
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\arialbd.ttf",
    ]
    for f in candidates:
        if os.path.exists(f):
            try:
                return ImageFont.truetype(f, size)
            except:
                pass
    return find_font(size)

def gradient_bg(w, h):
    """Create a gradient background."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        t = y / h
        r = int(BG_TOP[0] + (BG_BOT[0] - BG_TOP[0]) * t)
        g = int(BG_TOP[1] + (BG_BOT[1] - BG_TOP[1]) * t)
        b = int(BG_TOP[2] + (BG_BOT[2] - BG_TOP[2]) * t)
        for x in range(w):
            img.putpixel((x, y), (r, g, b, 255))
    return img

def gradient_bg_fast(w, h):
    """Fast gradient using line drawing."""
    img = Image.new("RGBA", (w, h), (0,0,0,255))
    draw = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        r = int(BG_TOP[0] + (BG_BOT[0] - BG_TOP[0]) * t)
        g = int(BG_TOP[1] + (BG_BOT[1] - BG_TOP[1]) * t)
        b = int(BG_TOP[2] + (BG_BOT[2] - BG_TOP[2]) * t)
        draw.line([(0, y), (w, y)], fill=(r, g, b, 255))
    return img

def draw_rounded_rect(draw, xy, radius, fill=None, outline=None, width=1):
    """Draw a rounded rectangle."""
    x1, y1, x2, y2 = xy
    if fill:
        draw.rounded_rectangle(xy, radius=radius, fill=fill)
    if outline:
        draw.rounded_rectangle(xy, radius=radius, outline=outline, width=width)

def draw_glass_card(img, xy, radius=sc(32)):
    """Draw a glassmorphism card (overlay on img)."""
    overlay = Image.new("RGBA", img.size, (0,0,0,0))
    od = ImageDraw.Draw(overlay)
    x1, y1, x2, y2 = xy
    # Card background
    od.rounded_rectangle(xy, radius=radius, fill=(20, 30, 50, 140))
    # Inner highlight at top
    od.rounded_rectangle((x1, y1, x2, y1 + sc(60)), radius=radius, fill=(255, 255, 255, 10))
    # Border
    od.rounded_rectangle(xy, radius=radius, outline=(255, 255, 255, 22), width=1)
    return Image.alpha_composite(img, overlay)

def draw_status_bar(draw, img):
    """Draw Android status bar."""
    font = find_font(sc(13))
    draw.text((sc(30), sc(14)), "12:36", fill=WHITE, font=font)
    # Battery, wifi, signal icons as simple shapes
    bx = W - sc(90)
    by = sc(16)
    # Battery
    draw.rounded_rectangle((bx, by, bx+sc(22), by+sc(12)), radius=2, outline=WHITE, width=1)
    draw.rectangle((bx+sc(22), by+sc(3), bx+sc(25), by+sc(9)), fill=WHITE)
    # Signal bars
    sx = bx - sc(35)
    for i in range(4):
        bh = sc(4 + i * 2)
        draw.rectangle((sx + i*sc(6), by + sc(12) - bh, sx + i*sc(6) + sc(4), by + sc(12)), fill=WHITE)
    # WiFi (simple arc approximation)
    wx = bx - sc(65)
    draw.arc((wx, by-sc(4), wx+sc(16), by+sc(14)), 210, 330, fill=WHITE, width=1)

def draw_pill_button(draw, xy, text, fill_color, text_color=WHITE, font=None, icon_text=None):
    """Draw a pill-shaped button."""
    x1, y1, x2, y2 = xy
    draw.rounded_rectangle(xy, radius=sc(18), fill=fill_color)
    if font is None:
        font = find_font_bold(sc(16))
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = x1 + (x2 - x1 - tw) // 2
    ty = y1 + (y2 - y1 - th) // 2 - sc(2)
    if icon_text:
        icon_font = find_font(sc(14))
        icon_w = icon_font.getbbox(icon_text)[2]
        total = icon_w + sc(6) + tw
        sx = x1 + (x2 - x1 - total) // 2
        draw.text((sx, ty), icon_text, fill=text_color, font=icon_font)
        draw.text((sx + icon_w + sc(6), ty), text, fill=text_color, font=font)
    else:
        draw.text((tx, ty), text, fill=text_color, font=font)

def center_text(draw, y, text, fill, font, width=W):
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    x = (width - tw) // 2
    draw.text((x, y), text, fill=fill, font=font)

def draw_divider(draw, x1, y, x2):
    draw.line([(x1, y), (x2, y)], fill=(255, 255, 255, 25), width=1)


# ============================================================
# IMAGE 1: Batch Complete Page
# ============================================================
def gen_batch_complete():
    img = gradient_bg_fast(W, H)
    draw = ImageDraw.Draw(img)
    
    # Status bar
    draw_status_bar(draw, img)
    
    # Add subtle glow in center
    glow = Image.new("RGBA", (W, H), (0,0,0,0))
    gd = ImageDraw.Draw(glow)
    cx, cy = W//2, H//2 - sc(40)
    for r in range(sc(300), 0, -2):
        alpha = int(8 * (r / sc(300)))
        gd.ellipse((cx-r, cy-r, cx+r, cy+r), fill=(10, 60, 140, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(sc(40)))
    img = Image.alpha_composite(img, glow)
    
    # Glass card
    card_x1 = sc(28)
    card_y1 = H//2 - sc(200)
    card_x2 = W - sc(28)
    card_y2 = H//2 + sc(200)
    img = draw_glass_card(img, (card_x1, card_y1, card_x2, card_y2))
    draw = ImageDraw.Draw(img)
    
    y = card_y1 + sc(36)
    
    # SWIPE SEQUENCE COMPLETE
    font_label = find_font(sc(11))
    center_text(draw, y, "SWIPE SEQUENCE COMPLETE", MUTED_BLUE, font_label)
    y += sc(32)
    
    # 本轮清理完成
    font_heading = find_font_bold(sc(28))
    center_text(draw, y, "本轮清理完成", WHITE, font_heading)
    y += sc(52)
    
    # Divider
    draw_divider(draw, card_x1 + sc(24), y, card_x2 - sc(24))
    y += sc(20)
    
    # Stats row
    font_stats = find_font(sc(13))
    center_text(draw, y, "共 50 张 · 保留 38 张 · 删除队列 12 张", LIGHT_GRAY, font_stats)
    y += sc(36)
    
    # Divider
    draw_divider(draw, card_x1 + sc(24), y, card_x2 - sc(24))
    y += sc(28)
    
    # Red button: 确认删除 (12 张)
    btn_x1 = card_x1 + sc(24)
    btn_x2 = card_x2 - sc(24)
    btn_h = sc(50)
    draw_pill_button(draw, (btn_x1, y, btn_x2, y + btn_h),
                     "确认删除 (12 张)", DESTRUCT, WHITE, find_font_bold(sc(16)), "🗑")
    y += btn_h + sc(14)
    
    # Blue button: 下一批
    draw_pill_button(draw, (btn_x1, y, btn_x2, y + btn_h),
                     "下一批", PRIMARY, WHITE, find_font_bold(sc(16)), "🔀")
    y += btn_h + sc(24)
    
    # 返回首页 text button
    font_sub = find_font(sc(14))
    center_text(draw, y, "返回首页", MUTED_GRAY, font_sub)
    
    img = img.convert("RGB")
    img.save(os.path.join(OUT, "mockup_1_batch_complete.png"), quality=95)
    print("Saved mockup_1_batch_complete.png")


# ============================================================
# IMAGE 2: Swipe Cleanup Page
# ============================================================
def gen_swipe_cleanup():
    img = gradient_bg_fast(W, H)
    draw = ImageDraw.Draw(img)
    
    # Status bar
    draw_status_bar(draw, img)
    
    y = sc(50)
    
    # Top app bar
    font_title = find_font_bold(sc(20))
    draw.text((sc(24), y), "随机清理", fill=WHITE, font=font_title)
    y += sc(32)
    font_sub = find_font(sc(12))
    draw.text((sc(24), y), "已整理 120 项 · 随机 50 张", fill=MUTED_GRAY, font=font_sub)
    y += sc(36)
    
    # Status glass card
    sc_x1 = sc(16)
    sc_y1 = y
    sc_x2 = W - sc(16)
    sc_y2 = y + sc(70)
    img = draw_glass_card(img, (sc_x1, sc_y1, sc_x2, sc_y2), radius=sc(24))
    draw = ImageDraw.Draw(img)
    
    # "RANDOM BOARD" label
    font_sm = find_font(sc(10))
    draw.text((sc(36), sc_y1 + sc(16)), "RANDOM BOARD", fill=MUTED_BLUE, font=font_sm)
    
    # "第 15 / 50 张" right aligned
    font_mid = find_font(sc(14))
    progress_text = "第 15 / 50 张"
    bbox = font_mid.getbbox(progress_text)
    draw.text((sc_x2 - sc(36) - (bbox[2]-bbox[0]), sc_y1 + sc(14)), progress_text, fill=WHITE, font=font_mid)
    
    # Progress bar
    pb_y = sc_y1 + sc(48)
    pb_x1 = sc(36)
    pb_x2 = sc_x2 - sc(36)
    pb_h = sc(6)
    # Track
    draw.rounded_rectangle((pb_x1, pb_y, pb_x2, pb_y + pb_h), radius=sc(3), fill=(255,255,255,25))
    # Fill ~30%
    pb_fill = pb_x1 + int((pb_x2 - pb_x1) * 0.30)
    draw.rounded_rectangle((pb_x1, pb_y, pb_fill, pb_y + pb_h), radius=sc(3), fill=PRIMARY)
    
    y = sc_y2 + sc(20)
    
    # Large photo card
    ph_x1 = sc(16)
    ph_y1 = y
    ph_x2 = W - sc(16)
    ph_y2 = y + sc(720)
    ph_radius = sc(20)
    
    # Create placeholder landscape photo
    photo = Image.new("RGBA", (ph_x2 - ph_x1, ph_y2 - ph_y1), (0,0,0,255))
    pd = ImageDraw.Draw(photo)
    pw, ph = photo.size
    
    # Sky gradient
    for py in range(ph // 2):
        t = py / (ph // 2)
        r = int(30 + 80 * t)
        g = int(50 + 60 * t)
        b = int(120 - 40 * t)
        pd.line([(0, py), (pw, py)], fill=(r, g, b))
    
    # Sunset colors
    for py in range(ph // 2, ph // 2 + ph // 6):
        t = (py - ph//2) / (ph // 6)
        r = int(220 - 60 * t)
        g = int(120 - 40 * t)
        b = int(60 + 20 * t)
        pd.line([(0, py), (pw, py)], fill=(r, g, b))
    
    # Mountains
    points = [(0, ph//2 + ph//8)]
    for mx in range(0, pw + 10, 10):
        my = ph//2 + ph//8 - int(80 * math.sin(mx * 0.008) * math.sin(mx * 0.003 + 1) - 20 * math.sin(mx * 0.02))
        points.append((mx, my))
    points.append((pw, ph))
    points.append((0, ph))
    pd.polygon(points, fill=(20, 40, 60))
    
    # Water
    for py in range(ph * 2 // 3, ph):
        t = (py - ph * 2 // 3) / (ph // 3)
        r = int(15 + 10 * t)
        g = int(30 + 10 * t)
        b = int(60 + 10 * t)
        pd.line([(0, py), (pw, py)], fill=(r, g, b))
    
    # Sun
    sun_x, sun_y = pw // 2, ph // 2 - sc(20)
    sun_r = sc(30)
    for sr in range(sun_r, 0, -1):
        a = int(255 * (sr / sun_r))
        pd.ellipse((sun_x - sr, sun_y - sr, sun_x + sr, sun_y + sr), fill=(255, 200, 100, a))
    
    # Apply rounded corners mask
    mask = Image.new("L", photo.size, 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, pw, ph), radius=ph_radius, fill=255)
    photo.putalpha(mask)
    
    img.paste(photo, (ph_x1, ph_y1), photo)
    draw = ImageDraw.Draw(img)
    
    # PHOTO badge top-left
    badge_y = ph_y1 + sc(14)
    badge_x = ph_x1 + sc(14)
    badge_font = find_font_bold(sc(10))
    draw.rounded_rectangle((badge_x, badge_y, badge_x + sc(56), badge_y + sc(24)), 
                           radius=sc(12), fill=(0,0,0,160))
    draw.text((badge_x + sc(10), badge_y + sc(4)), "PHOTO", fill=WHITE, font=badge_font)
    
    # Green checkmark badge top-right
    cb_x = ph_x2 - sc(38)
    cb_y = ph_y1 + sc(14)
    draw.ellipse((cb_x, cb_y, cb_x + sc(28), cb_y + sc(28)), fill=SUCCESS)
    check_font = find_font(sc(14))
    draw.text((cb_x + sc(6), cb_y + sc(3)), "✓", fill=WHITE, font=check_font)
    
    y = ph_y2 + sc(16)
    
    # Swipe hint
    font_hint = find_font(sc(13))
    center_text(draw, y, "↑", MUTED_GRAY, find_font(sc(20)))
    y += sc(28)
    center_text(draw, y, "上滑加入删除队列", MUTED_GRAY, font_hint)
    y += sc(36)
    
    # Bottom bar
    bb_y1 = H - sc(90)
    bb_y2 = H - sc(16)
    bb_x1 = sc(16)
    bb_x2 = W - sc(16)
    img = draw_glass_card(img, (bb_x1, bb_y1, bb_x2, bb_y2), radius=sc(24))
    draw = ImageDraw.Draw(img)
    
    # DELETE QUEUE label
    font_bq = find_font(sc(10))
    draw.text((bb_x1 + sc(20), bb_y1 + sc(14)), "DELETE QUEUE", fill=MUTED_BLUE, font=font_bq)
    
    # Counter
    font_cnt = find_font_bold(sc(18))
    draw.text((bb_x1 + sc(20), bb_y1 + sc(36)), "5 / 50", fill=WHITE, font=font_cnt)
    
    # 确认删除 red button
    rb_x2 = bb_x2 - sc(20)
    rb_x1 = rb_x2 - sc(140)
    rb_y1 = bb_y1 + sc(18)
    rb_y2 = bb_y2 - sc(18)
    draw.rounded_rectangle((rb_x1, rb_y1, rb_x2, rb_y2), radius=sc(18), fill=DESTRUCT)
    font_btn = find_font_bold(sc(14))
    bbox = font_btn.getbbox("确认删除")
    tw = bbox[2] - bbox[0]
    draw.text((rb_x1 + (rb_x2-rb_x1-tw)//2, rb_y1 + sc(10)), "确认删除", fill=WHITE, font=font_btn)
    
    # 撤销 button
    ub_x2 = rb_x1 - sc(10)
    ub_x1 = ub_x2 - sc(80)
    draw.rounded_rectangle((ub_x1, rb_y1, ub_x2, rb_y2), radius=sc(18), outline=(255,255,255,60), width=1)
    bbox = font_btn.getbbox("撤销")
    tw = bbox[2] - bbox[0]
    draw.text((ub_x1 + (ub_x2-ub_x1-tw)//2, rb_y1 + sc(10)), "撤销", fill=MUTED_GRAY, font=font_btn)
    
    img = img.convert("RGB")
    img.save(os.path.join(OUT, "mockup_2_swipe_cleanup.png"), quality=95)
    print("Saved mockup_2_swipe_cleanup.png")


# ============================================================
# IMAGE 3: Gallery Home Page
# ============================================================
def gen_gallery_home():
    img = gradient_bg_fast(W, H)
    draw = ImageDraw.Draw(img)
    
    # Status bar
    draw_status_bar(draw, img)
    
    y = sc(50)
    
    # Top app bar
    font_title = find_font_bold(sc(22))
    draw.text((sc(24), y), "相册清理", fill=WHITE, font=font_title)
    font_sub = find_font(sc(12))
    draw.text((sc(24), y + sc(32)), "12 个月份", fill=MUTED_GRAY, font=font_sub)
    
    # Right icons (palette + delete)
    icon_font = find_font(sc(18))
    draw.text((W - sc(70), y + sc(4)), "🎨", fill=MUTED_GRAY, font=icon_font)
    draw.text((W - sc(42), y + sc(4)), "🗑", fill=MUTED_GRAY, font=icon_font)
    
    y += sc(60)
    
    # Add glow
    glow = Image.new("RGBA", (W, H), (0,0,0,0))
    gd = ImageDraw.Draw(glow)
    cx, cy = W//2, y + sc(200)
    for r in range(sc(250), 0, -2):
        alpha = int(6 * (r / sc(250)))
        gd.ellipse((cx-r, cy-r, cx+r, cy+r), fill=(10, 60, 140, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(sc(40)))
    img = Image.alpha_composite(img, glow)
    
    # Hero glass card
    h_x1 = sc(16)
    h_y1 = y
    h_x2 = W - sc(16)
    h_y2 = y + sc(360)
    img = draw_glass_card(img, (h_x1, h_y1, h_x2, h_y2), radius=sc(32))
    draw = ImageDraw.Draw(img)
    
    cy = h_y1 + sc(28)
    
    # SCENE 01 · DEDE BLUE + READY badge
    font_scene = find_font(sc(10))
    draw.text((h_x1 + sc(28), cy), "SCENE 01 · DEEP BLUE", fill=MUTED_BLUE, font=font_scene)
    
    # READY badge
    rd_text = "READY"
    rd_bbox = font_scene.getbbox(rd_text)
    rd_w = rd_bbox[2] - rd_bbox[0] + sc(16)
    rd_x = h_x2 - sc(28) - rd_w
    draw.rounded_rectangle((rd_x, cy - sc(2), rd_x + rd_w, cy + sc(18)), radius=sc(9), fill=SUCCESS)
    draw.text((rd_x + sc(8), cy), rd_text, fill=(10, 30, 20), font=font_scene)
    
    cy += sc(32)
    
    # 相册清理 headline
    font_head = find_font_bold(sc(26))
    draw.text((h_x1 + sc(28), cy), "相册清理", fill=WHITE, font=font_head)
    cy += sc(44)
    
    # Description lines
    font_desc = find_font(sc(12))
    desc_lines = ["按月浏览设备媒体", "批量加入删除队列", "系统回收站保护"]
    for line in desc_lines:
        draw.text((h_x1 + sc(28), cy), line, fill=LIGHT_GRAY, font=font_desc)
        cy += sc(22)
    cy += sc(10)
    
    # Progress bar
    pb_x1 = h_x1 + sc(28)
    pb_x2 = h_x2 - sc(28)
    pb_h = sc(6)
    draw.rounded_rectangle((pb_x1, cy, pb_x2, cy + pb_h), radius=sc(3), fill=(255,255,255,20))
    draw.rounded_rectangle((pb_x1, cy, pb_x1 + int((pb_x2-pb_x1)*0.45), cy + pb_h), radius=sc(3), fill=PRIMARY)
    cy += pb_h + sc(16)
    
    # Stats row
    font_stats = find_font(sc(12))
    draw.text((h_x1 + sc(28), cy), "总项目 2,847 / 月份 12 / 月均 237", fill=LIGHT_GRAY, font=font_stats)
    cy += sc(30)
    
    # Two buttons side by side
    btn_y1 = cy
    btn_y2 = cy + sc(46)
    gap = sc(12)
    btn_w = (h_x2 - h_x1 - sc(56) - gap) // 2
    
    # 已整理 (gray)
    btn1_x1 = h_x1 + sc(28)
    draw.rounded_rectangle((btn1_x1, btn_y1, btn1_x1 + btn_w, btn_y2), radius=sc(18), fill=DARK_SURFACE)
    font_btn = find_font_bold(sc(15))
    bbox = font_btn.getbbox("已整理")
    tw = bbox[2] - bbox[0]
    draw.text((btn1_x1 + (btn_w - tw)//2, btn_y1 + sc(10)), "已整理", fill=WHITE, font=font_btn)
    
    # 随机清理 (blue)
    btn2_x1 = btn1_x1 + btn_w + gap
    draw.rounded_rectangle((btn2_x1, btn_y1, btn2_x1 + btn_w, btn_y2), radius=sc(18), fill=PRIMARY)
    bbox = font_btn.getbbox("随机清理")
    tw = bbox[2] - bbox[0]
    draw.text((btn2_x1 + (btn_w - tw)//2, btn_y1 + sc(10)), "随机清理", fill=WHITE, font=font_btn)
    
    y = h_y2 + sc(20)
    
    # Section header
    font_sec = find_font(sc(12))
    draw.text((sc(28), y), "月份", fill=MUTED_GRAY, font=font_sec)
    y += sc(28)
    
    # Month cards
    months = [
        ("2026年6月", "156 张", [(100, 160, 220), (80, 180, 140), (220, 160, 80), (160, 100, 180)]),
        ("2026年5月", "203 张", [(80, 140, 200), (120, 180, 100), (200, 140, 100), (100, 180, 200)]),
        ("2026年4月", "178 张", [(140, 100, 160), (100, 160, 120), (180, 160, 100), (120, 120, 180)]),
    ]
    
    mc_x1 = sc(16)
    mc_x2 = W - sc(16)
    mc_h = sc(100)
    mc_gap = sc(12)
    
    for i, (label, count, thumbs) in enumerate(months):
        mc_y1 = y
        mc_y2 = y + mc_h
        
        # Glass card
        img = draw_glass_card(img, (mc_x1, mc_y1, mc_x2, mc_y2), radius=sc(24))
        draw = ImageDraw.Draw(img)
        
        # Thumbnails
        thumb_size = sc(56)
        thumb_gap = sc(8)
        tx = mc_x1 + sc(20)
        ty = mc_y1 + sc(22)
        for j, color in enumerate(thumbs):
            tx1 = tx + j * (thumb_size + thumb_gap)
            draw.rounded_rectangle((tx1, ty, tx1 + thumb_size, ty + thumb_size), 
                                   radius=sc(12), fill=color)
        
        # Month label
        text_x = mc_x1 + sc(20) + 4 * (thumb_size + thumb_gap) + sc(16)
        font_month = find_font_bold(sc(15))
        draw.text((text_x, mc_y1 + sc(28)), label, fill=WHITE, font=font_month)
        
        # Count
        font_count = find_font(sc(12))
        draw.text((text_x, mc_y1 + sc(56)), count, fill=MUTED_GRAY, font=font_count)
        
        y = mc_y2 + mc_gap
    
    img = img.convert("RGB")
    img.save(os.path.join(OUT, "mockup_3_gallery_home.png"), quality=95)
    print("Saved mockup_3_gallery_home.png")


# Run all
gen_batch_complete()
gen_swipe_cleanup()
gen_gallery_home()
print("All 3 mockups generated!")
