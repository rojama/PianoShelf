package com.rojama.pianoshelf;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutopia Project 应用内目录浏览器。
 *
 * 顶层 3 个维度 Tab：
 *   - 作曲家 (Composer)
 *   - 乐器 (Instrument)
 *   - 风格 (Style)
 *
 * 点击分类行 → 列出该分类下所有作品 → 点击作品（或下载按钮）：
 *   1) 先解析详情页 resolveMusicXmlUrl，得到真实 MusicXML(.gz) 直链
 *   2) 交给 OnlineScoreDownloader 下载（缓存 + SHA-256 去重）
 *   3) 写入最近记录 → 调 GraphicsActivity 打开
 */
public class InAppBrowseActivity extends AppCompatActivity {
    private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";
    private static final String KEY_STACK = "browse_stack"; // 暂未用；此处保留

    public static final int DIM_COMPOSER = 0;
    public static final int DIM_INSTRUMENT = 1;
    public static final int DIM_STYLE = 2;

    private TabLayout tlDimension;
    private ListView listView;
    private LinearLayout llLoading;
    private TextView tvLoading;
    private TextView tvEmpty;
    private DatabaseHelper dbhelp;

    private int currentDim = DIM_COMPOSER;
    // 当前浏览状态：若 categoryUrl 非 null 表示在作品列表层；否则在分类列表层
    @Nullable private String currentCategoryUrl = null;
    @Nullable private String currentCategoryName = null;

    // 两份数据源 + 两个 adapter
    private final List<MutopiaCatalogParser.CategoryItem> cats = new ArrayList<>();
    private final List<MutopiaCatalogParser.PieceItem> pieces = new ArrayList<>();
    private CategoryAdapter catAdapter;
    private PieceAdapter pieceAdapter;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inapp_browse);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.browse_title);
        }
        dbhelp = new DatabaseHelper(this);

        tlDimension = findViewById(R.id.tl_dimension);
        listView = findViewById(R.id.lv_catalog);
        llLoading = findViewById(R.id.ll_loading);
        tvLoading = findViewById(R.id.tv_loading);
        tvEmpty = findViewById(R.id.tv_empty);

        catAdapter = new CategoryAdapter(this, cats);
        pieceAdapter = new PieceAdapter(this, pieces);
        listView.setAdapter(catAdapter); // 初始显示分类列表

        // 初始化 3 个 Tab
        tlDimension.addTab(tlDimension.newTab().setText(R.string.browse_tab_composer));
        tlDimension.addTab(tlDimension.newTab().setText(R.string.browse_tab_instrument));
        tlDimension.addTab(tlDimension.newTab().setText(R.string.browse_tab_style));
        tlDimension.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentDim = tab.getPosition();
                currentCategoryUrl = null; // 切换维度时回到分类层
                currentCategoryName = null;
                updateTitle();
                loadCategoryList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                // 重新拉取
                loadCategoryList();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentCategoryUrl == null) {
                    // 分类层 → 点分类进入作品列表
                    if (position < 0 || position >= cats.size()) return;
                    MutopiaCatalogParser.CategoryItem cat = cats.get(position);
                    openCategory(cat.name, cat.url);
                } else {
                    // 作品列表层 → 点行触发下载
                    if (position < 0 || position >= pieces.size()) return;
                    onPieceSelected(pieces.get(position));
                }
            }
        });

        loadCategoryList();
    }

    @Override
    public void onBackPressed() {
        if (currentCategoryUrl != null) {
            // 从作品列表层退回到分类列表层
            currentCategoryUrl = null;
            currentCategoryName = null;
            updateTitle();
            showCategoryList();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbhelp != null) {
            try { dbhelp.close(); } catch (Throwable ignored) {}
            dbhelp = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (currentCategoryUrl != null) {
                currentCategoryUrl = null;
                currentCategoryName = null;
                updateTitle();
                showCategoryList();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------
    // 数据加载
    // ------------------------------------------------------------------

    private void updateTitle() {
        if (getSupportActionBar() == null) return;
        if (currentCategoryUrl != null && currentCategoryName != null) {
            getSupportActionBar().setTitle(currentCategoryName);
            getSupportActionBar().setSubtitle(R.string.browse_subtitle_pieces);
            tlDimension.setVisibility(View.GONE); // 作品列表层隐藏 Tab，避免误切换
        } else {
            getSupportActionBar().setTitle(R.string.browse_title);
            getSupportActionBar().setSubtitle(null);
            tlDimension.setVisibility(View.VISIBLE);
        }
    }

    private void loadCategoryList() {
        showLoading(getString(R.string.browse_loading_cats));
        tvEmpty.setVisibility(View.GONE);
        cats.clear();
        catAdapter.notifyDataSetChanged();
        MutopiaCatalogParser.Callback<List<MutopiaCatalogParser.CategoryItem>> cb =
                new MutopiaCatalogParser.Callback<List<MutopiaCatalogParser.CategoryItem>>() {
            @Override public void onSuccess(List<MutopiaCatalogParser.CategoryItem> result) {
                final List<MutopiaCatalogParser.CategoryItem> r = result;
                mainHandler.post(new Runnable() { @Override public void run() {
                    cats.clear();
                    cats.addAll(r);
                    showCategoryList();
                }});
            }
            @Override public void onFailure(final String message) {
                mainHandler.post(new Runnable() { @Override public void run() {
                    hideLoading();
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.browse_load_fail) + "\n" + message);
                    Toast.makeText(InAppBrowseActivity.this,
                            R.string.browse_load_fail_toast, Toast.LENGTH_LONG).show();
                }});
            }
        };
        switch (currentDim) {
            case DIM_COMPOSER:   MutopiaCatalogParser.loadComposers(cb);   break;
            case DIM_INSTRUMENT: MutopiaCatalogParser.loadInstruments(cb); break;
            case DIM_STYLE:      MutopiaCatalogParser.loadStyles(cb);      break;
        }
    }

    private void openCategory(String name, String url) {
        currentCategoryName = name;
        currentCategoryUrl = url;
        updateTitle();
        showLoading(getString(R.string.browse_loading_pieces, name));
        tvEmpty.setVisibility(View.GONE);
        pieces.clear();
        pieceAdapter.notifyDataSetChanged();
        listView.setAdapter(pieceAdapter);
        MutopiaCatalogParser.loadPieces(url, new MutopiaCatalogParser.Callback<List<MutopiaCatalogParser.PieceItem>>() {
            @Override public void onSuccess(List<MutopiaCatalogParser.PieceItem> result) {
                final List<MutopiaCatalogParser.PieceItem> r = result;
                mainHandler.post(new Runnable() { @Override public void run() {
                    pieces.clear();
                    pieces.addAll(r);
                    pieceAdapter.notifyDataSetChanged();
                    hideLoading();
                    if (pieces.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(R.string.browse_empty_pieces);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                }});
            }
            @Override public void onFailure(final String message) {
                mainHandler.post(new Runnable() { @Override public void run() {
                    hideLoading();
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.browse_load_fail) + "\n" + message);
                    Toast.makeText(InAppBrowseActivity.this,
                            R.string.browse_load_fail_toast, Toast.LENGTH_LONG).show();
                }});
            }
        });
    }

    private void showCategoryList() {
        listView.setAdapter(catAdapter);
        catAdapter.notifyDataSetChanged();
        hideLoading();
        if (cats.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.browse_empty_cats);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showLoading(String text) {
        llLoading.setVisibility(View.VISIBLE);
        tvLoading.setText(text);
    }

    private void hideLoading() {
        llLoading.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------
    // 作品 → 下载流程
    // ------------------------------------------------------------------

    private void onPieceSelected(final MutopiaCatalogParser.PieceItem p) {
        Toast.makeText(this, getString(R.string.browse_resolving, p.title), Toast.LENGTH_SHORT).show();
        MutopiaCatalogParser.resolveMusicXmlUrl(p.infoUrl, new MutopiaCatalogParser.Callback<String>() {
            @Override public void onSuccess(final String musicXmlUrl) {
                mainHandler.post(new Runnable() { @Override public void run() {
                    downloadAndOpen(musicXmlUrl, p.title);
                }});
            }
            @Override public void onFailure(final String message) {
                mainHandler.post(new Runnable() { @Override public void run() {
                    Toast.makeText(InAppBrowseActivity.this,
                            getString(R.string.browse_resolve_fail) + message, Toast.LENGTH_LONG).show();
                }});
            }
        });
    }

    private void downloadAndOpen(final String url, final String titleLabel) {
        // 复用 OnlineScoreDownloader（带缓存、进度、主线程回调）
        final String toastLoading = getString(R.string.browse_dl_starting, titleLabel);
        Toast.makeText(this, toastLoading, Toast.LENGTH_LONG).show();

        OnlineScoreDownloader.downloadAsync(this, url, false, new OnlineScoreDownloader.DownloadCallback() {
            @Override public void onProgress(int percent) {
                // 此处可以挂全局面进度条；简单处理：不再弹 Toast，避免过频
            }

            @Override public void onSuccess(File localFile) {
                if (dbhelp != null) dbhelp.insertRecentItem(localFile.getAbsolutePath());
                try {
                    Uri uri = FileProvider.getUriForFile(InAppBrowseActivity.this,
                            FILEPROVIDER_AUTH, localFile);
                    Intent intent = new Intent();
                    intent.setDataAndType(uri, "application/vnd.recordare.musicxml");
                    intent.setClass(InAppBrowseActivity.this, GraphicsActivity.class);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra(GraphicsActivity.EXTRA_FILE_PATH, localFile.getAbsolutePath());
                    startActivity(intent);
                    Toast.makeText(InAppBrowseActivity.this,
                            R.string.online_open_ok, Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(InAppBrowseActivity.this,
                            getString(R.string.info_open_err) + " " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override public void onFailure(String message) {
                Toast.makeText(InAppBrowseActivity.this,
                        getString(R.string.online_status_fail) + "\n" + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ------------------------------------------------------------------
    // Adapters
    // ------------------------------------------------------------------

    private static class CategoryAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<MutopiaCatalogParser.CategoryItem> list;
        CategoryAdapter(Context c, List<MutopiaCatalogParser.CategoryItem> l) { ctx = c; list = l; }
        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int i) { return list.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder h;
            if (v == null) {
                v = LayoutInflater.from(ctx).inflate(R.layout.item_category, parent, false);
                h = new ViewHolder();
                h.name = v.findViewById(R.id.tv_name);
                h.sub = v.findViewById(R.id.tv_subtitle);
                h.count = v.findViewById(R.id.tv_count);
                v.setTag(h);
            } else { h = (ViewHolder) v.getTag(); }
            MutopiaCatalogParser.CategoryItem it = list.get(pos);
            h.name.setText(it.name);
            if (it.subtitle != null && !it.subtitle.isEmpty()) {
                h.sub.setVisibility(View.VISIBLE);
                h.sub.setText(it.subtitle);
            } else {
                h.sub.setVisibility(View.GONE);
            }
            h.count.setText(it.countStr);
            return v;
        }
        private static class ViewHolder {
            TextView name; TextView sub; TextView count;
        }
    }

    private class PieceAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<MutopiaCatalogParser.PieceItem> list;
        PieceAdapter(Context c, List<MutopiaCatalogParser.PieceItem> l) { ctx = c; list = l; }
        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int i) { return list.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder h;
            if (v == null) {
                v = LayoutInflater.from(ctx).inflate(R.layout.item_piece, parent, false);
                h = new ViewHolder();
                h.title = v.findViewById(R.id.tv_title);
                h.composer = v.findViewById(R.id.tv_composer);
                h.instr = v.findViewById(R.id.tv_instr);
                h.instrSep = v.findViewById(R.id.tv_instr_sep);
                h.style = v.findViewById(R.id.tv_style);
                h.btn = v.findViewById(R.id.btn_download);
                v.setTag(h);
            } else { h = (ViewHolder) v.getTag(); }
            final MutopiaCatalogParser.PieceItem p = list.get(pos);
            h.title.setText(p.title);
            h.composer.setText(p.composer);
            boolean hasInstr = p.instrumentation != null && !p.instrumentation.isEmpty();
            h.instr.setVisibility(hasInstr ? View.VISIBLE : View.GONE);
            h.instrSep.setVisibility(hasInstr ? View.VISIBLE : View.GONE);
            if (hasInstr) h.instr.setText(p.instrumentation);
            if (p.style != null && !p.style.isEmpty()) {
                h.style.setVisibility(View.VISIBLE);
                h.style.setText(p.style);
            } else {
                h.style.setVisibility(View.GONE);
            }
            h.btn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { onPieceSelected(p); }
            });
            return v;
        }
        private class ViewHolder {
            TextView title; TextView composer; TextView instr;
            TextView instrSep; TextView style; Button btn;
        }
    }
}
