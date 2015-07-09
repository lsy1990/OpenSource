
package com.example.blurbitmap;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

public class BitmapAlgrithom {

    public static final String TAG = BitmapAlgrithom.class.getSimpleName();
    private Context mContext;
    private int mScreenWidth;
    private int mScreenHeight;
    private static final Paint BG_PAINT = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final Paint COPY_PAINT = new Paint();
    private static final int BLUR_ARTIFACT_WIDTH = 30;
    private static final BitmapFactory.Options BG_BITMAP_OPTS = new BitmapFactory.Options();
    static {
        BG_BITMAP_OPTS.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }
    private static int BLUR_RADIUS = 3;

    public BitmapAlgrithom(Context context) {
        mContext = context;
    }

    public Bitmap blurBitmap(Bitmap src, int width, int height) {
        Bitmap bgBitmap = null;
        Bitmap overlay = null;
        Bitmap src8 = null;
        mScreenWidth = width;
        mScreenHeight = height;
        try {
            src8 = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(src8);
            canvas.drawBitmap(src, 0, 0, COPY_PAINT);

            src8 = performBlur(src8);

            Matrix matrix = getCenterCropMatrix(src8.getWidth(), src8.getHeight(),
                    mScreenWidth + BLUR_ARTIFACT_WIDTH,
                    mScreenHeight);
            bgBitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_8888);
            Canvas bgCanvas = new Canvas(bgBitmap);

            bgCanvas.drawBitmap(src8, matrix, BG_PAINT);

            src8.recycle();
            src8 = null;

            overlay = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.ic_launcher, BG_BITMAP_OPTS);
            if (overlay == null) {
                Log.w(TAG, "Cannot get Vignette, use default");
                bgBitmap.recycle();
                return null;
            }
            Rect rect = new Rect(0, 0, mScreenWidth, mScreenHeight);
            bgCanvas.drawBitmap(overlay, null, rect, BG_PAINT);
            overlay.recycle();
            overlay = null;

        } catch (OutOfMemoryError oom) {
            Log.w(TAG, "blurBitmap", oom);
            // DO not return image without overlay - just return null
            if (bgBitmap != null) {
                bgBitmap.recycle();
            }
            if (overlay != null) {
                overlay.recycle();
            }
            if (src8 != null) {
                src8.recycle();
            }
            return null;
        }

        return bgBitmap;
    }

    private Bitmap performBlur(Bitmap src) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        RenderScript rs = RenderScript.create(mContext);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, src,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation tmpOut = Allocation.createFromBitmap(rs, out,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(out);
        src.recycle();
        return out;
    }

    /**
     * Returns Matrix to draw Bitmap with CENTER_CROP scale type.
     * 
     * @param width Bitmap width
     * @param height Bitmap height
     * @param targetWidth target width
     * @param targetHeight target height
     * @return Matrix to draw Bitmap with CENTER_CROP scale type
     */
    public static Matrix getCenterCropMatrix(int width, int height, int targetWidth,
            int targetHeight) {
        Matrix matrix = new Matrix();
        float scale;
        float dx = 0, dy = 0;

        if (width * targetHeight > targetWidth * height) {
            scale = (float) targetHeight / (float) height;
            dx = (targetWidth - width * scale) * 0.5f;
        } else {
            scale = (float) targetWidth / (float) width;
            dy = (targetHeight - height * scale) * 0.5f;
        }

        matrix.setScale(scale, scale);
        matrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        return matrix;
    }
}
