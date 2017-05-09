package com.changwilling.cache_android.cache;

import android.graphics.Bitmap;
import android.widget.ImageView;

import rx.Observable;

/**
 * Created by changwilling on 17/2/16.
 */

public interface IPicDataCache {
    String getFilePathByUrl(String picId);
    Observable<Bitmap> loadBitmap(String picUrl, ImageView imageView, boolean isThumbnail);
}
