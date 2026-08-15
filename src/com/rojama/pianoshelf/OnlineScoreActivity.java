package com.rojama.pianoshelf;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 在线乐谱下载 Activity。
 *
 * 功能：
 *   1) 输入任意 http(s) 链接（.xml/.mxl/.musicxml）直接下载并打开
 *   2) 推荐 4 个免费 MusicXML 平台，点击对应卡片跳转浏览器浏览
 *   3) 支持剪贴板粘贴（一键粘贴），下载有进度条
 *   4) 下载成功后写入最近记录 → 自动跳转 GraphicsActivity 打开
 *
 * 免费 MusicXML 平台（预置在 OnlineScoreDownloader.PRESET_PLATFORMS）：
 *   - MuseScore.com (100万+乐谱，公有领域免费)
 *   - IMSLP.org (经典乐谱，公有领域)
 *   - MutopiaProject.org (LilyPond 高精度排版)
 *   - OpenScore (MuseScore，公有领域交互式项目)
 */
public class OnlineScoreActivity extends AppCompatActivity {
    private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";

    private EditText etUrl;
    private Button btnDownload;
    private Button btnPaste;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private LinearLayout platformContainer;
    private DatabaseHelper dbhelp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.online_score);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.tab_online);
        }
        dbhelp = new DatabaseHelper(this);

        etUrl = findViewById(R.id.et_url);
        btnDownload = findViewById(R.id.btn_download);
        btnPaste = findViewById(R.id.btn_paste);
        progressBar = findViewById(R.id.progress_download);
        tvStatus = findViewById(R.id.tv_status);
        platformContainer = findViewById(R.id.platform_container);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String url = etUrl.getText() == null ? "" : etUrl.getText().toString().trim();
                startDownload(url, false);
            }
        });

        btnPaste.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CharSequence pasted = getClipboardFirstText();
                if (!TextUtils.isEmpty(pasted)) {
                    etUrl.setText(pasted);
                    etUrl.setSelection(etUrl.getText().length());
                    Toast.makeText(OnlineScoreActivity.this,
                            R.string.online_pasted_ok, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OnlineScoreActivity.this,
                            R.string.online_paste_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // IME Done：点击键盘完成键也触发下载
        etUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    btnDownload.performClick();
                    return true;
                }
                return false;
            }
        });

        buildPresetPlatformCards();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------
    // 预置平台卡片（可点击跳浏览器）
    // ------------------------------------------------------------------

    private void buildPresetPlatformCards() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final OnlineScoreDownloader.PlatformInfo info
                : OnlineScoreDownloader.PRESET_PLATFORMS) {
            CardView card = (CardView) inflater.inflate(
                    R.layout.online_platform_item, platformContainer, false);
            TextView name = card.findViewById(R.id.tv_name);
            TextView desc = card.findViewById(R.id.tv_desc);
            TextView link = card.findViewById(R.id.tv_link);
            Button open = card.findViewById(R.id.btn_open_browser);

            name.setText(info.name);
            desc.setText(info.description);
            link.setText(info.websiteUrl);

            // 针对 Mutopia：CGI 表格结构规则，支持「应用内分类目录」直接浏览
            // 其它 3 个平台：用内置 WebView 打开 + 拦截 MusicXML 下载链接
            final boolean mutopia = "Mutopia Project".equalsIgnoreCase(info.name);
            final View.OnClickListener clicker = new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (mutopia) {
                        startActivity(new Intent(OnlineScoreActivity.this, InAppBrowseActivity.class));
                    } else {
                        openInAppWebView(info);
                    }
                }
            };
            card.setOnClickListener(clicker);
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (mutopia) {
                        open.setText(R.string.online_btn_visit); // 按钮文案统一；保留 Mutopia 走 InAppBrowse
                        startActivity(new Intent(OnlineScoreActivity.this, InAppBrowseActivity.class));
                    } else {
                        openInAppWebView(info);
                    }
                }
            });
            // Mutopia 按钮文案显示为「应用内浏览」以突出该特性
            if (mutopia) open.setText(R.string.online_btn_inapp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            platformContainer.addView(card, lp);
        }
    }

    /** 使用内置 WebView 打开平台（拦截 MusicXML 链接直接下载打开） */
    private void openInAppWebView(OnlineScoreDownloader.PlatformInfo info) {
        try {
            Intent i = new Intent(this, PlatformWebViewActivity.class);
            i.putExtra(PlatformWebViewActivity.EXTRA_INIT_URL, info.websiteUrl);
            i.putExtra(PlatformWebViewActivity.EXTRA_PLATFORM_NAME, info.name);
            startActivity(i);
        } catch (Throwable t) {
            // WebView 不可用时回退到系统浏览器
            openInBrowser(info.websiteUrl);
        }
    }

    private void openInBrowser(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this,
                    getString(R.string.online_browser_fail) + " " + url,
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // 下载流程
    // ------------------------------------------------------------------

    private void startDownload(String url, boolean force) {
        String err = OnlineScoreDownloader.validateUrl(url);
        if (err != null) {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            return;
        }
        hideSoftKeyboard();
        btnDownload.setEnabled(false);
        btnPaste.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.online_status_connecting);

        OnlineScoreDownloader.downloadAsync(this, url, force, new OnlineScoreDownloader.DownloadCallback() {
            @Override public void onProgress(int percent) {
                if (percent < 0) {
                    // 未知总长度：显示为不确定进度 + 百分比隐藏
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        progressBar.setMin(0);
                    }
                    progressBar.setIndeterminate(true);
                    tvStatus.setText(R.string.online_status_downloading_unknown);
                } else {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(percent);
                    tvStatus.setText(getString(R.string.online_status_downloading, percent));
                }
            }

            @Override public void onSuccess(File localFile) {
                resetButtons();
                progressBar.setIndeterminate(false);
                progressBar.setProgress(100);
                tvStatus.setText(getString(R.string.online_status_done, localFile.getName()));
                onDownloadSucceeded(localFile);
            }

            @Override public void onFailure(String message) {
                resetButtons();
                progressBar.setVisibility(View.GONE);
                tvStatus.setVisibility(View.GONE);
                Toast.makeText(OnlineScoreActivity.this,
                        getString(R.string.online_status_fail) + "\n" + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetButtons() {
        btnDownload.setEnabled(true);
        btnPaste.setEnabled(true);
    }

    private void onDownloadSucceeded(File file) {
        // 写入最近记录，方便下次从「最近打开」再次打开
        if (dbhelp != null) dbhelp.insertRecentItem(file.getAbsolutePath());

        // 启动 GraphicsActivity（与本地文件完全一致的通路，走 FileProvider）
        try {
            Uri uri = FileProvider.getUriForFile(this, FILEPROVIDER_AUTH, file);
            Intent intent = new Intent();
            intent.setDataAndType(uri, "application/vnd.recordare.musicxml");
            intent.setClass(this, GraphicsActivity.class);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            Toast.makeText(this, R.string.online_open_ok, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this,
                    getString(R.string.info_open_err) + " " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // 剪贴板工具
    // ------------------------------------------------------------------

    @Nullable
    private CharSequence getClipboardFirstText() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return null;
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return null;
            ClipData.Item item = clip.getItemAt(0);
            if (item == null) return null;
            return item.coerceToText(this);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void hideSoftKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Throwable ignored) {}
    }
}
