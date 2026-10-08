package dev.lumina.gui;

import dev.lumina.anim.Anim;
import dev.lumina.anim.Motion;
import dev.lumina.client.LuminaKeys;
import dev.lumina.client.VanillaBridge;
import dev.lumina.config.ConfigManager;
import dev.lumina.config.LibraryService;
import dev.lumina.config.LibraryService.Entry;
import dev.lumina.config.LibraryService.Kind;
import dev.lumina.setting.BoolSetting;
import dev.lumina.setting.Category;
import dev.lumina.setting.ColorSetting;
import dev.lumina.setting.EnumSetting;
import dev.lumina.setting.NumberSetting;
import dev.lumina.setting.Setting;
import dev.lumina.setting.Settings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Lumina settings menu. Everything is drawn in "immediate mode": each frame the layout is rebuilt
 * and clickable regions are collected in {@link #hits}. Animations live in {@link Anim} objects.
 */
public final class LuminaScreen extends Screen {
    private static final int PW = 620;
    private static final int PH = 384;
    private static final int SIDE_W = 142;
    private static final int[] PALETTE = {
            0xF0507A, 0xFF8A3D, 0xF5C542, 0x4ADE80, 0x38BDF8, 0x8B5CF6, 0xFFFFFF, 0x000000
    };

    private static Category lastPage = Category.GENERAL;

    @FunctionalInterface private interface ClickAction { void click(int button, int mx, int my); }
    @FunctionalInterface private interface ScrollAction { void scroll(double dy); }
    private record Hit(int x, int y, int w, int h, ClickAction click, ScrollAction scroll) {}

    private static final class RowState {
        final Anim hover = new Anim(0f);
        final Anim flash = new Anim(0f, 5f);
        final Anim expand = new Anim(0f, 12f);
        final Anim tog = new Anim(0f);
        final Anim fill = new Anim(0f, 20f);
        int relY;
        int h;
    }

    // ---- state
    private Category page = lastPage;
    private String group = null;
    private String query = "";
    private boolean searchFocused;
    private String nameBuf = "";
    private boolean nameFocused;
    private float openT;
    private boolean closing;
    private boolean closeNow;
    private long lastNs = System.nanoTime();
    private float scrollTarget;
    private float scrollCur;
    private float contentH;
    private int viewH = 200;
    private float curScale = 1f;
    private final Anim pageAnim = new Anim(1f, 12f);
    private final Anim[] sideHover = new Anim[Category.values().length];
    private final Anim[] sideSel = new Anim[Category.values().length];
    private final Map<Object, Anim> anims = new HashMap<>();
    private final Map<Setting, RowState> rows = new HashMap<>();
    private final Map<ColorSetting, float[]> hsbMap = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> popupHits = new ArrayList<>();
    private final List<Object> entries = new ArrayList<>();
    private final List<String> groups = new ArrayList<>();
    private List<Entry> libCache = new ArrayList<>();
    private Setting focused;
    private int clipTop;
    private int clipBot;

    private NumberSetting dragNum;
    private int dragX;
    private int dragW;
    private ColorSetting dragCol;
    private int dragCh;
    private int dragBarX;
    private int dragBarW;
    private ColorSetting openColor;
    private EnumSetting openEnum;
    private EnumSetting popEnum;
    private int popX;
    private int popY;
    private int popW;
    private final Anim dropAnim = new Anim(0f, 16f);

    public LuminaScreen() {
        super(Component.literal("Lumina"));
        for (int i = 0; i < sideHover.length; i++) {
            sideHover[i] = new Anim(0f);
            sideSel[i] = new Anim(i == lastPage.ordinal() ? 1f : 0f);
        }
        VanillaBridge.pull();
        buildEntries();
        refreshLibrary();
        pageAnim.snap(1f);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { closing = true; }

    @Override public void tick() { if (closeNow) this.minecraft.setScreen(null); }

    @Override
    public void removed() {
        lastPage = page;
        ConfigManager.saveIfDirty();
        VanillaBridge.saveVanilla();
    }

    // ------------------------------------------------------------------ data

    private RowState state(Setting s) {
        RowState st = rows.get(s);
        if (st == null) {
            st = new RowState();
            if (s instanceof BoolSetting b) st.tog.snap(b.get() ? 1f : 0f);
            if (s instanceof NumberSetting n) st.fill.snap(n.fraction());
            rows.put(s, st);
        }
        return st;
    }

    private Anim anim(Object key) {
        return anims.computeIfAbsent(key, k -> new Anim(0f));
    }

    private float hover(Object key, boolean hov, float dt) {
        Anim a = anim(key);
        a.target = hov ? 1f : 0f;
        if (!Settings.ANIM_HOVER.get()) a.snap(a.target); else a.update(dt);
        return a.value;
    }

    private boolean matches(Setting s, String q) {
        return s.name.toLowerCase(Locale.ROOT).contains(q)
                || s.desc.toLowerCase(Locale.ROOT).contains(q)
                || s.group.toLowerCase(Locale.ROOT).contains(q)
                || s.category.label.toLowerCase(Locale.ROOT).contains(q);
    }

    private void buildEntries() {
        entries.clear();
        groups.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            for (Category c : Category.values()) {
                if (c.library) continue;
                boolean header = false;
                for (Setting s : Settings.all()) {
                    if (s.category == c && matches(s, q)) {
                        if (!header) {
                            entries.add(c.label);
                            header = true;
                        }
                        entries.add(s);
                    }
                }
            }
        } else if (!page.library) {
            for (Setting s : Settings.all()) {
                if (s.category == page && !groups.contains(s.group)) groups.add(s.group);
            }
            if (group != null && !groups.contains(group)) group = null;
            String last = null;
            for (Setting s : Settings.all()) {
                if (s.category != page) continue;
                if (group != null && !s.group.equals(group)) continue;
                if (!s.group.equals(last)) {
                    entries.add(s.group);
                    last = s.group;
                }
                entries.add(s);
            }
        }
        if (focused != null && !entries.contains(focused)) focused = null;
        if (openColor != null && !entries.contains(openColor)) openColor = null;
    }

    private void refreshLibrary() {
        if (page == Category.PRESETS) libCache = LibraryService.list(Kind.PRESETS);
        else if (page == Category.THEMES) libCache = LibraryService.list(Kind.THEMES);
    }

    private Kind kind() { return page == Category.THEMES ? Kind.THEMES : Kind.PRESETS; }

    private void selectPage(Category c) {
        page = c;
        group = null;
        query = "";
        searchFocused = false;
        nameFocused = false;
        openEnum = null;
        openColor = null;
        scrollTarget = 0f;
        scrollCur = 0f;
        pageAnim.snap(0f);
        pageAnim.target = 1f;
        buildEntries();
        refreshLibrary();
    }

    private void onQueryChanged() {
        scrollTarget = 0f;
        scrollCur = 0f;
        openEnum = null;
        buildEntries();
    }

    private void flash(Setting s) {
        if (!Settings.ANIM_FLASH.get()) return;
        RowState st = state(s);
        st.flash.value = 1f;
        st.flash.target = 0f;
    }

    private void resetSetting(Setting s) {
        if (s.isDefault()) return;
        s.reset();
        flash(s);
    }

    private int rowBase() {
        if (Settings.GUI_COMPACT.get()) return 24;
        return Settings.GUI_DESC.get() ? 34 : 26;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        UiTheme.refresh();
        ConfigManager.saveDebounced();
        hits.clear();
        popupHits.clear();

        // open / close tween
        int style = Settings.ANIM_OPEN.index();
        float sp = Motion.speed();
        if (sp <= 0f || style == 3) {
            openT = closing ? 0f : 1f;
        } else {
            openT = Math.max(0f, Math.min(1f, openT + (closing ? -1f : 1f) * dt * sp / 0.2f));
        }
        if (closing && openT <= 0f) {
            closeNow = true;
            return;
        }
        float p = Motion.outCubic(openT);
        float alpha = style == 3 ? 1f : p;

        float fit = Math.min((width - 12f) / PW, (height - 12f) / PH);
        float s = Math.max(0.35f, Math.min(Settings.GUI_SCALE.getF(), fit));
        curScale = s;
        float k = s * (style == 0 ? 0.94f + 0.06f * p : 1f);
        float slide = style == 1 ? (1f - p) * 14f : 0f;

        // backdrop
        int dim = Colors.alpha(0x000000, Settings.GUI_BACKDROP.getF() * alpha);
        if ((dim >>> 24) >= 3) g.fill(0, 0, width, height, dim);

        float cxs = width / 2f;
        float cys = height / 2f;
        g.pose().pushMatrix();
        g.pose().translate(cxs, cys + slide);
        g.pose().scale(k, k);
        g.pose().translate(-cxs, -cys);
        Gfx.ga = alpha;

        double lmx = (mouseX - cxs) / s + cxs;
        double lmy = (mouseY - cys) / s + cys;
        int mx = (int) Math.round(lmx);
        int my = (int) Math.round(lmy);

        int x0 = (width - PW) / 2;
        int y0 = (height - PH) / 2;
        int r = UiTheme.radius;

        pageAnim.update(dt);

        drawPanel(g, x0, y0, r);
        drawSidebar(g, x0, y0, mx, my, dt);
        drawContent(g, x0, y0, mx, my, dt);
        drawPopup(g, x0, y0, mx, my, dt);

        Gfx.ga = alpha;
        Toasts.render(g, this.font, x0 + PW - 14, y0 + PH - 12);

        Gfx.ga = 1f;
        g.pose().popMatrix();
    }

    private void drawPanel(GuiGraphicsExtractor g, int x0, int y0, int r) {
        if (Settings.MENU_SHADOW.get()) Gfx.shadow(g, x0, y0, PW, PH, r + 2, UiTheme.shadow);
        Gfx.round(g, x0 - 1, y0 - 1, PW + 2, PH + 2, r + 1, UiTheme.border);
        Gfx.round(g, x0, y0, PW, PH, r, UiTheme.bg);
        if (Settings.GUI_GLOW.get()) {
            int top = Colors.mulAlpha(Colors.alpha(UiTheme.accent, 0.11f), Gfx.ga);
            int bottom = Colors.alpha(UiTheme.accent & 0xFFFFFF, 0f);
            g.fillGradient(x0 + 1, y0 + r, x0 + PW - 1, y0 + r + 70, top, bottom);
        }
    }

    // ---- sidebar

    private void drawSidebar(GuiGraphicsExtractor g, int x0, int y0, int mx, int my, float dt) {
        int r = UiTheme.radius;
        int sx = x0 + 10;
        int sy = y0 + 10;
        int sh = PH - 20;
        Gfx.round(g, sx, sy, SIDE_W, sh, Math.max(0, r - 2), UiTheme.panel);

        // logo + moon motif
        Gfx.text(g, font, "LUMINA", sx + 12, sy + 11, UiTheme.accent, false);
        Gfx.text(g, font, "visual overhaul", sx + 12, sy + 22, UiTheme.textDim, false);
        if (Settings.GUI_GLOW.get()) {
            int mxp = sx + SIDE_W - 20;
            int myp = sy + 19;
            disc(g, mxp, myp, 11, Colors.alpha(UiTheme.accent & 0xFFFFFF, 0.08f));
            disc(g, mxp, myp, 8, Colors.alpha(UiTheme.accent & 0xFFFFFF, 0.14f));
            disc(g, mxp, myp, 5, Colors.alpha(UiTheme.text & 0xFFFFFF, 0.85f));
        }

        // search box
        int bx = sx + 8;
        int by = sy + 40;
        int bw = SIDE_W - 16;
        int bh = 18;
        boolean hovSearch = inside(mx, my, bx, by, bw, bh);
        float hv = hover("search", hovSearch || searchFocused, dt);
        Gfx.roundOutline(g, bx, by, bw, bh, 4,
                Colors.lerp(UiTheme.border, UiTheme.accent, searchFocused ? 1f : hv * 0.5f),
                Colors.lerp(UiTheme.bg | 0xFF000000, UiTheme.panel | 0xFF000000, 0.5f));
        String shown = query.isEmpty() ? (searchFocused ? "" : "Search...") : Gfx.fit(font, query, bw - 14);
        Gfx.text(g, font, shown, bx + 6, by + 5, query.isEmpty() ? UiTheme.textDim : UiTheme.text, false);
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            Gfx.fill(g, bx + 6 + font.width(shown), by + 4, 1, 10, UiTheme.text);
        }
        addHit(bx, by, bw, bh, (b, hx, hy) -> {
            searchFocused = true;
            nameFocused = false;
        });

        // page list
        int iy = sy + 66;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            if (c == Category.PRESETS) {
                Gfx.fill(g, sx + 12, iy + 2, SIDE_W - 24, 1, UiTheme.border);
                iy += 8;
            }
            boolean sel = c == page && query.trim().isEmpty();
            boolean hov = inside(mx, my, sx + 6, iy, SIDE_W - 12, 18);
            Anim sa = sideSel[i];
            sa.target = sel ? 1f : 0f;
            sa.update(dt);
            Anim ha = sideHover[i];
            ha.target = hov ? 1f : 0f;
            if (!Settings.ANIM_HOVER.get()) ha.snap(ha.target); else ha.update(dt);
            int bg = Colors.lerp(Colors.alpha(UiTheme.text & 0xFFFFFF, 0f), UiTheme.hover, ha.value);
            bg = Colors.lerp(bg, UiTheme.accentSoft, sa.value);
            Gfx.round(g, sx + 6, iy, SIDE_W - 12, 18, 4, bg);
            Gfx.round(g, sx + 6, iy + 4, 2, 10, 1, Colors.mulAlpha(UiTheme.accent, sa.value));
            int tc = Colors.lerp(UiTheme.textDim, UiTheme.text, Math.max(ha.value, sa.value));
            Gfx.text(g, font, c.label, sx + 16 + Math.round(sa.value * 2f), iy + 5, tc, false);
            final Category target = c;
            addHit(sx + 6, iy, SIDE_W - 12, 18, (b, hx, hy) -> selectPage(target));
            iy += 20;
        }

        if (Settings.KEY_HINTS.get()) {
            Gfx.text(g, font, "Toggle: " + LuminaKeys.OPEN.getTranslatedKeyMessage().getString(),
                    sx + 12, sy + sh - 16, UiTheme.textDim, false);
        }
    }

    private void disc(GuiGraphicsExtractor g, int cx, int cy, int rad, int argb) {
        for (int dy = -rad; dy <= rad; dy++) {
            int hw = (int) Math.round(Math.sqrt(rad * (double) rad - dy * (double) dy));
            Gfx.fill(g, cx - hw, cy + dy, hw * 2, 1, argb);
        }
    }

    // ---- content

    private void drawContent(GuiGraphicsExtractor g, int x0, int y0, int mx, int my, float dt) {
        int cx = x0 + 10 + SIDE_W + 14;
        int cr = x0 + PW - 14;
        int cw = cr - cx;
        boolean searching = !query.trim().isEmpty();

        // header
        String title = searching ? "Search results" : page.label;
        Gfx.text(g, font, title, cx, y0 + 16, UiTheme.text, false);
        Gfx.round(g, cx, y0 + 28, 18, 2, 1, UiTheme.accent);

        if (!page.library || searching) {
            int bw = 58;
            int bx = cr - bw;
            button(g, "resetpage", bx, y0 + 12, bw, 16, "Reset page", mx, my, dt, () -> {
                int n = 0;
                for (Object e : entries) {
                    if (e instanceof Setting st && !st.isDefault()) {
                        resetSetting(st);
                        n++;
                    }
                }
                Toasts.push(n == 0 ? "Nothing to reset" : "Reset " + n + " setting" + (n == 1 ? "" : "s"));
            });
        }

        // sub-category chips
        if (!searching && !page.library && groups.size() > 1) {
            int chx = cx;
            chips:
            {
                String[] labels = new String[groups.size() + 1];
                labels[0] = "All";
                for (int i = 0; i < groups.size(); i++) labels[i + 1] = groups.get(i);
                for (int i = 0; i < labels.length; i++) {
                    String label = labels[i];
                    int w = font.width(label) + 14;
                    if (chx + w > cr) break chips;
                    boolean sel = (i == 0 && group == null) || (i > 0 && labels[i].equals(group));
                    boolean hov = inside(mx, my, chx, y0 + 36, w, 16);
                    float hv = hover("chip:" + label, hov, dt);
                    Anim sa = anim("chipsel:" + label);
                    sa.target = sel ? 1f : 0f;
                    sa.update(dt);
                    int bg = Colors.lerp(Colors.alpha(UiTheme.text & 0xFFFFFF, 0f), UiTheme.hover, hv);
                    bg = Colors.lerp(bg, UiTheme.accentSoft, sa.value);
                    Gfx.round(g, chx, y0 + 36, w, 16, 8, bg);
                    Gfx.textCenter(g, font, label, chx + w / 2, y0 + 40,
                            Colors.lerp(UiTheme.textDim, UiTheme.accent, sa.value), false);
                    final String target = i == 0 ? null : labels[i];
                    addHit(chx, y0 + 36, w, 16, (b, hx, hy) -> {
                        group = target;
                        scrollTarget = 0f;
                        scrollCur = 0f;
                        pageAnim.snap(0f);
                        pageAnim.target = 1f;
                        buildEntries();
                    });
                    chx += w + 6;
                }
            }
        }

        // viewport
        int vy = y0 + 58;
        int vh = PH - 58 - 34;
        viewH = vh;
        clipTop = vy;
        clipBot = vy + vh;

        float pa = pageAnim.value;
        float prevGa = Gfx.ga;
        Gfx.ga = prevGa * pa;
        int yo = Math.round((1f - pa) * 8f);

        float maxScroll = Math.max(0f, contentH - vh);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        if (Settings.ANIM_SCROLL.get() && Motion.on()) {
            scrollCur += (scrollTarget - scrollCur) * (1f - (float) Math.exp(-dt * 16f * Motion.speed()));
            if (Math.abs(scrollTarget - scrollCur) < 0.3f) scrollCur = scrollTarget;
        } else {
            scrollCur = scrollTarget;
        }

        g.enableScissor(cx - 2, vy, cr + 2, vy + vh);
        int top = vy - Math.round(scrollCur) + yo;
        if (page.library && !searching) {
            contentH = drawLibrary(g, cx, top, cw - 8, mx, my, dt);
        } else {
            contentH = drawRows(g, cx, top, cw - 8, mx, my, dt);
        }
        g.disableScissor();

        // scrollbar
        if (contentH > vh) {
            int th = Math.max(20, Math.round(vh * vh / contentH));
            int ty = vy + Math.round((vh - th) * (scrollCur / Math.max(1f, contentH - vh)));
            Gfx.round(g, cr - 3, vy, 3, vh, 1, Colors.alpha(UiTheme.text & 0xFFFFFF, 0.06f));
            Gfx.round(g, cr - 3, ty, 3, th, 1, Colors.alpha(UiTheme.accent & 0xFFFFFF, 0.7f));
        }
        Gfx.ga = prevGa;

        if (searching && entries.isEmpty()) {
            Gfx.textCenter(g, font, "No settings found", cx + cw / 2, vy + 40, UiTheme.textDim, false);
        }

        // footer hints
        if (Settings.KEY_HINTS.get()) {
            String hint = "Up/Down select   Left/Right adjust   Enter toggle   Ctrl+F search   Del reset   Esc close";
            Gfx.text(g, font, Gfx.fit(font, hint, cw - 150), cx, y0 + PH - 22, UiTheme.textDim, false);
        }
    }

    private float drawRows(GuiGraphicsExtractor g, int x, int top, int w, int mx, int my, float dt) {
        int base = rowBase();
        int yy = top;
        boolean first = true;
        for (Object e : entries) {
            if (e instanceof String header) {
                int h = first ? 18 : 26;
                int ty = yy + h - 14;
                if (ty + 10 > clipTop && ty < clipBot) {
                    Gfx.text(g, font, header.toUpperCase(Locale.ROOT), x + 4, ty, UiTheme.textDim, false);
                    Gfx.fill(g, x + 8 + font.width(header.toUpperCase(Locale.ROOT)), ty + 4,
                            Math.max(0, w - font.width(header) - 14), 1, UiTheme.border);
                }
                yy += h;
                first = false;
                continue;
            }
            Setting s = (Setting) e;
            RowState st = state(s);
            st.expand.target = (s == openColor) ? 1f : 0f;
            st.expand.update(dt);
            int extra = s instanceof ColorSetting ? Math.round(64f * st.expand.value) : 0;
            int h = base + extra;
            st.relY = yy - top;
            st.h = h;
            if (yy + h > clipTop && yy < clipBot) drawRow(g, s, st, x, yy, w, base, extra, mx, my, dt);
            yy += h + 2;
            first = false;
        }
        return yy - top + 6;
    }

    private void drawRow(GuiGraphicsExtractor g, Setting s, RowState st, int x, int y, int w, int base, int extra,
                         int mx, int my, float dt) {
        boolean hov = inside(mx, my, x, y, w, base) && my >= clipTop && my < clipBot && openEnum == null;
        st.hover.target = hov ? 1f : 0f;
        if (!Settings.ANIM_HOVER.get()) st.hover.snap(st.hover.target); else st.hover.update(dt);
        st.flash.update(dt);

        int rad = Math.min(5, UiTheme.radius);
        int fillC = Colors.lerp(Colors.alpha(UiTheme.text & 0xFFFFFF, 0f), UiTheme.hover, st.hover.value);
        if (extra > 0) fillC = Colors.lerp(fillC, UiTheme.hover, 0.6f);
        fillC = Colors.lerp(fillC, Colors.alpha(UiTheme.accent & 0xFFFFFF, 0.28f), st.flash.value);
        Gfx.round(g, x, y, w, base + extra, rad, fillC);
        if (s == focused) Gfx.outline(g, x, y, w, base + extra, UiTheme.accent);

        boolean hasDesc = base >= 34 && !s.desc.isEmpty();
        int rr = x + w - 10;
        int cy = y + base / 2;
        int labelRight = rr - 170;
        Gfx.text(g, font, Gfx.fit(font, s.name, labelRight - (x + 12)), x + 12,
                hasDesc ? y + 6 : y + (base - 8) / 2, UiTheme.text, false);
        if (hasDesc) {
            Gfx.text(g, font, Gfx.fit(font, s.desc, labelRight - (x + 12)), x + 12, y + 19, UiTheme.textDim, false);
        }
        if (!s.isDefault()) Gfx.round(g, x + 4, cy - 2, 3, 3, 1, UiTheme.accent); // "modified" dot

        // whole-row click: right click resets, left click toggles booleans
        final Setting fs = s;
        addHit(x, y, w, base, (b, hx, hy) -> {
            focused = fs;
            if (b == 1) resetSetting(fs);
            else if (b == 0 && fs instanceof BoolSetting bs) {
                bs.toggle();
                flash(bs);
            }
        }, null);

        if (s instanceof BoolSetting b) {
            st.tog.target = b.get() ? 1f : 0f;
            st.tog.update(dt);
            int sx = rr - 30;
            int sy = cy - 8;
            Gfx.round(g, sx, sy, 30, 16, 8, Colors.lerp(UiTheme.track, UiTheme.accent, st.tog.value));
            Gfx.round(g, sx + 2 + Math.round(14f * st.tog.value), sy + 2, 12, 12, 6, UiTheme.text | 0xFF000000);
        } else if (s instanceof NumberSetting n) {
            int sw = 100;
            int sx = rr - 46 - 10 - sw;
            if (dragNum == n) st.fill.snap(n.fraction());
            else {
                st.fill.target = n.fraction();
                st.fill.update(dt);
            }
            Gfx.round(g, sx, cy - 2, sw, 4, 2, UiTheme.track);
            int fw = Math.max(4, Math.round(sw * st.fill.value));
            Gfx.round(g, sx, cy - 2, fw, 4, 2, UiTheme.accent);
            boolean grab = dragNum == n || inside(mx, my, sx - 4, y, sw + 8, base);
            int kn = grab ? 10 : 8;
            Gfx.round(g, sx + fw - kn / 2, cy - kn / 2, kn, kn, kn / 2, 0xFF000000 | UiTheme.text);
            Gfx.textRight(g, font, n.display(), rr, cy - 4, UiTheme.text, false);
            final NumberSetting fn = n;
            final int fsx = sx;
            addHit(sx - 4, y, sw + 8, base, (b, hx, hy) -> {
                if (b != 0) return;
                focused = fn;
                dragNum = fn;
                dragX = fsx;
                dragW = sw;
                applySlider(hx);
            }, dy -> {
                fn.set(fn.get() + (dy > 0 ? 1 : -1) * fn.step);
                flash(fn);
            });
        } else if (s instanceof EnumSetting en) {
            int pw = 112;
            int px = rr - pw;
            int py = cy - 9;
            boolean ph = inside(mx, my, px, py, pw, 18) && openEnum == null;
            float hv = hover(en, ph || openEnum == en, dt);
            Gfx.roundOutline(g, px, py, pw, 18, 4, Colors.lerp(UiTheme.border, UiTheme.accent, hv),
                    0xFF000000 | Colors.lerp(UiTheme.panel, UiTheme.hover | 0xFF000000, hv * 0.4f));
            Gfx.textCenter(g, font, Gfx.fit(font, en.get(), pw - 22), px + pw / 2 - 4, py + 5, UiTheme.text, false);
            Gfx.text(g, font, "v", px + pw - 11, py + 5, UiTheme.textDim, false);
            if (popEnum == en) {
                popX = px;
                popY = py;
                popW = pw;
            }
            final EnumSetting fe = en;
            addHit(px, py, pw, 18, (b, hx, hy) -> {
                focused = fe;
                if (b == 1) {
                    fe.cycle(-1);
                    flash(fe);
                } else if (b == 0) {
                    openEnum = fe;
                    popEnum = fe;
                    popX = px;
                    popY = py;
                    popW = pw;
                }
            }, dy -> {
                fe.cycle(dy > 0 ? -1 : 1);
                flash(fe);
            });
        } else if (s instanceof ColorSetting cs) {
            int swx = rr - 30;
            int swy = cy - 8;
            boolean ph = inside(mx, my, swx, swy, 30, 16);
            float hv = hover(cs, ph || openColor == cs, dt);
            Gfx.round(g, swx - 1, swy - 1, 32, 18, 5, Colors.lerp(UiTheme.border, UiTheme.accent, hv));
            Gfx.round(g, swx, swy, 30, 16, 4, 0xFF000000 | cs.get());
            Gfx.textRight(g, font, cs.hex(), swx - 8, cy - 4, UiTheme.textDim, false);
            final ColorSetting fc = cs;
            addHit(swx - 60, swy, 90, 16, (b, hx, hy) -> {
                focused = fc;
                if (b == 1) {
                    resetSetting(fc);
                    return;
                }
                if (openColor == fc) {
                    openColor = null;
                } else {
                    openColor = fc;
                    hsbMap.put(fc, Colors.rgbToHsb(fc.get()));
                }
            }, null);
            if (st.expand.value > 0.02f) drawColorEditor(g, cs, x, y + base, w, extra, mx, my);
        }
    }

    private void drawColorEditor(GuiGraphicsExtractor g, ColorSetting cs, int x, int y, int w, int h, int mx, int my) {
        g.enableScissor(x, y, x + w, y + h);
        float[] hsb = hsbMap.computeIfAbsent(cs, c -> Colors.rgbToHsb(c.get()));
        if (dragCol != cs && Colors.hsbToRgb(hsb[0], hsb[1], hsb[2]) != cs.get()) {
            float[] fresh = Colors.rgbToHsb(cs.get());
            hsbMap.put(cs, fresh);
            hsb = fresh;
        }
        final float[] fh = hsb;
        String[] names = {"Hue", "Sat", "Bri"};
        int bx = x + 44;
        int bw = Math.min(250, w - 60 - 4 * 20);
        int segs = 40;
        for (int ch = 0; ch < 3; ch++) {
            int by = y + 6 + ch * 17;
            Gfx.text(g, font, names[ch], x + 12, by + 1, UiTheme.textDim, false);
            for (int sI = 0; sI < segs; sI++) {
                float f = (sI + 0.5f) / segs;
                int c = switch (ch) {
                    case 0 -> Colors.hsbToRgb(f, Math.max(0.6f, fh[1]), Math.max(0.7f, fh[2]));
                    case 1 -> Colors.hsbToRgb(fh[0], f, Math.max(0.5f, fh[2]));
                    default -> Colors.hsbToRgb(fh[0], fh[1], f);
                };
                int x1 = bx + sI * bw / segs;
                int x2 = bx + (sI + 1) * bw / segs;
                Gfx.fill(g, x1, by, x2 - x1, 10, 0xFF000000 | c);
            }
            int kx = bx + Math.round(fh[ch] * bw);
            Gfx.fill(g, kx - 1, by - 2, 3, 14, 0xFF000000 | UiTheme.text);
            final int fch = ch;
            final int fby = by;
            addHit(bx - 3, by - 3, bw + 6, 16, (b, hx, hy) -> {
                if (b != 0) return;
                dragCol = cs;
                dragCh = fch;
                dragBarX = bx;
                dragBarW = bw;
                applyColorBar(hx);
            }, null);
        }
        // palette
        int px = bx + bw + 20;
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = px + (i % 4) * 20;
            int sy = y + 6 + (i / 4) * 20;
            int c = PALETTE[i];
            boolean hv = inside(mx, my, sx, sy, 16, 16);
            Gfx.round(g, sx - 1, sy - 1, 18, 18, 4, hv ? UiTheme.accent : UiTheme.border);
            Gfx.round(g, sx, sy, 16, 16, 3, 0xFF000000 | c);
            addHit(sx, sy, 16, 16, (b, hx, hy) -> {
                if (b != 0) return;
                cs.set(c);
                hsbMap.put(cs, Colors.rgbToHsb(c));
                flash(cs);
            }, null);
        }
        g.disableScissor();
    }

    private void applySlider(int lx) {
        if (dragNum == null) return;
        float f = Math.max(0f, Math.min(1f, (lx - dragX) / (float) dragW));
        dragNum.set(dragNum.min + f * (dragNum.max - dragNum.min));
    }

    private void applyColorBar(int lx) {
        if (dragCol == null) return;
        float f = Math.max(0f, Math.min(1f, (lx - dragBarX) / (float) dragBarW));
        float[] hsb = hsbMap.computeIfAbsent(dragCol, c -> Colors.rgbToHsb(c.get()));
        hsb[dragCh] = f;
        dragCol.set(Colors.hsbToRgb(hsb[0], hsb[1], hsb[2]));
    }

    // ---- library (presets / themes)

    private float drawLibrary(GuiGraphicsExtractor g, int x, int top, int w, int mx, int my, float dt) {
        Kind kind = kind();
        // name field + save button
        int fy = top;
        int fw = 230;
        boolean hovF = inside(mx, my, x, fy, fw, 20);
        float hv = hover("namefield", hovF || nameFocused, dt);
        Gfx.roundOutline(g, x, fy, fw, 20, 4, Colors.lerp(UiTheme.border, UiTheme.accent, nameFocused ? 1f : hv * 0.5f),
                0xFF000000 | UiTheme.panel);
        String shown = nameBuf.isEmpty() ? (nameFocused ? "" : "Name for a new " + (kind == Kind.PRESETS ? "preset" : "theme") + "...")
                : Gfx.fit(font, nameBuf, fw - 14);
        Gfx.text(g, font, shown, x + 7, fy + 6, nameBuf.isEmpty() ? UiTheme.textDim : UiTheme.text, false);
        if (nameFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            Gfx.fill(g, x + 7 + font.width(shown), fy + 5, 1, 10, UiTheme.text);
        }
        addHit(x, fy, fw, 20, (b, hx, hy) -> {
            nameFocused = true;
            searchFocused = false;
        });
        button(g, "savenew", x + fw + 8, fy, 100, 20, "Save current", mx, my, dt, this::saveNew);

        String info = kind == Kind.PRESETS
                ? "Presets store HUD, crosshair and effect settings."
                : "Themes store the menu colours, opacity, radius and shadow.";
        Gfx.text(g, font, Gfx.fit(font, info, w), x + 2, fy + 28, UiTheme.textDim, false);

        int yy = fy + 46;
        boolean firstUser = true;
        boolean sawBuiltIn = false;
        for (Entry e : libCache) {
            if (e.builtIn() && !sawBuiltIn) {
                drawLibHeader(g, "BUILT-IN", x, yy, w);
                yy += 20;
                sawBuiltIn = true;
            }
            if (!e.builtIn() && firstUser) {
                drawLibHeader(g, "YOUR " + (kind == Kind.PRESETS ? "PRESETS (CUSTOM)" : "THEMES"), x, yy + 4, w);
                yy += 24;
                firstUser = false;
            }
            if (yy + 26 > clipTop && yy < clipBot) drawLibRow(g, e, kind, x, yy, w, mx, my, dt);
            yy += 28;
        }
        if (firstUser) {
            Gfx.text(g, font, "No saved " + (kind == Kind.PRESETS ? "presets" : "themes") + " yet. Type a name and press Save current.",
                    x + 2, yy + 8, UiTheme.textDim, false);
            yy += 24;
        }
        return yy - top + 6;
    }

    private void drawLibHeader(GuiGraphicsExtractor g, String text, int x, int y, int w) {
        Gfx.text(g, font, text, x + 4, y + 4, UiTheme.textDim, false);
        Gfx.fill(g, x + 8 + font.width(text), y + 8, Math.max(0, w - font.width(text) - 14), 1, UiTheme.border);
    }

    private void drawLibRow(GuiGraphicsExtractor g, Entry e, Kind kind, int x, int y, int w, int mx, int my, float dt) {
        boolean hov = inside(mx, my, x, y, w, 26) && my >= clipTop && my < clipBot;
        float hv = hover("lib:" + e.name() + e.builtIn(), hov, dt);
        Gfx.round(g, x, y, w, 26, Math.min(5, UiTheme.radius),
                Colors.lerp(Colors.alpha(UiTheme.text & 0xFFFFFF, 0f), UiTheme.hover, hv));
        Gfx.text(g, font, e.name(), x + 12, y + 9, UiTheme.text, false);
        int rr = x + w - 8;
        if (!e.builtIn()) {
            rr -= 56;
            button(g, "del:" + e.name(), rr, y + 4, 56, 18, "Delete", mx, my, dt, () -> {
                LibraryService.delete(kind, e.name());
                refreshLibrary();
                Toasts.push("Deleted " + e.name());
            });
            rr -= 6 + 76;
            button(g, "ow:" + e.name(), rr, y + 4, 76, 18, "Overwrite", mx, my, dt, () -> {
                if (LibraryService.saveCurrent(kind, e.name())) Toasts.push("Saved " + e.name());
            });
            rr -= 6 + 52;
        } else {
            rr -= 52;
        }
        button(g, "load:" + e.name() + e.builtIn(), rr, y + 4, 52, 18, "Load", mx, my, dt, () -> {
            if (LibraryService.apply(kind, e)) {
                Toasts.push((kind == Kind.PRESETS ? "Preset loaded: " : "Theme applied: ") + e.name());
            } else {
                Toasts.push("Could not load " + e.name());
            }
        });
    }

    private void saveNew() {
        String name = ConfigManager.sanitize(nameBuf);
        if (name.isEmpty()) {
            Toasts.push("Enter a name first");
            return;
        }
        if (LibraryService.saveCurrent(kind(), name)) {
            Toasts.push("Saved " + name);
            nameBuf = "";
            nameFocused = false;
            refreshLibrary();
        } else {
            Toasts.push("Could not save (name reserved?)");
        }
    }

    // ---- popup (enum dropdown)

    private void drawPopup(GuiGraphicsExtractor g, int x0, int y0, int mx, int my, float dt) {
        dropAnim.target = openEnum != null ? 1f : 0f;
        dropAnim.update(dt);
        if (popEnum == null || dropAnim.value < 0.01f) {
            if (openEnum == null) popEnum = null;
            return;
        }
        EnumSetting en = popEnum;
        int ih = 17;
        int full = en.options.length * ih + 6;
        int shown = Math.max(1, Math.round(full * dropAnim.value));
        int px = popX;
        int py = popY + 20;
        boolean flip = py + full > y0 + PH - 4;
        if (flip) py = popY - 2 - shown;
        Gfx.shadow(g, px, py, popW, shown, 4, 0.6f);
        Gfx.roundOutline(g, px, py, popW, shown, 4, UiTheme.border, 0xFF000000 | Colors.lerp(UiTheme.panel, 0xFF000000, 0.15f));
        g.enableScissor(px, py, px + popW, py + shown);
        for (int i = 0; i < en.options.length; i++) {
            int iy = flip ? py + shown - full + 3 + i * ih : py + 3 + i * ih;
            boolean hov = inside(mx, my, px + 3, iy, popW - 6, ih - 1) && dropAnim.value > 0.9f;
            boolean sel = i == en.index();
            Gfx.round(g, px + 3, iy, popW - 6, ih - 1, 3,
                    sel ? UiTheme.accentSoft : (hov ? UiTheme.hover : 0x00000000));
            Gfx.text(g, font, en.options[i], px + 10, iy + 4, sel ? UiTheme.accent : UiTheme.text, false);
            final int idx = i;
            if (dropAnim.value > 0.9f) {
                popupHits.add(new Hit(px + 3, iy, popW - 6, ih - 1, (b, hx, hy) -> {
                    en.setIndex(idx);
                    flash(en);
                    openEnum = null;
                }, null));
            }
        }
        g.disableScissor();
    }

    // ---- widgets

    private boolean button(GuiGraphicsExtractor g, String id, int x, int y, int w, int h, String label,
                           int mx, int my, float dt, Runnable action) {
        boolean hov = inside(mx, my, x, y, w, h) && my >= clipTop - 40 && my < clipBot + 40;
        float hv = hover(id, hov, dt);
        Gfx.roundOutline(g, x, y, w, h, 4, Colors.lerp(UiTheme.border, UiTheme.accent, hv),
                0xFF000000 | Colors.lerp(UiTheme.panel, UiTheme.hover | 0xFF000000, hv * 0.5f));
        Gfx.textCenter(g, font, label, x + w / 2, y + (h - 8) / 2, Colors.lerp(UiTheme.text, UiTheme.accent, hv), false);
        addHit(x, y, w, h, (b, hx, hy) -> {
            if (b == 0) action.run();
        });
        return hov;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void addHit(int x, int y, int w, int h, ClickAction click) {
        addHit(x, y, w, h, click, null);
    }

    /** Registers a clickable region; regions inside the scrolling viewport are clipped to it. */
    private void addHit(int x, int y, int w, int h, ClickAction click, ScrollAction scroll) {
        hits.add(new Hit(x, y, w, h, click, scroll));
    }

    private Hit hitAt(List<Hit> list, int mx, int my) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Hit h = list.get(i);
            if (inside(mx, my, h.x, h.y, h.w, h.h)) return h;
        }
        return null;
    }

    private int lx(double mx) { return (int) Math.round((mx - width / 2.0) / curScale + width / 2.0); }

    private int ly(double my) { return (int) Math.round((my - height / 2.0) / curScale + height / 2.0); }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = lx(event.x());
        int my = ly(event.y());
        int b = event.button();

        if (openEnum != null) {
            Hit ph = hitAt(popupHits, mx, my);
            if (ph != null) ph.click.click(b, mx, my);
            openEnum = null;
            return true;
        }
        searchFocused = false;
        nameFocused = false;
        Hit h = hitAt(hits, mx, my);
        if (h != null && h.click != null) {
            // Rows scrolled partly beyond the viewport edge must not be clickable there.
            boolean inContent = h.x > (width - PW) / 2 + SIDE_W + 20 && my > (height - PH) / 2 + 55;
            if (inContent && (my < clipTop || my >= clipBot)) return true;
            h.click.click(b, mx, my);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragNum != null) flash(dragNum);
        dragNum = null;
        dragCol = null;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int mx = lx(event.x());
        if (dragNum != null) applySlider(mx);
        if (dragCol != null) applyColorBar(mx);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = lx(mouseX);
        int my = ly(mouseY);
        if (openEnum != null) {
            openEnum = null;
            return true;
        }
        Hit h = hitAt(hits, mx, my);
        if (h != null && h.scroll != null && my >= clipTop && my < clipBot) {
            h.scroll.scroll(scrollY);
            return true;
        }
        scrollTarget = Math.max(0f, Math.min(Math.max(0f, contentH - viewH), scrollTarget - (float) scrollY * 30f));
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (cp < 32 || cp == 127) return false;
        String s = new String(Character.toChars(cp));
        if (nameFocused) {
            if ((Character.isLetterOrDigit(cp) || cp == ' ' || cp == '_' || cp == '-') && cp < 128 && nameBuf.length() < 24) {
                nameBuf += s;
            }
            return true;
        }
        searchFocused = true;
        if (query.length() < 28) {
            query += s;
            onQueryChanged();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (!searchFocused && !nameFocused && LuminaKeys.OPEN.matches(event)) {
            onClose();
            return true;
        }
        if (k == GLFW.GLFW_KEY_ESCAPE) {
            if (openEnum != null) {
                openEnum = null;
            } else if (searchFocused && !query.isEmpty()) {
                query = "";
                onQueryChanged();
            } else if (searchFocused || nameFocused) {
                searchFocused = false;
                nameFocused = false;
            } else if (!query.isEmpty()) {
                query = "";
                onQueryChanged();
            } else {
                onClose();
            }
            return true;
        }
        if (ctrl && k == GLFW.GLFW_KEY_F) {
            searchFocused = true;
            nameFocused = false;
            return true;
        }
        if (nameFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !nameBuf.isEmpty()) nameBuf = nameBuf.substring(0, nameBuf.length() - 1);
            else if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) saveNew();
            return true;
        }
        if (searchFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                onQueryChanged();
                return true;
            }
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_DOWN) {
                searchFocused = false;
                moveFocus(1);
                return true;
            }
            return false;
        }
        if (openEnum != null) {
            if (k == GLFW.GLFW_KEY_UP) { openEnum.cycle(-1); flash(openEnum); }
            else if (k == GLFW.GLFW_KEY_DOWN) { openEnum.cycle(1); flash(openEnum); }
            else if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER || k == GLFW.GLFW_KEY_SPACE) openEnum = null;
            return true;
        }
        switch (k) {
            case GLFW.GLFW_KEY_TAB -> {
                Category[] c = Category.values();
                selectPage(c[Math.floorMod(page.ordinal() + (shift ? -1 : 1), c.length)]);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> { moveFocus(1); return true; }
            case GLFW.GLFW_KEY_UP -> { moveFocus(-1); return true; }
            case GLFW.GLFW_KEY_LEFT -> { adjustFocused(-1, shift); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { adjustFocused(1, shift); return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> { activateFocused(); return true; }
            case GLFW.GLFW_KEY_DELETE -> {
                if (focused != null) resetSetting(focused);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollTarget += viewH * 0.8f; return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { scrollTarget -= viewH * 0.8f; return true; }
            case GLFW.GLFW_KEY_HOME -> { scrollTarget = 0f; return true; }
            case GLFW.GLFW_KEY_END -> { scrollTarget = contentH; return true; }
            default -> { return false; }
        }
    }

    private void moveFocus(int dir) {
        List<Setting> list = new ArrayList<>();
        for (Object e : entries) if (e instanceof Setting s) list.add(s);
        if (list.isEmpty()) return;
        int idx = focused == null ? (dir > 0 ? -1 : list.size()) : list.indexOf(focused);
        idx = Math.max(0, Math.min(list.size() - 1, idx + dir));
        focused = list.get(idx);
        RowState st = state(focused);
        if (st.relY < scrollTarget) scrollTarget = Math.max(0f, st.relY - 24f);
        else if (st.relY + st.h > scrollTarget + viewH) scrollTarget = st.relY + st.h - viewH + 6f;
    }

    private void adjustFocused(int dir, boolean big) {
        if (focused == null) return;
        if (focused instanceof NumberSetting n) {
            n.set(n.get() + dir * n.step * (big ? 5 : 1));
            flash(n);
        } else if (focused instanceof EnumSetting e) {
            e.cycle(dir);
            flash(e);
        } else if (focused instanceof BoolSetting b) {
            b.set(dir > 0);
            flash(b);
        }
    }

    private void activateFocused() {
        if (focused instanceof BoolSetting b) {
            b.toggle();
            flash(b);
        } else if (focused instanceof EnumSetting e) {
            openEnum = e;
            popEnum = e;
        } else if (focused instanceof ColorSetting c) {
            if (openColor == c) openColor = null;
            else {
                openColor = c;
                hsbMap.put(c, Colors.rgbToHsb(c.get()));
            }
        }
    }
}
