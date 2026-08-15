package com.rojama.pianoshelf;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Mutopia Project 目录解析器。
 *
 * Mutopia 结构非常规整（基于 CGI + LilyPond 源码归档，2124 首作品，每首都有 MusicXML / PDF / MIDI）：
 *   - 作曲家：    https://www.mutopiaproject.org/composers.html    → 列表 <a href="make-table.cgi?Composer=BachJS">[N]</a>
 *   - 乐器：      https://www.mutopiaproject.org/instruments.html  → 列表 <a href="make-table.cgi?Instrument=Piano">[N]</a>
 *   - 风格：      首页内链接 → make-table.cgi?Style=Baroque / Classical / Folk ...
 *   - 作品列表：  make-table.cgi?Composer=xxx → HTML 表格每行 = 一个作品，含 piece-info.cgi?id=NNNN 详情
 *   - 详情页：    piece-info.cgi?id=NNNN      → 解析出 *-musicxml.xml.gz 或 *.xml.gz 或直接 *.xml 直链
 *
 * Mutopia 文件命名 (LilyPond 导出的归档格式，稳定可预测)：
 *   {base}/piece-info.cgi?id=XXXX
 *   实际文件位于 https://www.mutopiaproject.org/ftp/.../PieceName-Piano/...-musicxml.xml.gz
 *
 * 本类所有解析均为轻量级正则（不引入 jsoup，减小 APK 体积），
 * 并在后台线程调用（OkHttp），主线程通过回调接收结果（JSON 便于 Debug 及后续扩展）。
 */
public class MutopiaCatalogParser {

    public static final String BASE = "https://www.mutopiaproject.org";
    public static final String URL_COMPOSERS = BASE + "/composers.html";
    public static final String URL_INSTRUMENTS = BASE + "/instruments.html";
    public static final String URL_STYLES = BASE + "/browse.html";

    // 目录项（作曲家 / 乐器 / 风格 通用）
    public static class CategoryItem {
        public final String name;       // 展示名，例如 "J. S. Bach"
        public final String subtitle;   // 可选副标题，例如 "(1685–1750) [123]"
        public final String countStr;   // 原始方括号计数，例如 "[123]"
        public final String url;        // make-table.cgi?xxx 完整 URL

        public CategoryItem(String name, String subtitle, String countStr, String url) {
            this.name = name;
            this.subtitle = subtitle;
            this.countStr = countStr;
            this.url = url;
        }
    }

    // 作品项（从 make-table 解析）
    public static class PieceItem {
        public final String title;
        public final String composer;
        public final String instrumentation; // 例如 "Piano"
        public final String style;           // 例如 "Baroque"
        public final String infoUrl;         // piece-info.cgi?id=2247 完整 URL
        public final String rawPieceUrl;     // 详情页内的相对 piece 路径（如果有）

        public PieceItem(String title, String composer, String instrumentation,
                         String style, String infoUrl, String rawPieceUrl) {
            this.title = title;
            this.composer = composer;
            this.instrumentation = instrumentation;
            this.style = style;
            this.infoUrl = infoUrl;
            this.rawPieceUrl = rawPieceUrl;
        }

        /** 转为 JSON 便于调试 */
        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("title", title);
                o.put("composer", composer);
                o.put("instrumentation", instrumentation);
                o.put("style", style);
                o.put("infoUrl", infoUrl);
                o.put("rawPieceUrl", rawPieceUrl);
            } catch (JSONException ignored) {}
            return o;
        }
    }

    // ------------------------------------------------------------------
    // 回调接口（结果通过 handler 或调用方自行 post 主线程）
    // ------------------------------------------------------------------

    public interface Callback<T> {
        void onSuccess(T result);
        void onFailure(String message);
    }

    // ------------------------------------------------------------------
    // 共享 OkHttp (复用 OnlineScoreDownloader 单例 client；若未来解耦也可本地自建)
    // ------------------------------------------------------------------

    private static OkHttpClient client() {
        // 通过反射 / 新建都行；这里直接 new 一个小实例（parser 流量小，单独实例问题不大）
        return new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    private static String get(String url) throws IOException {
        Request req = new Request.Builder().url(url)
                .header("User-Agent", "PianoShelf-Android/2.0 (compat; +https://www.mutopiaproject.org/)")
                .build();
        try (Response resp = client().newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("HTTP " + resp.code());
            }
            return resp.body() == null ? "" : resp.body().string();
        }
    }

    // 把相对 URL 补全为绝对 URL
    private static String abs(String href) {
        if (href == null) return "";
        String h = href.trim();
        if (h.startsWith("http://") || h.startsWith("https://")) return h;
        if (h.startsWith("/")) return BASE + h;
        // CGI 文件可能和当前页同级
        if (h.startsWith("make-table.cgi") || h.startsWith("piece-info.cgi")
                || h.startsWith("cgibin/")) {
            // Mutopia 站点 CGI 实际位于 cgibin 目录下；但首页 HTML 中链接写的是 make-table.cgi 相对
            // 真实可访问形式是 /cgibin/make-table.cgi?...  但站点会做重定向，故 BASE + "/cgibin/" 或直接 BASE + "/" 都试
            // 这里做保守处理：若 href 不含 cgibin 前缀，则自动加一层
            if (h.startsWith("cgibin/")) {
                return BASE + "/" + h;
            }
            return BASE + "/cgibin/" + h;
        }
        return BASE + "/" + h;
    }

    // ------------------------------------------------------------------
    // 1) 解析分类列表：作曲家 / 乐器 / 风格
    // ------------------------------------------------------------------

    /**
     * 从 HTML 中抓取所有锚点，链接目标中包含 keyword 的作为分类项。
     * 例如 composers.html 中所有 href = "make-table.cgi?Composer=xxx" 的 <a>。
     */
    private static List<CategoryItem> parseCategoryList(String html, String keywordInHref) {
        List<CategoryItem> out = new ArrayList<>();
        if (TextUtils.isEmpty(html)) return out;

        // <a href="...">文本</a> [数字] 的模式（紧随其后的计数方括号）
        Pattern aP = Pattern.compile(
                "<a\\s+[^>]*?href\\s*=\\s*[\"']([^\"']*\"'])[^>]*?>(.*?)</a>\\s*(?:\\[([0-9]+)\\])?",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = aP.matcher(html);
        while (m.find()) {
            String href = m.group(1);
            String text = m.group(2);
            String count = m.group(3) == null ? "" : m.group(3);
            if (href == null || text == null) continue;
            href = href.replace("\"", "").replace("'", "").trim();
            if (!href.toLowerCase().contains(keywordInHref.toLowerCase())) continue;
            text = stripHtml(text).trim();
            if (text.isEmpty()) continue;
            String name = text;
            String subtitle = "";
            // 有些含 "Bach, JS (1685–1750)" 或 "J. S. Bach (1685–1750)"，把 () 部分当 subtitle
            int paren = text.indexOf('(');
            if (paren > 0) {
                name = text.substring(0, paren).trim();
                subtitle = text.substring(paren).trim();
            }
            String countStr = count.isEmpty() ? "" : "[" + count + "]";
            out.add(new CategoryItem(name, subtitle, countStr, abs(href)));
        }
        return out;
    }

    // 剥离 <sub> <sup> <br> 等内联标签
    private static String stripHtml(String s) {
        String r = s.replaceAll("(?s)<br\\s*/?>", " ");
        r = r.replaceAll("(?s)<[^>]+>", "");
        r = r.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        return r;
    }

    public static void loadComposers(final Callback<List<CategoryItem>> cb) {
        new Thread(new Runnable() { @Override public void run() {
            try {
                String html = get(URL_COMPOSERS);
                List<CategoryItem> list = parseCategoryList(html, "Composer=");
                Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
                cb.onSuccess(list);
            } catch (Throwable t) {
                cb.onFailure(t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
            }
        }}).start();
    }

    public static void loadInstruments(final Callback<List<CategoryItem>> cb) {
        new Thread(new Runnable() { @Override public void run() {
            try {
                String html = get(URL_INSTRUMENTS);
                List<CategoryItem> list = parseCategoryList(html, "Instrument=");
                Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
                cb.onSuccess(list);
            } catch (Throwable t) {
                cb.onFailure(t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
            }
        }}).start();
    }

    public static void loadStyles(final Callback<List<CategoryItem>> cb) {
        new Thread(new Runnable() { @Override public void run() {
            try {
                String html = get(URL_STYLES);
                List<CategoryItem> list = parseCategoryList(html, "Style=");
                Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
                cb.onSuccess(list);
            } catch (Throwable t) {
                cb.onFailure(t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
            }
        }}).start();
    }

    // ------------------------------------------------------------------
    // 2) 作品列表：parse make-table.cgi?Composer=BachJS
    // ------------------------------------------------------------------

    // Mutopia 的 make-table 输出典型结构：
    // <table ...>  <tr><th>Title</th><th>Composer</th><th>Instrument/Voices</th><th>Style</th><th>Opus</th>...</tr>
    //   <tr class="...">
    //     <td><a href="/cgi-bin/make-table.cgi?...">Title</a></td>
    //     <td>Composer</td>
    //     <td>Instrument</td>
    //     <td>Style</td>
    //     <td><a href="piece-info.cgi?id=XXXX">Opus / PieceName</a></td>   ← 详情页
    //   </tr>
    // </table>
    //
    // 简化解析策略：每一行抓取 5 列；其中最后一列含 piece-info 链接（或首列含）

    public static void loadPieces(final String categoryListUrl, final Callback<List<PieceItem>> cb) {
        new Thread(new Runnable() { @Override public void run() {
            try {
                String html = get(categoryListUrl);
                List<PieceItem> list = parsePieceTable(html);
                cb.onSuccess(list);
            } catch (Throwable t) {
                cb.onFailure(t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
            }
        }}).start();
    }

    private static List<PieceItem> parsePieceTable(String html) {
        List<PieceItem> out = new ArrayList<>();
        if (TextUtils.isEmpty(html)) return out;
        // 1) 提取 <table ...> ... </table> 主表（第一个含 piece-info 的 table）
        int tableStart = html.toLowerCase().indexOf("<table");
        int tableEnd = html.toLowerCase().indexOf("</table>");
        if (tableStart < 0 || tableEnd < 0 || tableEnd < tableStart) return out;
        String tableHtml = html.substring(tableStart, tableEnd + 8);

        // 2) 每 <tr ...> ... </tr> 一行
        Pattern trP = Pattern.compile("<tr[^>]*?>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher trM = trP.matcher(tableHtml);
        while (trM.find()) {
            String tr = trM.group(1);
            if (tr == null) continue;
            if (tr.toLowerCase().contains("<th")) continue; // 表头行跳过

            // 提取所有 <td ...> ... </td>
            List<String> tds = new ArrayList<>();
            Pattern tdP = Pattern.compile("<td[^>]*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher tdM = tdP.matcher(tr);
            while (tdM.find()) {
                String td = tdM.group(1);
                tds.add(td == null ? "" : td);
            }
            if (tds.size() < 3) continue;

            // 在所有 td 中寻找 piece-info.cgi?id=NNNN 链接（即详情页）
            String infoHref = "";
            for (String td : tds) {
                Pattern p = Pattern.compile(
                        "<a\\s+[^>]*?href\\s*=\\s*[\"']([^\"']*piece-info\\.cgi\\?id=[0-9]+[\"']?)",
                        Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(td);
                if (m.find()) {
                    infoHref = m.group(1);
                    break;
                }
            }
            if (infoHref.isEmpty()) continue;
            infoHref = infoHref.replace("\"", "").replace("'", "").trim();

            String title = "";
            // title 取首列纯文本；如果首列里有 piece-info 锚点，取其锚文本优先
            {
                String first = tds.get(0);
                Pattern p = Pattern.compile(
                        "<a[^>]*?href=[\"']([^\"']*piece-info[^\"']*[\"']?)[^>]*?>(.*?)</a>",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher m = p.matcher(first);
                if (m.find()) title = stripHtml(m.group(2) == null ? "" : m.group(2));
                if (title.isEmpty()) {
                    p = Pattern.compile("<a\\s+[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    m = p.matcher(first);
                    if (m.find()) title = stripHtml(m.group(1) == null ? "" : m.group(1));
                    if (title.isEmpty()) title = stripHtml(first);
                }
            }
            // composer 取第二列
            String composer = stripHtml(tds.get(1)).trim();
            // instrumentation 取第三列
            String instr = stripHtml(tds.get(2)).trim();
            String style = tds.size() >= 4 ? stripHtml(tds.get(3)).trim() : "";

            out.add(new PieceItem(title, composer, instr, style, abs(infoHref), ""));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 3) 详情页解析：piece-info.cgi?id=NNNN → MusicXML 直接下载 URL
    // ------------------------------------------------------------------
    //
    // Mutopia 详情页中文件链接典型形式：
    //   <a href="/ftp/BachJS/[...]/FugueinDmajor-Piano/FugueinDmajor-musicxml.xml.gz">MusicXML - compressed</a>
    //   <a href="/ftp/.../...-musicxml.xml">MusicXML</a>
    //   另外还有 PDF / MIDI / LilyPond 等
    //
    // 优先返回 musicxml.xml.gz（官方压缩版，OkHttp 自动解压 gzip，所以也可以直接取到内容），
    // 其次 -musicxml.xml，再次 *.xml.gz。

    public static void resolveMusicXmlUrl(final String pieceInfoUrl, final Callback<String> cb) {
        new Thread(new Runnable() { @Override public void run() {
            try {
                String html = get(pieceInfoUrl);
                String url = findBestMusicXmlUrl(html);
                if (url == null || url.isEmpty()) {
                    cb.onFailure("该作品未提供 MusicXML 格式");
                } else {
                    cb.onSuccess(url);
                }
            } catch (Throwable t) {
                cb.onFailure(t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
            }
        }}).start();
    }

    static String findBestMusicXmlUrl(String html) {
        if (TextUtils.isEmpty(html)) return null;
        // 找出所有 <a href="..."> 并保留链接
        Pattern p = Pattern.compile(
                "<a\\s+[^>]*?href\\s*=\\s*[\"']([^\"']+[\"']?)[^>]*?>(.*?)</a>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(html);

        String best1 = null;   // -musicxml.xml.gz
        String best2 = null;   // -musicxml.xml
        String best3 = null;   // *.xml.gz (非 .pdf.gz / .mid.gz)
        while (m.find()) {
            String href = m.group(1);
            String text = m.group(2);
            if (href == null) continue;
            href = href.replace("\"", "").replace("'", "").trim();
            text = text == null ? "" : stripHtml(text);
            String h = href.toLowerCase();
            if (!h.endsWith(".xml") && !h.endsWith(".xml.gz")) continue;

            String abs = abs(href);
            // 以锚文本包含 "MusicXML" 者优先
            if (h.endsWith("-musicxml.xml.gz") && best1 == null) best1 = abs;
            if (h.contains("-musicxml.xml") && !h.endsWith(".gz") && best2 == null) best2 = abs;
            if (best3 == null && h.endsWith(".xml.gz")
                    && !h.endsWith(".pdf.gz") && !h.endsWith(".mid.gz")) {
                best3 = abs;
            }
            // 如果锚文本明确写了 MusicXML，立即给最高优先级，即使后缀略模糊
            if (text.toLowerCase().contains("musicxml") && best1 == null) best1 = abs;
        }
        return best1 != null ? best1 : (best2 != null ? best2 : best3);
    }

    // ------------------------------------------------------------------
    // 调试工具：转为 JSONArray
    // ------------------------------------------------------------------

    public static JSONArray categoriesToJson(List<CategoryItem> list) {
        JSONArray a = new JSONArray();
        for (CategoryItem it : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("name", it.name);
                o.put("subtitle", it.subtitle);
                o.put("count", it.countStr);
                o.put("url", it.url);
            } catch (JSONException ignored) {}
            a.put(o);
        }
        return a;
    }

    public static JSONArray piecesToJson(List<PieceItem> list) {
        JSONArray a = new JSONArray();
        for (PieceItem p : list) a.put(p.toJson());
        return a;
    }
}
