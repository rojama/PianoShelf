package com.rojama.pianoshelf;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

/**
 * Tab 4: 在线乐谱入口 Tab。
 *
 * 不同于浏览/最近/收藏 3 个直接展示文件列表的 Tab，
 * 在线乐谱的功能集中在独立的 OnlineScoreActivity（URL 输入 + 平台推荐 + 下载进度）。
 * 本 Tab 仅作为入口页，展示说明卡片 + 「立即前往」按钮。
 */
public class TabOnlineWelcome extends LinearLayout {

    public TabOnlineWelcome(final Context context) {
        super(context);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.tab_online_welcome, this, true);

        CardView card = findViewById(R.id.card_guide);
        TextView title = findViewById(R.id.tv_title);
        TextView desc = findViewById(R.id.tv_desc);
        Button btnOpen = findViewById(R.id.btn_open_online);

        title.setText(R.string.tab_online_guide_title);
        desc.setText(R.string.tab_online_guide_desc);
        btnOpen.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(context, OnlineScoreActivity.class);
                context.startActivity(i);
            }
        });
        card.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(context, OnlineScoreActivity.class);
                context.startActivity(i);
            }
        });
    }
}
