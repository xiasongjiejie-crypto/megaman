package com.megaman;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * 类洛克人 2D 横版动作游戏 —— 平台无关核心（core 模块）。
 * 含 8 个原创元素 Boss 与选关界面。音频/字体经 Gdx.files.internal 从 assets 加载。
 */
public class CoreGame extends Game {
    @Override
    public void create() {
        setScreen(new GameScreen());
    }
}

/* ===================== 常量 ===================== */
final class K {
    private K() {}
    static final float VW = 480f, VH = 270f;

    static final float GRAVITY = -1500f;
    static final float MAX_FALL = -600f;

    static final float P_MOVE = 130f;
    static final float P_JUMP = 430f;
    static final int   P_MAX_JUMPS = 2;
    static final float P_W = 12f, P_H = 20f;
    static final int   P_MAX_HP = 28;
    static final int   P_LIVES = 3;

    static final float DASH_SPEED = 320f;
    static final float DASH_TIME = 0.18f;
    static final float DASH_CD = 0.5f;

    static final float SHOOT_TAP = 0.08f;
    static final float CHARGE_L1 = 0.35f;
    static final float CHARGE_L2 = 0.85f;

    static final float E_SPEED = 40f;
    static final float E_W = 16f, E_H = 16f;
    static final int   E_DMG = 4;

    static final float EB_SPEED = 150f;
    static final int   EB_DMG = 3;

    static final float BOSS_SPEED = 70f;
    static final int   BOSS_TOUCH_DMG = 8;
}

/* ===================== 音频（资源文件加载，跨平台） ===================== */
class Sfx {
    Sound shoot, charged, charge, jump, dash, hurt, explode, enemyShot, doorIn, clear;
    Music bgm;

    void load() {
        shoot     = snd("sfx/shoot.wav");
        charged   = snd("sfx/charged.wav");
        charge    = snd("sfx/charge.wav");
        jump      = snd("sfx/jump.wav");
        dash      = snd("sfx/dash.wav");
        hurt      = snd("sfx/hurt.wav");
        explode   = snd("sfx/explode.wav");
        enemyShot = snd("sfx/eshot.wav");
        doorIn    = snd("sfx/door.wav");
        clear     = snd("sfx/clear.wav");

        if (ext("bgm.ogg"))      bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.ogg"));
        else if (ext("bgm.wav")) bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.wav"));
        if (bgm != null) { bgm.setLooping(true); bgm.setVolume(0.45f); }
    }

    void play(Sound s, float vol) { if (s != null) s.play(vol); }

    private boolean ext(String f) {
        try { return Gdx.files.internal(f).exists(); } catch (Exception e) { return false; }
    }

    private Sound snd(String f) {
        try { return ext(f) ? Gdx.audio.newSound(Gdx.files.internal(f)) : null; }
        catch (Exception e) { return null; }
    }

    void dispose() {
        Sound[] all = {shoot, charged, charge, jump, dash, hurt, explode, enemyShot, doorIn, clear};
        for (Sound s : all) if (s != null) s.dispose();
        if (bgm != null) bgm.dispose();
    }
}

/* ===================== 资源（程序化像素 + 外部覆盖） ===================== */
class Assets {
    Texture player, walker, shooter, flyer, door, bullet, ebullet, tile, bgFar, bgNear, pixel;
    Texture[] bosses = new Texture[8];
    BitmapFont font;
    private final Array<Texture> managed = new Array<>();

    // 8 个 Boss 的主题调色板：{主色, 暗色, 核心亮色}
    private static final int[][] BOSS_PAL = {
        {0x7A3AD0, 0x3A1A66, 0xC0A0FF}, // 0 重力 GRAVITON
        {0xF0C020, 0x8A6A00, 0xFFFFC0}, // 1 光   PHOTON
        {0x3A2A5A, 0x140A24, 0x8A6AC0}, // 2 暗   SHADE
        {0xE0501A, 0x7A2000, 0xFFC060}, // 3 火   BLAZE
        {0x3AA84B, 0x155A22, 0xB0FF80}, // 4 自然 THORN
        {0x9A6A3A, 0x4A2A10, 0xE0C080}, // 5 岩   BOULDER
        {0x5AC0E0, 0x1A5A7A, 0xD0F8FF}, // 6 冰   GLACIER
        {0xE0D020, 0x2A3A8A, 0xFFFF80}, // 7 电   VOLT
    };

    void load() {
        player  = ext("player.png")  ? file("player.png")  : makePlayer();
        walker  = ext("walker.png")  ? file("walker.png")  : makeWalker();
        shooter = ext("shooter.png") ? file("shooter.png") : makeShooter();
        flyer   = ext("flyer.png")   ? file("flyer.png")   : makeFlyer();
        door    = ext("door.png")    ? file("door.png")    : makeDoor();
        bullet  = ext("bullet.png")  ? file("bullet.png")  : makeBullet();
        ebullet = ext("ebullet.png") ? file("ebullet.png") : makeEBullet();
        tile    = ext("tile.png")    ? file("tile.png")    : makeTile();
        bgFar   = ext("bg.png")      ? file("bg.png")      : makeSky();
        bgNear  = makeHill();
        pixel   = makePixel();
        for (int i = 0; i < 8; i++)
            bosses[i] = ext("boss" + i + ".png") ? file("boss" + i + ".png") : makeBossTex(i);

        if (ext("ui/font.fnt")) font = new BitmapFont(Gdx.files.internal("ui/font.fnt"));
        else                    font = new BitmapFont();
        font.setUseIntegerPositions(false);
    }

    private boolean ext(String f) {
        try { return Gdx.files.internal(f).exists(); } catch (Exception e) { return false; }
    }

    private Texture file(String f) {
        Texture t = new Texture(Gdx.files.internal(f));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        managed.add(t);
        return t;
    }

    private Pixmap pm(int w, int h) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        return p;
    }

    private void rect(Pixmap p, int x, int y, int w, int h, int rgb) {
        p.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1f);
        p.fillRectangle(x, y, w, h);
    }

    private Texture tex(Pixmap p) {
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        managed.add(t);
        return t;
    }

    private Texture makePixel() { Pixmap p = pm(1, 1); rect(p, 0, 0, 1, 1, 0xFFFFFF); return tex(p); }

    private Texture makePlayer() {
        Pixmap p = pm(12, 20);
        int body = 0x3A78D0, helm = 0x6FB0FF, face = 0xFFD9A0, dark = 0x244E8C, gun = 0xCFD6E0;
        rect(p, 2, 0, 8, 3, helm);
        rect(p, 3, 3, 6, 3, face);
        rect(p, 3, 3, 6, 1, helm);
        rect(p, 2, 6, 8, 8, body);
        rect(p, 3, 7, 6, 2, dark);
        rect(p, 9, 8, 3, 3, gun);
        rect(p, 3, 14, 3, 6, dark);
        rect(p, 6, 14, 3, 6, dark);
        return tex(p);
    }

    private Texture makeWalker() {
        Pixmap p = pm(16, 16);
        int red = 0xD94B3A, dark = 0x5A1A12, white = 0xFFFFFF;
        rect(p, 7, 1, 2, 3, red);
        rect(p, 2, 4, 12, 9, red);
        rect(p, 5, 6, 6, 3, dark);
        rect(p, 6, 7, 2, 1, white);
        rect(p, 3, 13, 3, 3, dark);
        rect(p, 10, 13, 3, 3, dark);
        return tex(p);
    }

    private Texture makeShooter() {
        Pixmap p = pm(16, 18);
        int pur = 0xA84BD9, dark = 0x3A1259, gun = 0xCFD6E0;
        rect(p, 2, 10, 12, 8, dark);
        rect(p, 4, 3, 8, 8, pur);
        rect(p, 6, 5, 4, 3, dark);
        rect(p, 11, 6, 5, 3, gun);
        return tex(p);
    }

    private Texture makeFlyer() {
        Pixmap p = pm(16, 16);
        int teal = 0x33CFC0, light = 0x9CF0E8, dark = 0x115C55;
        rect(p, 0, 6, 4, 3, light);
        rect(p, 12, 6, 4, 3, light);
        rect(p, 4, 5, 8, 6, teal);
        rect(p, 6, 6, 4, 2, dark);
        return tex(p);
    }

    /** 8 个 Boss 的原创像素建模：统一机甲轮廓 + 各主题配色 + 专属造型细节。 */
    private Texture makeBossTex(int t) {
        int main = BOSS_PAL[t][0], dark = BOSS_PAL[t][1], core = BOSS_PAL[t][2];
        Pixmap p = pm(28, 32);
        rect(p, 3, 24, 6, 8, dark);    // 左腿
        rect(p, 19, 24, 6, 8, dark);   // 右腿
        rect(p, 1, 8, 5, 6, dark);     // 左肩
        rect(p, 22, 8, 5, 6, dark);    // 右肩
        rect(p, 4, 8, 20, 16, main);   // 躯干
        rect(p, 2, 9, 3, 2, core);     // 肩饰
        rect(p, 23, 9, 3, 2, core);
        rect(p, 10, 1, 8, 7, main);    // 头
        rect(p, 11, 3, 6, 2, core);    // 眼

        // 核心：偶数方核 / 奇数十字核
        if (t % 2 == 0) rect(p, 11, 12, 6, 6, core);
        else { rect(p, 13, 11, 2, 8, core); rect(p, 10, 14, 8, 2, core); }

        // 头顶专属造型（按主题差异化）
        switch (t) {
            case 0: rect(p, 12, 0, 4, 1, core); break;                      // 重力：短平冠
            case 1: rect(p, 13, 0, 2, 1, core); rect(p, 9, 4, 2, 2, core); rect(p, 17, 4, 2, 2, core); break; // 光：侧发光点
            case 2: rect(p, 10, 0, 8, 1, dark); break;                      // 暗：暗檐
            case 3: rect(p, 11, 0, 2, 2, core); rect(p, 15, 0, 2, 2, core); break; // 火：双火苗
            case 4: rect(p, 13, 0, 2, 2, core); rect(p, 11, 1, 6, 1, core); break; // 自然：叶尖
            case 5: rect(p, 9, 6, 10, 2, dark); break;                      // 岩：厚护额
            case 6: rect(p, 12, 0, 1, 2, core); rect(p, 15, 0, 1, 2, core); break; // 冰：冰棱
            case 7: rect(p, 13, 0, 1, 2, core); rect(p, 11, 2, 6, 1, core); break; // 电：尖角
            default:
        }
        return tex(p);
    }

    private Texture makeTile() {
        Pixmap p = pm(16, 16);
        int base = 0x4A4E5C, top = 0x6E7484, bot = 0x2E323C;
        rect(p, 0, 0, 16, 16, base);
        rect(p, 0, 0, 16, 3, top);
        rect(p, 0, 13, 16, 3, bot);
        rect(p, 0, 0, 1, 16, top);
        rect(p, 15, 0, 1, 16, bot);
        return tex(p);
    }

    private Texture makeDoor() {
        Pixmap p = pm(24, 48);
        int frame = 0x2A2E3A, glow = 0x35E0C0, glow2 = 0x9CFFEF, panel = 0x123A38;
        rect(p, 0, 0, 24, 48, frame);
        rect(p, 3, 3, 18, 42, panel);
        rect(p, 6, 6, 12, 36, glow);
        rect(p, 9, 10, 6, 28, glow2);
        return tex(p);
    }

    private Texture makeBullet() {
        Pixmap p = pm(8, 6);
        int c = 0xFFEB3A, hi = 0xFFFFFF;
        rect(p, 1, 1, 6, 4, c);
        rect(p, 0, 2, 8, 2, c);
        rect(p, 2, 2, 2, 2, hi);
        return tex(p);
    }

    private Texture makeEBullet() {
        Pixmap p = pm(6, 6);
        int c = 0xFF8C26, hi = 0xFFD9A0;
        rect(p, 1, 0, 4, 6, c);
        rect(p, 0, 1, 6, 4, c);
        rect(p, 1, 1, 2, 2, hi);
        return tex(p);
    }

    private Texture makeSky() {
        Pixmap p = pm(1, 64);
        int top = 0x0B1024, bot = 0x202A50;
        for (int y = 0; y < 64; y++) {
            float f = y / 63f;
            int r = lerp((top >> 16) & 255, (bot >> 16) & 255, f);
            int g = lerp((top >> 8) & 255, (bot >> 8) & 255, f);
            int b = lerp(top & 255, bot & 255, f);
            p.setColor(r / 255f, g / 255f, b / 255f, 1f);
            p.drawPixel(0, y);
        }
        return tex(p);
    }

    private Texture makeHill() {
        Pixmap p = pm(80, 70);
        int c = 0x1A2042;
        for (int x = 0; x < 80; x++) {
            int h = (int) (30 + 30 * Math.sin(x / 80.0 * Math.PI));
            rect(p, x, 70 - h, 1, h, c);
        }
        return tex(p);
    }

    private static int lerp(int a, int b, float f) { return (int) (a + (b - a) * f); }

    void dispose() {
        for (Texture t : managed) t.dispose();
        if (font != null) font.dispose();
    }
}

/* ===================== 实体基类 ===================== */
abstract class Entity {
    final Rectangle bounds = new Rectangle();
    final Vector2 vel = new Vector2();
    boolean alive = true;
    float cx() { return bounds.x + bounds.width / 2f; }
    float cy() { return bounds.y + bounds.height / 2f; }
}

/* ===================== 玩家子弹（含蓄力等级） ===================== */
class Bullet extends Entity {
    int level;
    int dmgBoss;
    boolean pierce;

    Bullet(float x, float y, int dir, int level) {
        this.level = level;
        float w, h, sp;
        switch (level) {
            case 2:  w = 22; h = 13; sp = 360; dmgBoss = 10; pierce = true; break;
            case 1:  w = 14; h = 9;  sp = 340; dmgBoss = 5;  pierce = false; break;
            default: w = 8;  h = 4;  sp = 320; dmgBoss = 2;  pierce = false;
        }
        bounds.set(x, y, w, h);
        vel.x = dir * sp;
    }

    void tint(SpriteBatch b) {
        switch (level) {
            case 2:  b.setColor(1f, 0.4f, 1f, 1f); break;
            case 1:  b.setColor(0.4f, 0.95f, 1f, 1f); break;
            default: b.setColor(1f, 0.92f, 0.25f, 1f);
        }
    }

    void update(float dt, Level level) {
        if (!alive) return;
        bounds.x += vel.x * dt;
        if (bounds.x < -60 || bounds.x > level.width + 60) { alive = false; return; }
        for (Rectangle s : level.solids) {
            if (s.overlaps(bounds)) { alive = false; return; }
        }
    }
}

/* ===================== 敌方子弹 ===================== */
class EBullet extends Entity {
    EBullet(float x, float y, float dx, float dy, float speed) {
        bounds.set(x, y, 6, 6);
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001f) len = 1f;
        vel.set(dx / len * speed, dy / len * speed);
    }

    void update(float dt, Level level) {
        if (!alive) return;
        bounds.x += vel.x * dt;
        bounds.y += vel.y * dt;
        if (bounds.x < -40 || bounds.x > level.width + 40 || bounds.y < -40 || bounds.y > K.VH + 40) {
            alive = false;
            return;
        }
        for (Rectangle s : level.solids) {
            if (s.overlaps(bounds)) { alive = false; return; }
        }
    }
}

/* ===================== 敌人（含 AI） ===================== */
class Enemy extends Entity {
    static final int WALKER = 0, SHOOTER = 1, FLYER = 2;

    int type;
    float min, max, baseY, phase, fireT;
    int dir = 1;

    static Enemy walker(float x, float y, float min, float max) {
        Enemy e = new Enemy(); e.type = WALKER; e.bounds.set(x, y, K.E_W, K.E_H); e.min = min; e.max = max; return e;
    }
    static Enemy shooter(float x, float y) {
        Enemy e = new Enemy(); e.type = SHOOTER; e.bounds.set(x, y, 16, 18); e.fireT = 0.6f; return e;
    }
    static Enemy flyer(float x, float baseY) {
        Enemy e = new Enemy(); e.type = FLYER; e.bounds.set(x, baseY, 16, 16); e.baseY = baseY; e.fireT = 1.0f; return e;
    }

    void update(float dt, Player p, Array<EBullet> eb, float worldW) {
        if (!alive) return;
        switch (type) {
            case WALKER: {
                boolean sight = Math.abs(p.cx() - cx()) < 150 && Math.abs(p.cy() - cy()) < 50;
                if (sight) dir = p.cx() > cx() ? 1 : -1;
                float sp = sight ? K.E_SPEED * 2f : K.E_SPEED;
                bounds.x += dir * sp * dt;
                if (bounds.x < min) { bounds.x = min; dir = 1; }
                if (bounds.x + bounds.width > max) { bounds.x = max - bounds.width; dir = -1; }
                break;
            }
            case SHOOTER: {
                fireT -= dt;
                if (Math.abs(p.cx() - cx()) < 220 && fireT <= 0) { fireAt(eb, p); fireT = 1.2f; }
                break;
            }
            case FLYER: {
                phase += dt;
                bounds.y = baseY + (float) Math.sin(phase * 2.2f) * 24f;
                dir = p.cx() > cx() ? 1 : -1;
                bounds.x += dir * K.E_SPEED * 0.9f * dt;
                if (bounds.x < 0) bounds.x = 0;
                if (bounds.x + bounds.width > worldW) bounds.x = worldW - bounds.width;
                fireT -= dt;
                if (Math.abs(p.cx() - cx()) < 260 && fireT <= 0) { fireAt(eb, p); fireT = 1.6f; }
                break;
            }
            default:
        }
    }

    Texture tex(Assets a) {
        switch (type) { case SHOOTER: return a.shooter; case FLYER: return a.flyer; default: return a.walker; }
    }

    private void fireAt(Array<EBullet> eb, Player p) {
        eb.add(new EBullet(cx() - 3, cy() - 3, p.cx() - cx(), p.cy() - cy(), K.EB_SPEED));
    }
}

/* ===================== Boss（8 主题多招式状态机） ===================== */
class Boss extends Entity {
    // 招式常量
    static final int IDLE = 0, APPROACH = 1, FAN = 2, VOLLEY = 3, DASH = 4, SLAM = 5,
            GFLIP = 6, WELL = 7, PYRAMID = 8, LASER = 9, RAIN = 10, BURST = 11, TELEPORT = 12, GUARD = 13;

    // 8 个 Boss 名称 / 血量 / 招式序列（主题灵感：重力/光/暗/火/自然/岩/冰/电）
    static final String[] NAMES = {"GRAVITON", "PHOTON", "SHADE", "BLAZE", "THORN", "BOULDER", "GLACIER", "VOLT"};
    static final int[] HP = {70, 64, 66, 72, 74, 80, 66, 64};
    private static final int[][] SEQS = {
        {GFLIP, WELL, PYRAMID, DASH, SLAM},        // 0 重力
        {LASER, VOLLEY, DASH, BURST, FAN},         // 1 光：高速激光
        {TELEPORT, VOLLEY, FAN, TELEPORT, BURST},  // 2 暗：瞬移
        {RAIN, DASH, FAN, SLAM, BURST},            // 3 火：火雨
        {GUARD, APPROACH, VOLLEY, DASH, FAN},      // 4 自然：防御反击
        {RAIN, SLAM, APPROACH, BURST, SLAM},       // 5 岩：落石震地
        {FAN, DASH, VOLLEY, BURST, LASER},         // 6 冰：冰锥散射
        {LASER, DASH, BURST, VOLLEY, LASER},       // 7 电：连续闪电
    };

    int hp, maxHp;
    int bossType;
    float groundTop;
    boolean onGround;

    private final int[] seq;
    private int state = IDLE, seqI = 0, shots = 0;
    private float timer = 0.8f, subT = 0f;
    private int dashDir = -1;
    private boolean didJump = false, waveDone = false;
    boolean slamLanded = false;
    boolean flipStarted = false;
    boolean guarding = false;

    Boss(float x, float groundTop, int hp, int type) {
        bounds.set(x, groundTop, 28, 32);
        this.groundTop = groundTop;
        this.hp = this.maxHp = hp;
        this.bossType = type;
        this.seq = SEQS[type];
    }

    boolean enraged() { return hp <= maxHp / 2; }

    void update(float dt, Player p, Array<EBullet> eb, float left, float right) {
        if (!alive) return;
        slamLanded = false;
        flipStarted = false;
        float mul = enraged() ? 1.5f : 1f;

        boolean wasAir = !onGround;
        vel.y += K.GRAVITY * dt;
        bounds.y += vel.y * dt;
        onGround = false;
        if (bounds.y < groundTop) { bounds.y = groundTop; vel.y = 0; onGround = true; }

        timer -= dt;
        switch (state) {
            case IDLE:
                vel.x = 0;
                if (timer <= 0) startNext(p, eb, left, right);
                break;
            case APPROACH:
                vel.x = (p.cx() > cx() ? 1 : -1) * K.BOSS_SPEED * mul;
                if (timer <= 0) endMove(mul);
                break;
            case FAN:
                vel.x = 0;
                if (timer <= 0) endMove(mul);
                break;
            case VOLLEY:
                vel.x = 0;
                subT -= dt;
                if (shots > 0 && subT <= 0) { aimed(eb, p, mul); shots--; subT = 0.18f; }
                if (timer <= 0) endMove(mul);
                break;
            case DASH:
                vel.x = dashDir * (K.BOSS_SPEED * 3.2f) * mul;
                if (timer <= 0) endMove(mul);
                break;
            case SLAM:
                if (!didJump && onGround) {
                    vel.y = 440f;
                    vel.x = (p.cx() > cx() ? 1 : -1) * 130f;
                    didJump = true;
                }
                if (didJump && wasAir && onGround && !waveDone) {
                    eb.add(new EBullet(cx() - 3, groundTop + 3, -1, 0, K.EB_SPEED * 1.2f));
                    eb.add(new EBullet(cx() - 3, groundTop + 3, 1, 0, K.EB_SPEED * 1.2f));
                    waveDone = true;
                    slamLanded = true;
                    vel.x = 0;
                }
                if (timer <= 0 && onGround) endMove(mul);
                break;
            case GFLIP:
                // 重力反转：拉玩家至天花板，期间持续扇形弹幕
                vel.x = 0;
                subT -= dt;
                if (subT <= 0) { fan(eb, p, mul); subT = 0.6f; }
                if (timer <= 0) { p.gravitySign = 1f; endMove(mul); }
                break;
            case WELL: {
                // 引力井：把玩家往 Boss 拽 + 瞄准弹
                vel.x = 0;
                float dx = cx() - p.cx(), dy = cy() - p.cy();
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 0.001f) len = 1f;
                float pull = 260f * mul;
                p.vel.x += dx / len * pull * dt;
                p.vel.y += dy / len * pull * dt;
                subT -= dt;
                if (subT <= 0) { aimed(eb, p, mul); subT = 0.35f; }
                if (timer <= 0) endMove(mul);
                break;
            }
            case PYRAMID:
                // 金字塔：多轮 8 方向环形弹
                vel.x = 0;
                subT -= dt;
                if (shots > 0 && subT <= 0) { ring(eb, mul); shots--; subT = 0.5f; }
                if (timer <= 0) endMove(mul);
                break;
            case LASER:
                // 激光：连续高速水平弹（光/电）
                vel.x = 0;
                subT -= dt;
                if (shots > 0 && subT <= 0) {
                    int d = p.cx() > cx() ? 1 : -1;
                    eb.add(new EBullet(cx() - 3, cy() - 3, d, 0, K.EB_SPEED * 2.3f));
                    shots--; subT = 0.14f;
                }
                if (timer <= 0) endMove(mul);
                break;
            case RAIN:
                // 火雨/落石：从上方落下弹（火/岩）
                vel.x = 0;
                subT -= dt;
                if (shots > 0 && subT <= 0) {
                    float rx = p.cx() + (float) (Math.random() * 90 - 45);
                    eb.add(new EBullet(rx, K.VH - 6, 0, -1, K.EB_SPEED * 1.15f));
                    shots--; subT = 0.18f;
                }
                if (timer <= 0) endMove(mul);
                break;
            case BURST:
                // 爆发：多轮环形弹幕
                vel.x = 0;
                subT -= dt;
                if (shots > 0 && subT <= 0) { ring(eb, mul); shots--; subT = 0.4f; }
                if (timer <= 0) endMove(mul);
                break;
            case TELEPORT:
                // 瞬移（暗）：进入时已移动，短暂停顿
                vel.x = 0;
                if (timer <= 0) endMove(mul);
                break;
            case GUARD:
                // 防御（自然）：期间减伤
                vel.x = 0;
                if (timer <= 0) { guarding = false; endMove(mul); }
                break;
            default:
        }

        bounds.x += vel.x * dt;
        if (bounds.x < left) { bounds.x = left; if (state == DASH || state == APPROACH) dashDir = 1; }
        if (bounds.x + bounds.width > right) { bounds.x = right - bounds.width; if (state == DASH || state == APPROACH) dashDir = -1; }
    }

    private void startNext(Player p, Array<EBullet> eb, float left, float right) {
        state = seq[seqI];
        seqI = (seqI + 1) % seq.length;
        float mul = enraged() ? 1.5f : 1f;
        switch (state) {
            case APPROACH: timer = 1.0f / mul; break;
            case FAN:      fan(eb, p, mul); timer = 0.5f; break;
            case VOLLEY:   shots = enraged() ? 5 : 3; subT = 0f; timer = 0.7f + shots * 0.18f; break;
            case DASH:     dashDir = p.cx() > cx() ? 1 : -1; timer = 0.45f; break;
            case SLAM:     didJump = false; waveDone = false; timer = 2.2f; break;
            case GFLIP:    p.gravitySign = -1f; flipStarted = true; fan(eb, p, mul); subT = 0.6f; timer = enraged() ? 3.0f : 4.0f; break;
            case WELL:     subT = 0f; timer = 2.2f; break;
            case PYRAMID:  shots = enraged() ? 3 : 2; subT = 0f; timer = 0.5f + shots * 0.5f; break;
            case LASER:    shots = enraged() ? 7 : 5; subT = 0f; timer = 0.3f + shots * 0.14f; break;
            case RAIN:     shots = enraged() ? 9 : 6; subT = 0f; timer = 0.3f + shots * 0.18f; break;
            case BURST:    shots = enraged() ? 4 : 2; subT = 0f; timer = 0.3f + shots * 0.4f; break;
            case TELEPORT: {
                float nx = (p.cx() > cx()) ? p.cx() + 50 : p.cx() - 50 - bounds.width;
                if (nx < left) nx = left;
                if (nx + bounds.width > right) nx = right - bounds.width;
                bounds.x = nx; bounds.y = groundTop; vel.set(0, 0);
                fan(eb, p, mul); timer = 0.5f;
                break;
            }
            case GUARD:    guarding = true; timer = 1.5f; break;
            default:       timer = 0.4f;
        }
    }

    private void endMove(float mul) { state = IDLE; vel.x = 0; timer = 0.45f / mul; }

    private void fan(Array<EBullet> eb, Player p, float mul) {
        float base = (float) Math.atan2(p.cy() - cy(), p.cx() - cx());
        int n = enraged() ? 5 : 3;
        for (int i = 0; i < n; i++) {
            float a = base + (i - (n - 1) / 2f) * 0.26f;
            eb.add(new EBullet(cx() - 3, cy() - 3, (float) Math.cos(a), (float) Math.sin(a), K.EB_SPEED));
        }
    }

    private void aimed(Array<EBullet> eb, Player p, float mul) {
        eb.add(new EBullet(cx() - 3, cy() - 3, p.cx() - cx(), p.cy() - cy(), K.EB_SPEED * 1.6f * mul));
    }

    private void ring(Array<EBullet> eb, float mul) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            float a = (float) (i * Math.PI * 2.0 / n);
            eb.add(new EBullet(cx() - 3, cy() - 3, (float) Math.cos(a), (float) Math.sin(a), K.EB_SPEED * mul));
        }
    }
}

/* ===================== 玩家（含蓄力射击 + 重力反转） ===================== */
class Player extends Entity {
    int facing = 1;
    float gravitySign = 1f;   // 1=正常向下，-1=重力反转（被拉向天花板）
    boolean onGround = false;
    private int jumpsLeft = K.P_MAX_JUMPS;

    private boolean dashing = false;
    private float dashTimer = 0f, dashCd = 0f;

    private float chargeT = 0f;
    private boolean prevShoot = false;

    int hp = K.P_MAX_HP;
    private float invuln = 0f, hurtLock = 0f;

    int firedLevel = -1;
    boolean jumped = false, dashed = false;
    int chargeBeep = 0;

    Player(float x, float y) { bounds.set(x, y, K.P_W, K.P_H); }

    boolean isDashing() { return dashing; }
    boolean isInvulnerable() { return invuln > 0; }
    boolean isCharging() { return chargeT > 0.12f; }
    float chargeT() { return chargeT; }
    int chargeLevel() { return chargeT >= K.CHARGE_L2 ? 2 : chargeT >= K.CHARGE_L1 ? 1 : 0; }

    void resetForPhase(float x, float y) {
        bounds.setPosition(x, y);
        vel.set(0, 0);
        hp = K.P_MAX_HP;
        alive = true;
        dashing = false; dashTimer = 0; dashCd = 0;
        chargeT = 0; prevShoot = false;
        invuln = 0; hurtLock = 0;
        jumpsLeft = K.P_MAX_JUMPS;
        facing = 1;
        gravitySign = 1f;
    }

    void update(float dt, Level level, Array<Bullet> bullets) {
        firedLevel = -1; jumped = false; dashed = false; chargeBeep = 0;

        if (dashCd > 0)   dashCd -= dt;
        if (invuln > 0)   invuln -= dt;
        if (hurtLock > 0) hurtLock -= dt;
        boolean locked = hurtLock > 0;

        boolean left  = key(Keys.A, Keys.LEFT);
        boolean right = key(Keys.D, Keys.RIGHT);
        boolean jump  = keyJust(Keys.SPACE, Keys.K);
        boolean dash  = keyJust(Keys.SHIFT_LEFT, Keys.L);
        boolean shoot = key(Keys.J, Keys.ENTER);

        if (!locked && dash && !dashing && dashCd <= 0) {
            dashing = true; dashTimer = K.DASH_TIME; dashCd = K.DASH_CD;
            vel.x = facing * K.DASH_SPEED; vel.y = 0; dashed = true;
        }

        if (dashing) {
            dashTimer -= dt;
            vel.x = facing * K.DASH_SPEED;
            vel.y = 0;
            if (dashTimer <= 0) dashing = false;
        } else {
            float move = 0;
            if (!locked) {
                if (left)  { move -= 1; facing = -1; }
                if (right) { move += 1; facing = 1; }
            }
            vel.x = move * K.P_MOVE;
            if (!locked && jump && jumpsLeft > 0) { vel.y = K.P_JUMP * gravitySign; jumpsLeft--; onGround = false; jumped = true; }
            vel.y += K.GRAVITY * gravitySign * dt;
            if (gravitySign > 0 && vel.y < K.MAX_FALL) vel.y = K.MAX_FALL;
            if (gravitySign < 0 && vel.y > -K.MAX_FALL) vel.y = -K.MAX_FALL;
        }

        if (locked) {
            chargeT = 0;
            prevShoot = false;
        } else {
            if (shoot) {
                float before = chargeT;
                chargeT += dt;
                if (before < K.CHARGE_L1 && chargeT >= K.CHARGE_L1) chargeBeep = 1;
                if (before < K.CHARGE_L2 && chargeT >= K.CHARGE_L2) chargeBeep = 2;
            }
            if (prevShoot && !shoot && chargeT >= K.SHOOT_TAP) {
                int lvl = chargeLevel();
                fire(lvl, bullets);
                firedLevel = lvl;
                chargeT = 0;
            } else if (!shoot) {
                chargeT = 0;
            }
            prevShoot = shoot;
        }

        moveAndCollide(dt, level);
        if (onGround) jumpsLeft = K.P_MAX_JUMPS;
        if (bounds.y < -40) { hp = 0; alive = false; }
    }

    private void fire(int lvl, Array<Bullet> bullets) {
        float bw = lvl == 2 ? 22 : lvl == 1 ? 14 : 8;
        float bh = lvl == 2 ? 13 : lvl == 1 ? 9 : 4;
        float bx = facing > 0 ? bounds.x + bounds.width : bounds.x - bw;
        float by = bounds.y + bounds.height * 0.5f - bh / 2f;
        bullets.add(new Bullet(bx, by, facing, lvl));
    }

    private void moveAndCollide(float dt, Level level) {
        bounds.x += vel.x * dt;
        for (Rectangle s : level.solids) {
            if (s.overlaps(bounds)) {
                if (vel.x > 0)      bounds.x = s.x - bounds.width;
                else if (vel.x < 0) bounds.x = s.x + s.width;
                vel.x = 0;
                if (dashing) { dashing = false; dashTimer = 0; }
            }
        }
        if (bounds.x < 0) bounds.x = 0;
        if (bounds.x + bounds.width > level.width) bounds.x = level.width - bounds.width;

        bounds.y += vel.y * dt;
        onGround = false;
        for (Rectangle s : level.solids) {
            if (s.overlaps(bounds)) {
                if (vel.y <= 0) { bounds.y = s.y + s.height; if (gravitySign > 0) onGround = true; }
                else            { bounds.y = s.y - bounds.height; if (gravitySign < 0) onGround = true; }
                vel.y = 0;
            }
        }
        // 世界上下边界（重力反转时以天花板为落脚点）
        if (bounds.y < 0) { bounds.y = 0; vel.y = 0; if (gravitySign > 0) onGround = true; }
        if (bounds.y + bounds.height > K.VH) { bounds.y = K.VH - bounds.height; vel.y = 0; if (gravitySign < 0) onGround = true; }
    }

    void damage(int dmg, int knockDir) {
        if (invuln > 0 || !alive) return;
        hp -= dmg;
        invuln = 1.0f; hurtLock = 0.25f;
        vel.y = 200; vel.x = knockDir * 110;
        dashing = false; chargeT = 0;
        if (hp <= 0) { hp = 0; alive = false; }
    }

    private static boolean key(int a, int b) { return Gdx.input.isKeyPressed(a) || Gdx.input.isKeyPressed(b); }
    private static boolean keyJust(int a, int b) { return Gdx.input.isKeyJustPressed(a) || Gdx.input.isKeyJustPressed(b); }
}

/* ===================== 关卡（8 关：小兵关 + 对应 Boss） ===================== */
class Level {
    static final int MINION = 0, BOSS = 1;

    final Array<Rectangle> solids = new Array<>();
    final Array<Enemy> enemies = new Array<>();
    Boss boss;
    Rectangle door;
    int type;
    float width, height, spawnX, spawnY;

    static Level build(int stage, int phase) {
        Level L = new Level();
        L.height = K.VH;
        if (phase == MINION) {
            L.type = MINION;
            if (stage % 2 == 0) L.buildMinionA(); else L.buildMinionB();
            L.door = new Rectangle(L.width - 40, 24, 24, 48);
        } else {
            L.type = BOSS;
            L.buildBossArena();
            L.boss = new Boss(L.width / 2f - 14, 24, Boss.HP[stage], stage);
        }
        return L;
    }

    private void solid(float x, float y, float w, float h) { solids.add(new Rectangle(x, y, w, h)); }

    private void buildMinionA() {
        width = 1500f;
        solid(0, 0, width, 24);
        solid(180, 60, 90, 16); solid(320, 100, 80, 16); solid(470, 70, 70, 16);
        solid(620, 120, 100, 16); solid(800, 80, 80, 16); solid(980, 120, 110, 16);
        solid(1180, 80, 90, 16); solid(1330, 120, 110, 16);
        enemies.add(Enemy.walker(360, 24, 300, 460));
        enemies.add(Enemy.walker(840, 96, 800, 880));
        enemies.add(Enemy.shooter(700, 24));
        enemies.add(Enemy.shooter(1200, 96));
        enemies.add(Enemy.flyer(560, 150));
        enemies.add(Enemy.flyer(1050, 160));
        spawnX = 40; spawnY = 40;
    }

    private void buildMinionB() {
        width = 1700f;
        solid(0, 0, width, 24);
        solid(160, 80, 70, 16); solid(300, 120, 70, 16); solid(440, 70, 70, 16);
        solid(560, 120, 70, 16); solid(700, 90, 80, 16); solid(860, 130, 90, 16);
        solid(1030, 90, 80, 16); solid(1180, 130, 90, 16); solid(1340, 90, 80, 16);
        solid(1500, 120, 110, 16);
        enemies.add(Enemy.walker(320, 24, 260, 420));
        enemies.add(Enemy.walker(760, 24, 700, 900));
        enemies.add(Enemy.walker(1240, 24, 1180, 1380));
        enemies.add(Enemy.shooter(480, 86));
        enemies.add(Enemy.shooter(900, 146));
        enemies.add(Enemy.shooter(1360, 106));
        enemies.add(Enemy.flyer(600, 160));
        enemies.add(Enemy.flyer(1080, 170));
        enemies.add(Enemy.flyer(1520, 165));
        spawnX = 40; spawnY = 40;
    }

    private void buildBossArena() {
        width = K.VW;
        solid(0, 0, width, 24);
        solid(40, 74, 70, 14);
        solid(width - 110, 74, 70, 14);
        solid(width / 2f - 35, 132, 70, 14);
        spawnX = 30; spawnY = 30;
    }
}

/* ===================== 游戏屏幕（选关 + 8 关流程） ===================== */
class GameScreen extends ScreenAdapter {
    private enum State { SELECT, PLAYING, TRANSITION, GAMEOVER, WIN }

    private final OrthographicCamera camera = new OrthographicCamera();
    private final OrthographicCamera hudCam = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(K.VW, K.VH, camera);
    private final GlyphLayout layout = new GlyphLayout();

    private SpriteBatch batch;
    private Assets assets;
    private Sfx sfx;
    private Level level;
    private Player player;
    private final Array<Bullet> playerBullets = new Array<>();
    private final Array<EBullet> enemyBullets = new Array<>();

    private State state = State.SELECT;
    private int currentStage = 0, phase = 0, lives = K.P_LIVES;
    private final boolean[] cleared = new boolean[8];
    private int cursor = 0;
    private float transT = 0f;
    private String banner = "";
    private float shakeT = 0f, shakeMag = 0f;
    private float autoExit = -1f, elapsed = 0f;

    @Override
    public void show() {
        batch = new SpriteBatch();
        assets = new Assets();
        assets.load();
        sfx = new Sfx();
        sfx.load();
        if (sfx.bgm != null) sfx.bgm.play();
        hudCam.setToOrtho(false, K.VW, K.VH);
        player = new Player(0, 0);

        String ae = null;
        try { ae = System.getProperty("game.autoexit"); } catch (Exception ignored) {}
        if (ae != null) { try { autoExit = Float.parseFloat(ae); } catch (NumberFormatException ignored) {} }

        lives = K.P_LIVES;
        state = State.SELECT;
    }

    private void goToPhase(int ph, String text) {
        phase = ph;
        level = Level.build(currentStage, ph);
        player.resetForPhase(level.spawnX, level.spawnY);
        playerBullets.clear();
        enemyBullets.clear();
        snapCamera();
        banner = text; transT = 1.3f; state = State.TRANSITION;
    }

    private void goToStage(int idx) {
        currentStage = idx;
        goToPhase(Level.MINION, "STAGE " + (idx + 1) + "\n" + Boss.NAMES[idx]);
    }

    private void addShake(float mag, float t) { shakeMag = mag; shakeT = t; }

    @Override public void resize(int w, int h) { viewport.update(w, h, false); }

    @Override
    public void render(float delta) { update(delta); draw(); }

    private boolean kj(int k) { return Gdx.input.isKeyJustPressed(k); }

    private void update(float dt) {
        if (dt > 0.05f) dt = 0.05f;
        elapsed += dt;
        if (autoExit > 0 && elapsed >= autoExit) { Gdx.app.exit(); return; }

        switch (state) {
            case SELECT:     updateSelect(); break;
            case PLAYING:    updatePlaying(dt); break;
            case TRANSITION: transT -= dt; if (transT <= 0) state = State.PLAYING; break;
            case GAMEOVER:
                if (kj(Keys.R)) { lives = K.P_LIVES; state = State.SELECT; }
                break;
            case WIN:
                if (kj(Keys.R)) { lives = K.P_LIVES; for (int i = 0; i < 8; i++) cleared[i] = false; cursor = 0; state = State.SELECT; }
                break;
            default:
        }
    }

    private void updateSelect() {
        int col = cursor % 4, row = cursor / 4;
        if (kj(Keys.LEFT) || kj(Keys.A))  col = (col + 3) % 4;
        if (kj(Keys.RIGHT) || kj(Keys.D)) col = (col + 1) % 4;
        if (kj(Keys.UP) || kj(Keys.W))    row = 0;
        if (kj(Keys.DOWN) || kj(Keys.S))  row = 1;
        cursor = row * 4 + col;
        if (kj(Keys.J) || kj(Keys.ENTER) || kj(Keys.SPACE)) {
            sfx.play(sfx.doorIn, 0.6f);
            goToStage(cursor);
        }
    }

    private void updatePlaying(float dt) {
        if (shakeT > 0) shakeT -= dt;
        int prevHp = player.hp;

        player.update(dt, level, playerBullets);
        if (player.firedLevel == 0) sfx.play(sfx.shoot, 0.5f);
        else if (player.firedLevel > 0) sfx.play(sfx.charged, 0.6f);
        if (player.jumped) sfx.play(sfx.jump, 0.4f);
        if (player.dashed) sfx.play(sfx.dash, 0.4f);
        if (player.chargeBeep > 0) sfx.play(sfx.charge, 0.3f);

        if (!player.alive) { afterDamage(prevHp); onPlayerDeath(); return; }

        int ebBefore = enemyBullets.size;
        for (Enemy e : level.enemies) e.update(dt, player, enemyBullets, level.width);
        if (level.boss != null && level.boss.alive) {
            level.boss.update(dt, player, enemyBullets, 8, level.width - 8);
            if (level.boss.slamLanded) { addShake(5f, 0.3f); sfx.play(sfx.explode, 0.4f); }
            if (level.boss.flipStarted) { addShake(4f, 0.3f); sfx.play(sfx.doorIn, 0.6f); }
        }
        if (enemyBullets.size > ebBefore) sfx.play(sfx.enemyShot, 0.3f);

        for (Bullet b : playerBullets) {
            b.update(dt, level);
            if (!b.alive) continue;
            for (Enemy e : level.enemies) {
                if (e.alive && e.bounds.overlaps(b.bounds)) {
                    e.alive = false;
                    sfx.play(sfx.explode, 0.35f);
                    if (!b.pierce) { b.alive = false; break; }
                }
            }
            if (b.alive && level.boss != null && level.boss.alive && level.boss.bounds.overlaps(b.bounds)) {
                int dmg = level.boss.guarding ? Math.max(1, b.dmgBoss / 3) : b.dmgBoss;
                level.boss.hp -= dmg;
                b.alive = false;
                sfx.play(sfx.hurt, 0.3f);
                if (level.boss.hp <= 0) {
                    level.boss.alive = false;
                    player.gravitySign = 1f;
                    sfx.play(sfx.explode, 0.8f);
                    addShake(7f, 0.5f);
                }
            }
        }
        removeDead(playerBullets);

        for (EBullet e : enemyBullets) {
            e.update(dt, level);
            if (!e.alive) continue;
            if (player.alive && player.bounds.overlaps(e.bounds)) {
                player.damage(K.EB_DMG, player.cx() < e.cx() ? -1 : 1);
                e.alive = false;
            }
        }
        removeDeadE(enemyBullets);

        for (Enemy e : level.enemies) {
            if (e.alive && e.bounds.overlaps(player.bounds))
                player.damage(K.E_DMG, player.cx() < e.cx() ? -1 : 1);
        }
        if (level.boss != null && level.boss.alive && level.boss.bounds.overlaps(player.bounds))
            player.damage(K.BOSS_TOUCH_DMG, player.cx() < level.boss.cx() ? -1 : 1);

        afterDamage(prevHp);
        if (!player.alive) { onPlayerDeath(); return; }

        followCamera();

        if (level.type == Level.MINION) {
            if (level.door != null && player.bounds.overlaps(level.door)) clearMinion();
        } else {
            if (level.boss == null || !level.boss.alive) clearBoss();
        }
    }

    private void afterDamage(int prevHp) {
        if (player.hp < prevHp) sfx.play(sfx.hurt, 0.5f);
    }

    private void onPlayerDeath() {
        addShake(6f, 0.4f);
        player.gravitySign = 1f;
        lives--;
        if (lives < 0) state = State.GAMEOVER;
        else goToPhase(phase, "LIFE LOST  -  " + lives + " LEFT");
    }

    private void clearMinion() {
        sfx.play(sfx.doorIn, 0.6f);
        goToPhase(Level.BOSS, "WARNING !!\n" + Boss.NAMES[currentStage]);
    }

    private void clearBoss() {
        sfx.play(sfx.clear, 0.7f);
        cleared[currentStage] = true;
        boolean all = true;
        for (boolean c : cleared) if (!c) all = false;
        state = all ? State.WIN : State.SELECT;
    }

    private void removeDead(Array<Bullet> a) { for (int i = a.size - 1; i >= 0; i--) if (!a.get(i).alive) a.removeIndex(i); }
    private void removeDeadE(Array<EBullet> a) { for (int i = a.size - 1; i >= 0; i--) if (!a.get(i).alive) a.removeIndex(i); }

    private void followCamera() {
        float halfW = K.VW / 2f;
        float cx = player.bounds.x + player.bounds.width / 2f;
        cx = Math.max(halfW, Math.min(cx, level.width - halfW));
        float ox = 0, oy = 0;
        if (shakeT > 0) {
            float f = Math.min(1f, shakeT / 0.3f) * shakeMag;
            ox = (float) (Math.random() * 2 - 1) * f;
            oy = (float) (Math.random() * 2 - 1) * f;
        }
        camera.position.x = cx + ox;
        camera.position.y = K.VH / 2f + oy;
        camera.update();
    }

    private void snapCamera() {
        camera.position.x = Math.min(Math.max(K.VW / 2f, level.spawnX), level.width - K.VW / 2f);
        camera.position.y = K.VH / 2f;
        camera.update();
    }

    /* ===================== 渲染 ===================== */
    private void draw() {
        ScreenUtils.clear(0.06f, 0.06f, 0.1f, 1f);

        if (state == State.SELECT) { drawSelect(); return; }

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        drawBackground();
        batch.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        for (Rectangle s : level.solids) batch.draw(assets.tile, s.x, s.y, s.width, s.height);
        drawDoor();
        for (Enemy e : level.enemies)
            if (e.alive) batch.draw(e.tex(assets), e.bounds.x, e.bounds.y, e.bounds.width, e.bounds.height);
        if (level.boss != null && level.boss.alive) {
            if (level.boss.guarding) batch.setColor(0.7f, 0.9f, 1f, 1f);
            batch.draw(assets.bosses[level.boss.bossType], level.boss.bounds.x, level.boss.bounds.y, level.boss.bounds.width, level.boss.bounds.height);
            batch.setColor(Color.WHITE);
        }
        for (EBullet e : enemyBullets) batch.draw(assets.ebullet, e.bounds.x, e.bounds.y, e.bounds.width, e.bounds.height);
        for (Bullet b : playerBullets) { b.tint(batch); batch.draw(assets.bullet, b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height); }
        batch.setColor(Color.WHITE);
        drawPlayer();
        batch.end();

        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        drawHud();
        if (state == State.TRANSITION) drawCenterBanner(banner, 0.55f);
        if (state == State.GAMEOVER)   drawCenterBanner("GAME OVER\nPRESS R", 0.7f);
        if (state == State.WIN)        drawCenterBanner("ALL 8 CLEAR !\nPRESS R", 0.7f);
        batch.end();
    }

    private void drawSelect() {
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(assets.bgFar, 0, 0, K.VW, K.VH);

        assets.font.getData().setScale(0.8f);
        layout.setText(assets.font, "STAGE SELECT", Color.WHITE, K.VW, Align.center, false);
        assets.font.draw(batch, layout, 0, K.VH - 14);
        assets.font.getData().setScale(0.4f);

        float[] colX = {40, 150, 260, 370};
        float[] rowY = {150, 74};
        float iw = 48, ih = 52;
        for (int i = 0; i < 8; i++) {
            float ix = colX[i % 4], iy = rowY[i / 4];
            if (i == cursor) {
                float a = 0.5f + 0.5f * (float) Math.sin(elapsed * 8);
                batch.setColor(1f, 1f, 0.3f, 0.9f * a);
                batch.draw(assets.pixel, ix - 3, iy - 3, iw + 6, ih + 6);
            }
            batch.setColor(0.10f, 0.10f, 0.16f, 1f);
            batch.draw(assets.pixel, ix - 1, iy - 1, iw + 2, ih + 2);
            batch.setColor(Color.WHITE);
            batch.draw(assets.bosses[i], ix, iy, iw, ih);

            layout.setText(assets.font, Boss.NAMES[i], Color.WHITE, iw, Align.center, false);
            assets.font.draw(batch, layout, ix, iy - 4);

            if (cleared[i]) {
                batch.setColor(0.2f, 1f, 0.4f, 1f);
                batch.draw(assets.pixel, ix + iw - 9, iy + ih - 9, 8, 8);
                batch.setColor(Color.WHITE);
            }
        }
        layout.setText(assets.font, "ARROWS: MOVE     J / ENTER: FIGHT", Color.WHITE, K.VW, Align.center, false);
        assets.font.draw(batch, layout, 0, 26);
        batch.end();
    }

    private void drawDoor() {
        if (level.door == null) return;
        Rectangle d = level.door;
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsed * 6);
        batch.setColor(0.3f, 1f, 0.9f, 0.25f * pulse);
        batch.draw(assets.pixel, d.x - 3, d.y, d.width + 6, d.height);
        batch.setColor(Color.WHITE);
        batch.draw(assets.door, d.x, d.y, d.width, d.height);
    }

    private void drawPlayer() {
        if (!player.alive) return;
        if (player.isCharging()) {
            int lv = player.chargeLevel();
            float r = 3 + Math.min(player.chargeT(), 1.1f) * 9;
            float gx = player.facing > 0 ? player.bounds.x + player.bounds.width : player.bounds.x;
            float gy = player.bounds.y + player.bounds.height * 0.5f;
            float a = 0.4f + 0.4f * (float) Math.sin(elapsed * 25);
            if (lv == 2)      batch.setColor(1f, 0.4f, 1f, a);
            else if (lv == 1) batch.setColor(0.4f, 0.95f, 1f, a);
            else              batch.setColor(1f, 1f, 1f, a * 0.6f);
            batch.draw(assets.pixel, gx - r / 2, gy - r / 2, r, r);
            batch.setColor(Color.WHITE);
        }
        boolean blink = player.isInvulnerable() && ((int) (elapsed * 20) % 2 == 0);
        if (!blink) batch.draw(assets.player, player.bounds.x, player.bounds.y, player.bounds.width, player.bounds.height);
    }

    private void drawBackground() {
        batch.setColor(Color.WHITE);
        batch.draw(assets.bgFar, 0, 0, K.VW, K.VH);
        float tile = 120f;
        float scroll = (camera.position.x * 0.5f) % tile;
        if (scroll < 0) scroll += tile;
        int count = (int) (K.VW / tile) + 3;
        for (int i = -1; i < count; i++) batch.draw(assets.bgNear, i * tile - scroll, 24, 80, 70);
    }

    private void drawHud() {
        float x = 8, y = K.VH - 10;
        assets.font.getData().setScale(0.4f);

        float ratio = Math.max(0f, player.hp / (float) K.P_MAX_HP);
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(assets.pixel, x - 1, y - 1, 82, 8);
        batch.setColor(0.2f, 1f, 0.45f, 1f);
        batch.draw(assets.pixel, x, y, 80 * ratio, 6);
        batch.setColor(Color.WHITE);

        assets.font.draw(batch, "HP", x, y + 18);
        assets.font.draw(batch, "LIVES: " + lives, x, y - 6);
        String st = "STAGE " + (currentStage + 1) + "  " + Boss.NAMES[currentStage]
                + (phase == Level.BOSS ? " (BOSS)" : " (MINIONS)");
        assets.font.draw(batch, st, x + 86, y - 6);
        assets.font.draw(batch, "A/D move  SPACE/K jump x2  L dash  HOLD J charge", x, 14);

        if (level.boss != null && level.boss.alive) {
            float bw = 160f, bx = (K.VW - bw) / 2f, by = K.VH - 18;
            float r = Math.max(0f, level.boss.hp / (float) level.boss.maxHp);
            batch.setColor(0f, 0f, 0f, 0.6f);
            batch.draw(assets.pixel, bx - 1, by - 1, bw + 2, 8);
            batch.setColor(level.boss.enraged() ? 1f : 0.9f, 0.3f, 0.3f, 1f);
            batch.draw(assets.pixel, bx, by, bw * r, 6);
            batch.setColor(Color.WHITE);
            assets.font.draw(batch, Boss.NAMES[level.boss.bossType] + (level.boss.enraged() ? "  (ENRAGED)" : ""), bx, by + 16);
        }

        if (player.gravitySign < 0) {
            assets.font.getData().setScale(0.6f);
            layout.setText(assets.font, "!!  GRAVITY REVERSED  !!", Color.valueOf("9CFFEF"), K.VW, Align.center, false);
            assets.font.draw(batch, layout, 0, K.VH - 40);
            assets.font.getData().setScale(0.4f);
        }
    }

    private void drawCenterBanner(String text, float dimAlpha) {
        batch.setColor(0f, 0f, 0f, dimAlpha);
        batch.draw(assets.pixel, 0, 0, K.VW, K.VH);
        batch.setColor(Color.WHITE);
        assets.font.getData().setScale(0.9f);
        layout.setText(assets.font, text, Color.WHITE, K.VW, Align.center, true);
        assets.font.draw(batch, layout, 0, K.VH / 2f + layout.height / 2f);
        assets.font.getData().setScale(0.4f);
    }

    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (assets != null) assets.dispose();
        if (sfx != null) sfx.dispose();
    }
}
