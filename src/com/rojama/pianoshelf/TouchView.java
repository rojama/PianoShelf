package com.rojama.pianoshelf;

import android.content.Context;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

/**
 * 继承 ImageView 实现多点触控拖动、双指连续缩放、左右滑动翻页。
 *
 * 改造要点:
 *  - 移除 ZoomControls 依赖（用双指 Pinch 手势实现连续缩放）
 *  - 使用 ScaleGestureDetector 实现真正连续的 Pinch-to-Zoom（不用增量的 BIGGER/SMALLER）
 *  - 使用 GestureDetector 监听 onFling 实现左右滑动 → 翻页回调
 *  - 增加 PageFlipListener 接口，由外部 Activity/GraphicsView 响应翻页动作
 */
public class TouchView extends ImageView {

    // ----- 拖拽/缩放状态 -----
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private int screenW;
    private int screenH;
    public int imgW;
    public int imgH;

    // 拖拽用坐标
    private float startX;
    private float startY;
    private float lastRawX;
    private float lastRawY;

    // 连续缩放用：当前 scale、焦点、Matrix
    private float currentScale = 1.0f;
    private float minScale = 0.5f;
    private float maxScale = 3.0f;
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private float scaleFocusX, scaleFocusY;

    // 滑动翻页 + 双指缩放手势检测
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    // 拖动边界回弹动画
    private TranslateAnimation trans;

    /** 左右滑动翻页回调（由 Activity/GraphicsView 绑定）。 */
    public interface PageFlipListener {
        void onPrevPage();
        void onNextPage();
    }
    private PageFlipListener pageFlipListener;
    public void setPageFlipListener(PageFlipListener listener) {
        this.pageFlipListener = listener;
    }

    public TouchView(Context context, int w, int h) {
        super(context);
        this.setPadding(0, 0, 0, 0);
        screenW = w;
        screenH = h;

        // Note: ZoomControls 已从布局中删除，这里不再查找/绑定

        // 双指 Pinch 连续缩放
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mode = ZOOM;
                savedMatrix.set(matrix);
                scaleFocusX = detector.getFocusX();
                scaleFocusY = detector.getFocusY();
                return true;
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newScale = currentScale * factor;
                // clamp scale range
                if (newScale < minScale) factor = minScale / currentScale;
                if (newScale > maxScale) factor = maxScale / currentScale;
                currentScale *= factor;

                matrix.set(savedMatrix);
                matrix.postScale(factor, factor, scaleFocusX, scaleFocusY);
                applyMatrixToLayout();
                return true;
            }
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                // 缩放结束后检查边界
                processOut();
            }
        });

        // 左右滑动翻页（onFling）+ 单击等辅助手势
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            /**
             * onFling 触发翻页：
             *  - 水平方向速度显著大于垂直方向（避免误触）
             *  - 速度绝对值 > 阈值
             *  - 水平位移 > 最小距离
             */
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                final float minDistance = 80f;        // dp-ish 阈值；直接用像素也够用
                final float minVelocity = 200f;
                float dx = e2.getRawX() - e1.getRawX();
                float dy = e2.getRawY() - e1.getRawY();
                if (Math.abs(dx) < Math.abs(dy)) return false;          // 明显不是横向滑动
                if (Math.abs(dx) < minDistance) return false;           // 滑动距离太短
                if (Math.abs(velocityX) < minVelocity) return false;    // 速度太慢
                if (pageFlipListener != null) {
                    if (velocityX > 0) {
                        // 向右滑 → 上一页（手指左←右，内容从右往左移动 → 看前一页）
                        pageFlipListener.onPrevPage();
                    } else {
                        // 向左滑 → 下一页
                        pageFlipListener.onNextPage();
                    }
                    return true;
                }
                return false;
            }
        });

        setScaleType(ScaleType.MATRIX);
    }

    /**
     * 把 Matrix 的 scale/translate 折算成 layout(l,t,r,b) 的位置和尺寸。
     * 因为旧代码大量使用 getLeft()/getRight()/getWidth() 来判断边界与回弹，
     * 我们保持同一套"视图尺寸 = 实际 bitmap 尺寸 * scale"的坐标模型，
     * 这样 processOut 无需重写。
     */
    private void applyMatrixToLayout() {
        if (imgW <= 0 || imgH <= 0) return;
        float[] values = new float[9];
        matrix.getValues(values);
        float sx = values[Matrix.MSCALE_X];
        float sy = values[Matrix.MSCALE_Y];
        float tx = values[Matrix.MTRANS_X];
        float ty = values[Matrix.MTRANS_Y];
        int newW = Math.round(imgW * sx);
        int newH = Math.round(imgH * sy);
        // 矩阵中的 translate 是"图像左上角"相对 view 原点的偏移
        // 这里我们让 view 的 layout 左上角直接落在 (tx, ty)，宽高 = 图像缩放后尺寸
        int left = Math.round(tx);
        int top = Math.round(ty);
        this.layout(left, top, left + newW, top + newH);
    }

    /** 根据当前 layout 尺寸反向同步 Matrix，保证拖动/缩放起点一致。 */
    private void syncMatrixFromLayout() {
        if (imgW <= 0 || imgH <= 0) return;
        int w = getWidth();
        int h = getHeight();
        float sx = (w > 0) ? (float) w / imgW : 1f;
        float sy = (h > 0) ? (float) h / imgH : 1f;
        currentScale = (sx + sy) * 0.5f;
        if (currentScale < minScale) currentScale = minScale;
        if (currentScale > maxScale) currentScale = maxScale;
        matrix.reset();
        matrix.postScale(currentScale, currentScale);
        matrix.postTranslate(getLeft(), getTop());
        savedMatrix.set(matrix);
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        if (bm != null) {
            imgW = bm.getWidth();
            imgH = bm.getHeight();
        }
        // bitmap 变更后：重置矩阵，并根据屏幕尺寸自动选一个合适的初始缩放
        resetInitialScale();
    }

    /** 初始缩放：把图像按比例适配到屏幕内（等价于旧 CENTER_INSIDE 的视觉效果）。 */
    private void resetInitialScale() {
        if (imgW <= 0 || imgH <= 0) {
            matrix.reset();
            setImageMatrix(matrix);
            return;
        }
        float scaleX = (screenW > 0) ? (float) screenW / imgW : 1f;
        float scaleY = (screenH > 0) ? (float) screenH / imgH : 1f;
        float init = Math.min(scaleX, scaleY);
        if (init < minScale) init = minScale;
        if (init > maxScale) init = maxScale;
        currentScale = init;
        matrix.reset();
        matrix.postScale(currentScale, currentScale);
        // 居中
        int drawW = Math.round(imgW * currentScale);
        int drawH = Math.round(imgH * currentScale);
        int tx = Math.max(0, (screenW - drawW) / 2);
        int ty = Math.max(0, (screenH - drawH) / 2);
        matrix.postTranslate(tx, ty);
        savedMatrix.set(matrix);
        setImageMatrix(matrix);
        // 同步 layout 位置（边界判断依赖）
        this.layout(tx, ty, tx + drawW, ty + drawH);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 手势检测器先吃事件
        boolean scaleConsumed = scaleDetector.onTouchEvent(event);
        boolean gestureConsumed = gestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (mode != ZOOM) {
                    // 只有单指按下才算进入 DRAG
                    mode = DRAG;
                    lastRawX = event.getRawX();
                    lastRawY = event.getRawY();
                    startX = event.getRawX() - getLeft();
                    startY = event.getRawY() - getTop();
                    // DOWN 时把当前 layout 位置同步进 Matrix，避免跳动
                    syncMatrixFromLayout();
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 第二根手指按下 → 手势里 ScaleDetector 会处理 onScaleBegin
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() <= 1) {
                    // 所有手指抬起后：边界检查 + 回弹
                    processOut();
                    mode = NONE;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && !scaleDetector.isInProgress()) {
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    int newLeft = Math.round(rawX - startX);
                    int newTop = Math.round(rawY - startY);
                    int newRight = newLeft + getWidth();
                    int newBottom = newTop + getHeight();
                    // 旧代码有 88/85 的"容差阈值"，这里移除避免拖不动，只做简单范围限制
                    this.layout(newLeft, newTop, newRight, newBottom);
                    lastRawX = rawX;
                    lastRawY = rawY;
                    // 同步 matrix（下次 ZOOM 起点一致）
                    syncMatrixFromLayout();
                }
                break;
        }
        return true;
    }

    /** 抬起手指后：如果视图"小于屏幕"则居中回弹；大于屏幕则不让出现空白边。 */
    private void processOut() {
        int disX = 0;
        int disY = 0;
        if (getHeight() <= screenH) {
            if (this.getTop() < 0) {
                int dis = getTop();
                this.layout(this.getLeft(), 0, this.getRight(), 0 + this.getHeight());
                disY = dis - getTop();
            } else if (this.getBottom() > screenH) {
                disY = getHeight() - screenH + getTop();
                this.layout(this.getLeft(), screenH - getHeight(), this.getRight(), screenH);
            } else {
                // 垂直方向还能居中 → 主动居中
                int targetTop = (screenH - getHeight()) / 2;
                if (Math.abs(targetTop - getTop()) > 2) {
                    disY = getTop() - targetTop;
                    this.layout(this.getLeft(), targetTop, this.getRight(), targetTop + getHeight());
                }
            }
        } else {
            if (this.getTop() > 0) {
                disY = getTop();
                this.layout(this.getLeft(), 0, this.getRight(), this.getHeight());
            } else if (this.getBottom() < screenH) {
                disY = getBottom() - screenH;
                this.layout(this.getLeft(), screenH - getHeight(), this.getRight(), screenH);
            }
        }
        if (getWidth() <= screenW) {
            if (this.getLeft() < 0) {
                disX = getLeft();
                this.layout(0, this.getTop(), 0 + getWidth(), this.getBottom());
            } else if (this.getRight() > screenW) {
                disX = getWidth() - screenW + getLeft();
                this.layout(screenW - getWidth(), this.getTop(), screenW, this.getBottom());
            } else {
                int targetLeft = (screenW - getWidth()) / 2;
                if (Math.abs(targetLeft - getLeft()) > 2) {
                    disX = getLeft() - targetLeft;
                    this.layout(targetLeft, this.getTop(), targetLeft + getWidth(), this.getBottom());
                }
            }
        } else {
            if (this.getLeft() > 0) {
                disX = getLeft();
                this.layout(0, this.getTop(), getWidth(), this.getBottom());
            } else if (this.getRight() < screenW) {
                disX = getRight() - screenW;
                this.layout(screenW - getWidth(), this.getTop(), screenW, this.getBottom());
            }
        }
        if (disX != 0 || disY != 0) {
            trans = new TranslateAnimation(disX, 0, disY, 0);
            trans.setDuration(300);
            this.startAnimation(trans);
        }
        syncMatrixFromLayout();
    }

    /** Helper to read display metrics without deprecated APIs. */
    public static DisplayMetrics getDisplayMetrics(Context ctx) {
        return ctx.getResources().getDisplayMetrics();
    }
}
