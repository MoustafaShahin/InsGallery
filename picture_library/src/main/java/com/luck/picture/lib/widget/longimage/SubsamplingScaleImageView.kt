/*
Copyright 2013-2015 David Morrissey

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.luck.picture.lib.widget.longimage

import android.content.*
import android.database.Cursor
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.*
import android.os.Build.VERSION
import android.provider.MediaStore
import android.util.*
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import androidx.annotation.AnyThread
import com.luck.picture.lib.R
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.Executor

/**
 * Displays an image subsampled as necessary to avoid loading too much image data into memory. After a pinch to zoom in,
 * a set of image tiles subsampled at higher resolution are loaded and displayed over the base layer. During pinch and
 * zoom, tiles off screen or higher/lower resolution than required are discarded from memory.
 *
 *
 * Tiles are no larger than the max supported bitmap size, so with large images tiling may be used even when zoomed out.
 *
 *
 * v prefixes - coordinates, translations and distances measured in screen (view) pixels
 * s prefixes - coordinates, translations and distances measured in source image pixels (scaled)
 */
class SubsamplingScaleImageView @JvmOverloads constructor(context: Context, attr: AttributeSet? = null) : View(context, attr) {
    // Bitmap (preview or full image)
    private var bitmap: Bitmap? = null

    // Whether the bitmap is a preview image
    private var bitmapIsPreview = false

    // Specifies if a cache handler is also referencing the bitmap. Do not recycle if so.
    private var bitmapIsCached = false

    // Uri of full size image
    private var uri: Uri? = null

    // Sample size used to display the whole image when fully zoomed out
    private var fullImageSampleSize = 0

    // Map of zoom level to tile grid
    private var tileMap: MutableMap<Int, List<Tile>>? = null

    // Overlay tile boundaries and other info
    private var debug = false

    // Image orientation setting
    private var orientation = ORIENTATION_0

    // Max scale allowed (prevent infinite zoom)
    private var maxScale = 2f

    // Min scale allowed (prevent infinite zoom)
    private var minScale = minScale()

    // Density to reach before loading higher resolution tiles
    private var minimumTileDpi = -1

    // Pan limiting style
    private var panLimit = PAN_LIMIT_INSIDE

    // Minimum scale type
    private var minimumScaleType = SCALE_TYPE_CENTER_INSIDE
    private var maxTileWidth = TILE_SIZE_AUTO
    private var maxTileHeight = TILE_SIZE_AUTO

    // Whether to use the thread pool executor to load tiles
    private var parallelLoadingEnabled = false

    // Gesture detection settings
    private var panEnabled = true
    private var zoomEnabled = true
    private var quickScaleEnabled = true

    // Double tap zoom behaviour
    private var doubleTapZoomScale = 1f
    private var doubleTapZoomStyle = ZOOM_FOCUS_FIXED
    private var doubleTapZoomDuration = 500

    // Current scale and scale at start of zoom
    private var scale = 0f
    private var scaleStart = 0f

    // Screen coordinate of top-left corner of source image
    private var vTranslate: PointF? = null
    private var vTranslateStart: PointF? = null
    private var vTranslateBefore: PointF? = null

    // Source coordinate to center on, used when new position is set externally before view is ready
    private var pendingScale: Float? = null
    private var sPendingCenter: PointF? = null
    private var sRequestedCenter: PointF? = null

    // Source image dimensions and orientation - dimensions relate to the unrotated image
    private var sWidth = 0
    private var sHeight = 0
    private var sOrientation = 0
    private var sRegion: Rect? = null
    private var pRegion: Rect? = null

    // Is two-finger zooming in progress
    private var isZooming = false

    // Is one-finger panning in progress
    private var isPanning = false

    // Is quick-scale gesture in progress
    private var isQuickScaling = false

    // Max touches used in current gesture
    private var maxTouchCount = 0

    // Fling detector
    private var detector: GestureDetector? = null

    // Tile and image decoding
    private var decoder: ImageRegionDecoder? = null
    private val decoderLock = Any()
    private var bitmapDecoderFactory: DecoderFactory<out ImageDecoder?> = CompatDecoderFactory<ImageDecoder?>(SkiaImageDecoder::class.java)
    private var regionDecoderFactory: DecoderFactory<out ImageRegionDecoder?> = CompatDecoderFactory<ImageRegionDecoder?>(SkiaImageRegionDecoder::class.java)

    // Debug values
    private var vCenterStart: PointF? = null
    private var vDistStart = 0f

    // Current quickscale state
    private val quickScaleThreshold: Float
    private var quickScaleLastDistance = 0f
    private var quickScaleMoved = false
    private var quickScaleVLastPoint: PointF? = null
    private var quickScaleSCenter: PointF? = null
    private var quickScaleVStart: PointF? = null

    // Scale and center animation tracking
    private var anim: Anim? = null

    // Whether a ready notification has been sent to subclasses
    private var readySent = false

    // Whether a base layer loaded notification has been sent to subclasses
    private var imageLoadedSent = false

    // Event listener
    private var onImageEventListener: OnImageEventListener? = null

    // Scale and center listener
    private var onStateChangedListener: OnStateChangedListener? = null

    // Long click listener
    private var onLongClickListener: OnLongClickListener? = null

    // Long click handler
    private val handler: Handler

    // Paint objects created once and reused for efficiency
    private var bitmapPaint: Paint? = null
    private var debugPaint: Paint? = null
    private var tileBgPaint: Paint? = null

    // Volatile fields used to reduce object creation
    private var satTemp: ScaleAndTranslate? = null
    private var matrix: Matrix? = null
    private var sRect: RectF? = null
    private val srcArray = FloatArray(8)
    private val dstArray = FloatArray(8)

    //The logical density of the display
    private val density: Float

    /**
     * Sets the image orientation. It's best to call this before setting the image file or asset, because it may waste
     * loading of tiles. However, this can be freely called at any time.
     */
    fun setOrientation(orientation: Int) {
        require(VALID_ORIENTATIONS.contains(orientation)) { "Invalid orientation: $orientation" }
        this.orientation = orientation
        reset(false)
        invalidate()
        requestLayout()
    }

    /**
     * Set the image source from a bitmap, resource, asset, file or other URI.
     *
     * @param imageSource Image source.
     */
    fun setImage(imageSource: ImageSource?) {
        setImage(imageSource, null, null)
    }

    /**
     * Set the image source from a bitmap, resource, asset, file or other URI, starting with a given orientation
     * setting, scale and center. This is the best method to use when you want scale and center to be restored
     * after screen orientation change; it avoids any redundant loading of tiles in the wrong orientation.
     *
     * @param imageSource Image source.
     * @param state       State to be restored. Nullable.
     */
    fun setImage(imageSource: ImageSource?, state: ImageViewState?) {
        setImage(imageSource, null, state)
    }

    /**
     * Set the image source from a bitmap, resource, asset, file or other URI, providing a preview image to be
     * displayed until the full size image is loaded.
     *
     *
     * You must declare the dimensions of the full size image by calling [ImageSource.dimensions]
     * on the imageSource object. The preview source will be ignored if you don't provide dimensions,
     * and if you provide a bitmap for the full size image.
     *
     * @param imageSource   Image source. Dimensions must be declared.
     * @param previewSource Optional source for a preview image to be displayed and allow interaction while the full size image loads.
     */
    fun setImage(imageSource: ImageSource?, previewSource: ImageSource?) {
        setImage(imageSource, previewSource, null)
    }

    /**
     * Set the image source from a bitmap, resource, asset, file or other URI, providing a preview image to be
     * displayed until the full size image is loaded, starting with a given orientation setting, scale and center.
     * This is the best method to use when you want scale and center to be restored after screen orientation change;
     * it avoids any redundant loading of tiles in the wrong orientation.
     *
     *
     * You must declare the dimensions of the full size image by calling [ImageSource.dimensions]
     * on the imageSource object. The preview source will be ignored if you don't provide dimensions,
     * and if you provide a bitmap for the full size image.
     *
     * @param imageSource   Image source. Dimensions must be declared.
     * @param previewSource Optional source for a preview image to be displayed and allow interaction while the full size image loads.
     * @param state         State to be restored. Nullable.
     */
    fun setImage(imageSource: ImageSource?, previewSource: ImageSource?, state: ImageViewState?) {
        if (imageSource == null) {
            throw NullPointerException("imageSource must not be null")
        }
        reset(true)
        if (state != null) {
            restoreState(state)
        }
        if (previewSource != null) {
            require(imageSource.getBitmap() == null) { "Preview image cannot be used when a bitmap is provided for the main image" }
            require(!(imageSource.getSWidth() <= 0 || imageSource.getSHeight() <= 0)) { "Preview image cannot be used unless dimensions are provided for the main image" }
            sWidth = imageSource.getSWidth()
            sHeight = imageSource.getSHeight()
            pRegion = previewSource.getSRegion()
            if (previewSource.getBitmap() != null) {
                bitmapIsCached = previewSource.isCached()
                onPreviewLoaded(previewSource.getBitmap())
            } else {
                var uri: Uri? = previewSource.getUri()
                if (uri == null && previewSource.getResource() != null) {
                    uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + previewSource.getResource())
                }
                val task = BitmapLoadTask(this, context, bitmapDecoderFactory, uri, true)
                execute(task)
            }
        }
        if (imageSource.getBitmap() != null && imageSource.getSRegion() != null) {
            onImageLoaded(Bitmap.createBitmap(imageSource.getBitmap(), imageSource.getSRegion().left, imageSource.getSRegion().top, imageSource.getSRegion().width(), imageSource.getSRegion().height()), ORIENTATION_0, false)
        } else if (imageSource.getBitmap() != null) {
            onImageLoaded(imageSource.getBitmap(), ORIENTATION_0, imageSource.isCached())
        } else {
            sRegion = imageSource.getSRegion()
            uri = imageSource.getUri()
            if (uri == null && imageSource.getResource() != null) {
                uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + imageSource.getResource())
            }
            if (imageSource.getTile() || sRegion != null) {
                // Load the bitmap using tile decoding.
                val task = TilesInitTask(this, context, regionDecoderFactory, uri)
                execute(task)
            } else {
                // Load the bitmap as a single image.
                val task = BitmapLoadTask(this, context, bitmapDecoderFactory, uri, false)
                execute(task)
            }
        }
    }

    /**
     * Reset all state before setting/changing image or setting new rotation.
     */
    private fun reset(newImage: Boolean) {
        debug("reset newImage=$newImage")
        scale = 0f
        scaleStart = 0f
        vTranslate = null
        vTranslateStart = null
        vTranslateBefore = null
        pendingScale = 0f
        sPendingCenter = null
        sRequestedCenter = null
        isZooming = false
        isPanning = false
        isQuickScaling = false
        maxTouchCount = 0
        fullImageSampleSize = 0
        vCenterStart = null
        vDistStart = 0f
        quickScaleLastDistance = 0f
        quickScaleMoved = false
        quickScaleSCenter = null
        quickScaleVLastPoint = null
        quickScaleVStart = null
        anim = null
        satTemp = null
        matrix = null
        sRect = null
        if (newImage) {
            uri = null
            if (decoder != null) {
                synchronized(decoderLock) {
                    decoder.recycle()
                    decoder = null
                }
            }
            if (bitmap != null && !bitmapIsCached) {
                bitmap!!.recycle()
            }
            if (bitmap != null && bitmapIsCached && onImageEventListener != null) {
                onImageEventListener!!.onPreviewReleased()
            }
            sWidth = 0
            sHeight = 0
            sOrientation = 0
            sRegion = null
            pRegion = null
            readySent = false
            imageLoadedSent = false
            bitmap = null
            bitmapIsPreview = false
            bitmapIsCached = false
        }
        if (tileMap != null) {
            for ((_, value) in tileMap!!) {
                for (tile in value) {
                    tile.visible = false
                    if (tile.bitmap != null) {
                        tile.bitmap.recycle()
                        tile.bitmap = null
                    }
                }
            }
            tileMap = null
        }
        setGestureDetector(context)
    }

    private fun setGestureDetector(context: Context) {
        detector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (panEnabled && readySent && vTranslate != null && e1 != null && e2 != null && (Math.abs(e1.x - e2.x) > 50 || Math.abs(e1.y - e2.y) > 50) && (Math.abs(velocityX) > 500 || Math.abs(velocityY) > 500) && !isZooming) {
                    val vTranslateEnd = PointF(vTranslate!!.x + velocityX * 0.25f, vTranslate!!.y + velocityY * 0.25f)
                    val sCenterXEnd = (width / 2 - vTranslateEnd.x) / scale
                    val sCenterYEnd = (height / 2 - vTranslateEnd.y) / scale
                    AnimationBuilder(PointF(sCenterXEnd, sCenterYEnd)).withEasing(EASE_OUT_QUAD).withPanLimited(false).withOrigin(ORIGIN_FLING).start()
                    return true
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                performClick()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (zoomEnabled && readySent && vTranslate != null) {
                    // Hacky solution for #15 - after a double tap the GestureDetector gets in a state
                    // where the next fling is ignored, so here we replace it with a new one.
                    setGestureDetector(context)
                    return if (quickScaleEnabled) {
                        // Store quick scale params. This will become either a double tap zoom or a
                        // quick scale depending on whether the user swipes.
                        vCenterStart = PointF(e.x, e.y)
                        vTranslateStart = PointF(vTranslate!!.x, vTranslate!!.y)
                        scaleStart = scale
                        isQuickScaling = true
                        isZooming = true
                        quickScaleLastDistance = -1f
                        quickScaleSCenter = viewToSourceCoord(vCenterStart!!)
                        quickScaleVStart = PointF(e.x, e.y)
                        quickScaleVLastPoint = PointF(quickScaleSCenter!!.x, quickScaleSCenter!!.y)
                        quickScaleMoved = false
                        // We need to get events in onTouchEvent after this.
                        false
                    } else {
                        // Start double tap zoom animation.
                        doubleTapZoom(viewToSourceCoord(PointF(e.x, e.y)), PointF(e.x, e.y))
                        true
                    }
                }
                return super.onDoubleTapEvent(e)
            }
        })
    }

    /**
     * On resize, preserve center and scale. Various behaviours are possible, override this method to use another.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        debug("onSizeChanged %dx%d -> %dx%d", oldw, oldh, w, h)
        val sCenter = getCenter()
        if (readySent && sCenter != null) {
            anim = null
            pendingScale = scale
            sPendingCenter = sCenter
        }
    }

    /**
     * Measures the width and height of the view, preserving the aspect ratio of the image displayed if wrap_content is
     * used. The image will scale within this box, not resizing the view as it is zoomed.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        val resizeWidth = widthSpecMode != MeasureSpec.EXACTLY
        val resizeHeight = heightSpecMode != MeasureSpec.EXACTLY
        var width = parentWidth
        var height = parentHeight
        if (sWidth > 0 && sHeight > 0) {
            if (resizeWidth && resizeHeight) {
                width = sWidth()
                height = sHeight()
            } else if (resizeHeight) {
                height = (sHeight().toDouble() / sWidth().toDouble() * width).toInt()
            } else if (resizeWidth) {
                width = (sWidth().toDouble() / sHeight().toDouble() * height).toInt()
            }
        }
        width = Math.max(width, suggestedMinimumWidth)
        height = Math.max(height, suggestedMinimumHeight)
        setMeasuredDimension(width, height)
    }

    /**
     * Handle touch events. One finger pans, and two finger pinch and zoom plus panning.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // During non-interruptible anims, ignore all touch events
        anim = if (anim != null && !anim!!.interruptible) {
            requestDisallowInterceptTouchEvent(true)
            return true
        } else {
            if (anim != null && anim!!.listener != null) {
                try {
                    anim!!.listener!!.onInterruptedByUser()
                } catch (e: Exception) {
                    Log.w(TAG, "Error thrown by animation listener", e)
                }
            }
            null
        }

        // Abort if not ready
        if (vTranslate == null) {
            return true
        }
        // Detect flings, taps and double taps
        if (!isQuickScaling && (detector == null || detector!!.onTouchEvent(event))) {
            isZooming = false
            isPanning = false
            maxTouchCount = 0
            return true
        }
        if (vTranslateStart == null) {
            vTranslateStart = PointF(0, 0)
        }
        if (vTranslateBefore == null) {
            vTranslateBefore = PointF(0, 0)
        }
        if (vCenterStart == null) {
            vCenterStart = PointF(0, 0)
        }

        // Store current values so we can send an event if they change
        val scaleBefore = scale
        vTranslateBefore!!.set(vTranslate)
        val handled = onTouchEventInternal(event)
        sendStateChanged(scaleBefore, vTranslateBefore!!, ORIGIN_TOUCH)
        return handled || super.onTouchEvent(event)
    }

    private fun onTouchEventInternal(event: MotionEvent): Boolean {
        val touchCount = event.pointerCount
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_1_DOWN, MotionEvent.ACTION_POINTER_2_DOWN -> {
                anim = null
                requestDisallowInterceptTouchEvent(true)
                maxTouchCount = Math.max(maxTouchCount, touchCount)
                if (touchCount >= 2) {
                    if (zoomEnabled) {
                        // Start pinch to zoom. Calculate distance between touch points and center point of the pinch.
                        val distance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1))
                        scaleStart = scale
                        vDistStart = distance
                        vTranslateStart!![vTranslate!!.x] = vTranslate!!.y
                        vCenterStart!![(event.getX(0) + event.getX(1)) / 2] = (event.getY(0) + event.getY(1)) / 2
                    } else {
                        // Abort all gestures on second touch
                        maxTouchCount = 0
                    }
                    // Cancel long click timer
                    handler.removeMessages(MESSAGE_LONG_CLICK)
                } else if (!isQuickScaling) {
                    // Start one-finger pan
                    vTranslateStart!![vTranslate!!.x] = vTranslate!!.y
                    vCenterStart!![event.x] = event.y

                    // Start long click timer
                    handler.sendEmptyMessageDelayed(MESSAGE_LONG_CLICK, 600)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                var consumed = false
                if (maxTouchCount > 0) {
                    if (touchCount >= 2) {
                        // Calculate new distance between touch points, to scale and pan relative to start values.
                        val vDistEnd = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1))
                        val vCenterEndX = (event.getX(0) + event.getX(1)) / 2
                        val vCenterEndY = (event.getY(0) + event.getY(1)) / 2
                        if (zoomEnabled && (distance(vCenterStart!!.x, vCenterEndX, vCenterStart!!.y, vCenterEndY) > 5 || Math.abs(vDistEnd - vDistStart) > 5 || isPanning)) {
                            isZooming = true
                            isPanning = true
                            consumed = true
                            val previousScale = scale.toDouble()
                            scale = Math.min(maxScale, vDistEnd / vDistStart * scaleStart)
                            if (scale <= minScale()) {
                                // Minimum scale reached so don't pan. Adjust start settings so any expand will zoom in.
                                vDistStart = vDistEnd
                                scaleStart = minScale()
                                vCenterStart!![vCenterEndX] = vCenterEndY
                                vTranslateStart!!.set(vTranslate)
                            } else if (panEnabled) {
                                // Translate to place the source image coordinate that was at the center of the pinch at the start
                                // at the center of the pinch now, to give simultaneous pan + zoom.
                                val vLeftStart = vCenterStart!!.x - vTranslateStart!!.x
                                val vTopStart = vCenterStart!!.y - vTranslateStart!!.y
                                val vLeftNow = vLeftStart * (scale / scaleStart)
                                val vTopNow = vTopStart * (scale / scaleStart)
                                vTranslate!!.x = vCenterEndX - vLeftNow
                                vTranslate!!.y = vCenterEndY - vTopNow
                                if (previousScale * sHeight() < height && scale * sHeight() >= height || previousScale * sWidth() < width && scale * sWidth() >= width) {
                                    fitToBounds(true)
                                    vCenterStart!![vCenterEndX] = vCenterEndY
                                    vTranslateStart!!.set(vTranslate)
                                    scaleStart = scale
                                    vDistStart = vDistEnd
                                }
                            } else if (sRequestedCenter != null) {
                                // With a center specified from code, zoom around that point.
                                vTranslate!!.x = width / 2 - scale * sRequestedCenter!!.x
                                vTranslate!!.y = height / 2 - scale * sRequestedCenter!!.y
                            } else {
                                // With no requested center, scale around the image center.
                                vTranslate!!.x = width / 2 - scale * (sWidth() / 2)
                                vTranslate!!.y = height / 2 - scale * (sHeight() / 2)
                            }
                            fitToBounds(true)
                            refreshRequiredTiles(false)
                        }
                    } else if (isQuickScaling) {
                        // One finger zoom
                        // Stole Google's Magical Formula™ to make sure it feels the exact same
                        var dist = Math.abs(quickScaleVStart!!.y - event.y) * 2 + quickScaleThreshold
                        if (quickScaleLastDistance == -1f) {
                            quickScaleLastDistance = dist
                        }
                        val isUpwards = event.y > quickScaleVLastPoint!!.y
                        quickScaleVLastPoint!![0f] = event.y
                        val spanDiff = Math.abs(1 - dist / quickScaleLastDistance) * 0.5f
                        if (spanDiff > 0.03f || quickScaleMoved) {
                            quickScaleMoved = true
                            var multiplier = 1f
                            if (quickScaleLastDistance > 0) {
                                multiplier = if (isUpwards) 1 + spanDiff else 1 - spanDiff
                            }
                            val previousScale = scale.toDouble()
                            scale = Math.max(minScale(), Math.min(maxScale, scale * multiplier))
                            if (panEnabled) {
                                val vLeftStart = vCenterStart!!.x - vTranslateStart!!.x
                                val vTopStart = vCenterStart!!.y - vTranslateStart!!.y
                                val vLeftNow = vLeftStart * (scale / scaleStart)
                                val vTopNow = vTopStart * (scale / scaleStart)
                                vTranslate!!.x = vCenterStart!!.x - vLeftNow
                                vTranslate!!.y = vCenterStart!!.y - vTopNow
                                if (previousScale * sHeight() < height && scale * sHeight() >= height || previousScale * sWidth() < width && scale * sWidth() >= width) {
                                    fitToBounds(true)
                                    vCenterStart!!.set(sourceToViewCoord(quickScaleSCenter))
                                    vTranslateStart!!.set(vTranslate)
                                    scaleStart = scale
                                    dist = 0f
                                }
                            } else if (sRequestedCenter != null) {
                                // With a center specified from code, zoom around that point.
                                vTranslate!!.x = width / 2 - scale * sRequestedCenter!!.x
                                vTranslate!!.y = height / 2 - scale * sRequestedCenter!!.y
                            } else {
                                // With no requested center, scale around the image center.
                                vTranslate!!.x = width / 2 - scale * (sWidth() / 2)
                                vTranslate!!.y = height / 2 - scale * (sHeight() / 2)
                            }
                        }
                        quickScaleLastDistance = dist
                        fitToBounds(true)
                        refreshRequiredTiles(false)
                        consumed = true
                    } else if (!isZooming) {
                        // One finger pan - translate the image. We do this calculation even with pan disabled so click
                        // and long click behaviour is preserved.
                        val dx = Math.abs(event.x - vCenterStart!!.x)
                        val dy = Math.abs(event.y - vCenterStart!!.y)

                        //On the Samsung S6 long click event does not work, because the dx > 5 usually true
                        val offset = density * 5
                        if (dx > offset || dy > offset || isPanning) {
                            consumed = true
                            vTranslate!!.x = vTranslateStart!!.x + (event.x - vCenterStart!!.x)
                            vTranslate!!.y = vTranslateStart!!.y + (event.y - vCenterStart!!.y)
                            val lastX = vTranslate!!.x
                            val lastY = vTranslate!!.y
                            fitToBounds(true)
                            val atXEdge = lastX != vTranslate!!.x
                            val atYEdge = lastY != vTranslate!!.y
                            val edgeXSwipe = atXEdge && dx > dy && !isPanning
                            val edgeYSwipe = atYEdge && dy > dx && !isPanning
                            val yPan = lastY == vTranslate!!.y && dy > offset * 3
                            if (!edgeXSwipe && !edgeYSwipe && (!atXEdge || !atYEdge || yPan || isPanning)) {
                                isPanning = true
                            } else if (dx > offset || dy > offset) {
                                // Haven't panned the image, and we're at the left or right edge. Switch to page swipe.
                                maxTouchCount = 0
                                handler.removeMessages(MESSAGE_LONG_CLICK)
                                requestDisallowInterceptTouchEvent(false)
                            }
                            if (!panEnabled) {
                                vTranslate!!.x = vTranslateStart!!.x
                                vTranslate!!.y = vTranslateStart!!.y
                                requestDisallowInterceptTouchEvent(false)
                            }
                            refreshRequiredTiles(false)
                        }
                    }
                }
                if (consumed) {
                    handler.removeMessages(MESSAGE_LONG_CLICK)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_POINTER_2_UP -> {
                handler.removeMessages(MESSAGE_LONG_CLICK)
                if (isQuickScaling) {
                    isQuickScaling = false
                    if (!quickScaleMoved) {
                        doubleTapZoom(quickScaleSCenter, vCenterStart)
                    }
                }
                if (maxTouchCount > 0 && (isZooming || isPanning)) {
                    if (isZooming && touchCount == 2) {
                        // Convert from zoom to pan with remaining touch
                        isPanning = true
                        vTranslateStart!![vTranslate!!.x] = vTranslate!!.y
                        if (event.actionIndex == 1) {
                            vCenterStart!![event.getX(0)] = event.getY(0)
                        } else {
                            vCenterStart!![event.getX(1)] = event.getY(1)
                        }
                    }
                    if (touchCount < 3) {
                        // End zooming when only one touch point
                        isZooming = false
                    }
                    if (touchCount < 2) {
                        // End panning when no touch points
                        isPanning = false
                        maxTouchCount = 0
                    }
                    // Trigger load of tiles now required
                    refreshRequiredTiles(true)
                    return true
                }
                if (touchCount == 1) {
                    isZooming = false
                    isPanning = false
                    maxTouchCount = 0
                }
                return true
            }
        }
        return false
    }

    private fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        val parent = parent
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    /**
     * Double tap zoom handler triggered from gesture detector or on touch, depending on whether
     * quick scale is enabled.
     */
    private fun doubleTapZoom(sCenter: PointF?, vFocus: PointF?) {
        if (!panEnabled) {
            if (sRequestedCenter != null) {
                // With a center specified from code, zoom around that point.
                sCenter!!.x = sRequestedCenter!!.x
                sCenter.y = sRequestedCenter!!.y
            } else {
                // With no requested center, scale around the image center.
                sCenter!!.x = (sWidth() / 2).toFloat()
                sCenter.y = (sHeight() / 2).toFloat()
            }
        }
        val doubleTapZoomScale = Math.min(maxScale, doubleTapZoomScale)
        val zoomIn = scale <= doubleTapZoomScale * 0.9
        val targetScale = if (zoomIn) doubleTapZoomScale else minScale()
        if (doubleTapZoomStyle == ZOOM_FOCUS_CENTER_IMMEDIATE) {
            setScaleAndCenter(targetScale, sCenter)
        } else if (doubleTapZoomStyle == ZOOM_FOCUS_CENTER || !zoomIn || !panEnabled) {
            AnimationBuilder(targetScale, sCenter).withInterruptible(false).withDuration(doubleTapZoomDuration.toLong()).withOrigin(ORIGIN_DOUBLE_TAP_ZOOM).start()
        } else if (doubleTapZoomStyle == ZOOM_FOCUS_FIXED) {
            AnimationBuilder(targetScale, sCenter, vFocus).withInterruptible(false).withDuration(doubleTapZoomDuration.toLong()).withOrigin(ORIGIN_DOUBLE_TAP_ZOOM).start()
        }
        invalidate()
    }

    /**
     * Draw method should not be called until the view has dimensions so the first calls are used as triggers to calculate
     * the scaling and tiling required. Once the view is setup, tiles are displayed as they are loaded.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        createPaints()

        // If image or view dimensions are not known yet, abort.
        if (sWidth == 0 || sHeight == 0 || width == 0 || height == 0) {
            return
        }

        // When using tiles, on first render with no tile map ready, initialise it and kick off async base image loading.
        if (tileMap == null && decoder != null) {
            initialiseBaseLayer(getMaxBitmapDimensions(canvas))
        }

        // If image has been loaded or supplied as a bitmap, onDraw may be the first time the view has
        // dimensions and therefore the first opportunity to set scale and translate. If this call returns
        // false there is nothing to be drawn so return immediately.
        if (!checkReady()) {
            return
        }

        // Set scale and translate before draw.
        preDraw()

        // If animating scale, calculate current scale and center with easing equations
        if (anim != null) {
            // Store current values so we can send an event if they change
            val scaleBefore = scale
            if (vTranslateBefore == null) {
                vTranslateBefore = PointF(0, 0)
            }
            vTranslateBefore!!.set(vTranslate)
            var scaleElapsed = System.currentTimeMillis() - anim!!.time
            val finished = scaleElapsed > anim!!.duration
            scaleElapsed = Math.min(scaleElapsed, anim!!.duration)
            scale = ease(anim!!.easing, scaleElapsed, anim!!.scaleStart, anim!!.scaleEnd - anim!!.scaleStart, anim!!.duration)

            // Apply required animation to the focal point
            val vFocusNowX = ease(anim!!.easing, scaleElapsed, anim!!.vFocusStart!!.x, anim!!.vFocusEnd!!.x - anim!!.vFocusStart!!.x, anim!!.duration)
            val vFocusNowY = ease(anim!!.easing, scaleElapsed, anim!!.vFocusStart!!.y, anim!!.vFocusEnd!!.y - anim!!.vFocusStart!!.y, anim!!.duration)
            // Find out where the focal point is at this scale and adjust its position to follow the animation path
            vTranslate!!.x -= sourceToViewX(anim!!.sCenterEnd!!.x) - vFocusNowX
            vTranslate!!.y -= sourceToViewY(anim!!.sCenterEnd!!.y) - vFocusNowY

            // For translate anims, showing the image non-centered is never allowed, for scaling anims it is during the animation.
            fitToBounds(finished || anim!!.scaleStart == anim!!.scaleEnd)
            sendStateChanged(scaleBefore, vTranslateBefore!!, anim!!.origin)
            refreshRequiredTiles(finished)
            if (finished) {
                if (anim!!.listener != null) {
                    try {
                        anim!!.listener!!.onComplete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error thrown by animation listener", e)
                    }
                }
                anim = null
            }
            invalidate()
        }
        if (tileMap != null && isBaseLayerReady()) {

            // Optimum sample size for current scale
            val sampleSize = Math.min(fullImageSampleSize, calculateInSampleSize(scale))

            // First check for missing tiles - if there are any we need the base layer underneath to avoid gaps
            var hasMissingTiles = false
            for ((key, value) in tileMap!!) {
                if (key == sampleSize) {
                    for (tile in value) {
                        if (tile.visible && (tile.loading || tile.bitmap == null)) {
                            hasMissingTiles = true
                        }
                    }
                }
            }

            // Render all loaded tiles. LinkedHashMap used for bottom up rendering - lower res tiles underneath.
            for ((key, value) in tileMap!!) {
                if (key == sampleSize || hasMissingTiles) {
                    for (tile in value) {
                        sourceToViewRect(tile.sRect, tile.vRect)
                        if (!tile.loading && tile.bitmap != null) {
                            if (tileBgPaint != null) {
                                canvas.drawRect(tile.vRect!!, tileBgPaint!!)
                            }
                            if (matrix == null) {
                                matrix = Matrix()
                            }
                            matrix!!.reset()
                            setMatrixArray(srcArray, 0f, 0f, tile.bitmap.width.toFloat(), 0f, tile.bitmap.width.toFloat(), tile.bitmap.height.toFloat(), 0f, tile.bitmap.height.toFloat())
                            if (getRequiredRotation() == ORIENTATION_0) {
                                setMatrixArray(dstArray, tile.vRect!!.left.toFloat(), tile.vRect.top.toFloat(), tile.vRect.right.toFloat(), tile.vRect.top.toFloat(), tile.vRect.right.toFloat(), tile.vRect.bottom.toFloat(), tile.vRect.left.toFloat(), tile.vRect.bottom.toFloat())
                            } else if (getRequiredRotation() == ORIENTATION_90) {
                                setMatrixArray(dstArray, tile.vRect!!.right.toFloat(), tile.vRect.top.toFloat(), tile.vRect.right.toFloat(), tile.vRect.bottom.toFloat(), tile.vRect.left.toFloat(), tile.vRect.bottom.toFloat(), tile.vRect.left.toFloat(), tile.vRect.top.toFloat())
                            } else if (getRequiredRotation() == ORIENTATION_180) {
                                setMatrixArray(dstArray, tile.vRect!!.right.toFloat(), tile.vRect.bottom.toFloat(), tile.vRect.left.toFloat(), tile.vRect.bottom.toFloat(), tile.vRect.left.toFloat(), tile.vRect.top.toFloat(), tile.vRect.right.toFloat(), tile.vRect.top.toFloat())
                            } else if (getRequiredRotation() == ORIENTATION_270) {
                                setMatrixArray(dstArray, tile.vRect!!.left.toFloat(), tile.vRect.bottom.toFloat(), tile.vRect.left.toFloat(), tile.vRect.top.toFloat(), tile.vRect.right.toFloat(), tile.vRect.top.toFloat(), tile.vRect.right.toFloat(), tile.vRect.bottom.toFloat())
                            }
                            matrix!!.setPolyToPoly(srcArray, 0, dstArray, 0, 4)
                            canvas.drawBitmap(tile.bitmap, matrix!!, bitmapPaint)
                            if (debug) {
                                canvas.drawRect(tile.vRect!!, debugPaint!!)
                            }
                        } else if (tile.loading && debug) {
                            canvas.drawText("LOADING", (tile.vRect!!.left + 5).toFloat(), (tile.vRect.top + 35).toFloat(), debugPaint!!)
                        }
                        if (tile.visible && debug) {
                            canvas.drawText("ISS " + tile.sampleSize + " RECT " + tile.sRect!!.top + "," + tile.sRect.left + "," + tile.sRect.bottom + "," + tile.sRect.right, (tile.vRect!!.left + 5).toFloat(), (tile.vRect.top + 15).toFloat(), debugPaint!!)
                        }
                    }
                }
            }
        } else if (bitmap != null) {
            var xScale = scale
            var yScale = scale
            if (bitmapIsPreview) {
                xScale = scale * (sWidth.toFloat() / bitmap!!.width)
                yScale = scale * (sHeight.toFloat() / bitmap!!.height)
            }
            if (matrix == null) {
                matrix = Matrix()
            }
            matrix!!.reset()
            matrix!!.postScale(xScale, yScale)
            matrix!!.postRotate(getRequiredRotation().toFloat())
            matrix!!.postTranslate(vTranslate!!.x, vTranslate!!.y)
            if (getRequiredRotation() == ORIENTATION_180) {
                matrix!!.postTranslate(scale * sWidth, scale * sHeight)
            } else if (getRequiredRotation() == ORIENTATION_90) {
                matrix!!.postTranslate(scale * sHeight, 0f)
            } else if (getRequiredRotation() == ORIENTATION_270) {
                matrix!!.postTranslate(0f, scale * sWidth)
            }
            if (tileBgPaint != null) {
                if (sRect == null) {
                    sRect = RectF()
                }
                sRect!!.set(0f, 0f, if (bitmapIsPreview) bitmap!!.width else sWidth.toFloat(), if (bitmapIsPreview) bitmap!!.height else sHeight.toFloat())
                matrix!!.mapRect(sRect)
                canvas.drawRect(sRect!!, tileBgPaint!!)
            }
            canvas.drawBitmap(bitmap!!, matrix!!, bitmapPaint)
        }
        if (debug) {
            canvas.drawText("Scale: " + String.format(Locale.ENGLISH, "%.2f", scale), 5f, 15f, debugPaint!!)
            canvas.drawText("Translate: " + String.format(Locale.ENGLISH, "%.2f", vTranslate!!.x) + ":" + String.format(Locale.ENGLISH, "%.2f", vTranslate!!.y), 5f, 35f, debugPaint!!)
            val center = getCenter()
            canvas.drawText("Source center: " + String.format(Locale.ENGLISH, "%.2f", center!!.x) + ":" + String.format(Locale.ENGLISH, "%.2f", center.y), 5f, 55f, debugPaint!!)
            debugPaint!!.strokeWidth = 2f
            if (anim != null) {
                val vCenterStart = sourceToViewCoord(anim!!.sCenterStart)
                val vCenterEndRequested = sourceToViewCoord(anim!!.sCenterEndRequested)
                val vCenterEnd = sourceToViewCoord(anim!!.sCenterEnd)
                canvas.drawCircle(vCenterStart!!.x, vCenterStart.y, 10f, debugPaint!!)
                debugPaint!!.color = Color.RED
                canvas.drawCircle(vCenterEndRequested!!.x, vCenterEndRequested.y, 20f, debugPaint!!)
                debugPaint!!.color = Color.BLUE
                canvas.drawCircle(vCenterEnd!!.x, vCenterEnd.y, 25f, debugPaint!!)
                debugPaint!!.color = Color.CYAN
                canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 30f, debugPaint!!)
            }
            if (vCenterStart != null) {
                debugPaint!!.color = Color.RED
                canvas.drawCircle(vCenterStart!!.x, vCenterStart!!.y, 20f, debugPaint!!)
            }
            if (quickScaleSCenter != null) {
                debugPaint!!.color = Color.BLUE
                canvas.drawCircle(sourceToViewX(quickScaleSCenter!!.x), sourceToViewY(quickScaleSCenter!!.y), 35f, debugPaint!!)
            }
            if (quickScaleVStart != null) {
                debugPaint!!.color = Color.CYAN
                canvas.drawCircle(quickScaleVStart!!.x, quickScaleVStart!!.y, 30f, debugPaint!!)
            }
            debugPaint!!.color = Color.MAGENTA
            debugPaint!!.strokeWidth = 1f
        }
    }

    /**
     * Helper method for setting the values of a tile matrix array.
     */
    private fun setMatrixArray(array: FloatArray, f0: Float, f1: Float, f2: Float, f3: Float, f4: Float, f5: Float, f6: Float, f7: Float) {
        array[0] = f0
        array[1] = f1
        array[2] = f2
        array[3] = f3
        array[4] = f4
        array[5] = f5
        array[6] = f6
        array[7] = f7
    }

    /**
     * Checks whether the base layer of tiles or full size bitmap is ready.
     */
    private fun isBaseLayerReady(): Boolean {
        if (bitmap != null && !bitmapIsPreview) {
            return true
        } else if (tileMap != null) {
            var baseLayerReady = true
            for ((key, value) in tileMap!!) {
                if (key == fullImageSampleSize) {
                    for (tile in value) {
                        if (tile.loading || tile.bitmap == null) {
                            baseLayerReady = false
                        }
                    }
                }
            }
            return baseLayerReady
        }
        return false
    }

    /**
     * Check whether view and image dimensions are known and either a preview, full size image or
     * base layer tiles are loaded. First time, send ready event to listener. The next draw will
     * display an image.
     */
    private fun checkReady(): Boolean {
        val ready = width > 0 && height > 0 && sWidth > 0 && sHeight > 0 && (bitmap != null || isBaseLayerReady())
        if (!readySent && ready) {
            preDraw()
            readySent = true
            onReady()
            if (onImageEventListener != null) {
                onImageEventListener!!.onReady()
            }
        }
        return ready
    }

    /**
     * Check whether either the full size bitmap or base layer tiles are loaded. First time, send image
     * loaded event to listener.
     */
    private fun checkImageLoaded(): Boolean {
        val imageLoaded = isBaseLayerReady()
        if (!imageLoadedSent && imageLoaded) {
            preDraw()
            imageLoadedSent = true
            onImageLoaded()
            if (onImageEventListener != null) {
                onImageEventListener!!.onImageLoaded()
            }
        }
        return imageLoaded
    }

    /**
     * Creates Paint objects once when first needed.
     */
    private fun createPaints() {
        if (bitmapPaint == null) {
            bitmapPaint = Paint()
            bitmapPaint!!.isAntiAlias = true
            bitmapPaint!!.isFilterBitmap = true
            bitmapPaint!!.isDither = true
        }
        if (debugPaint == null && debug) {
            debugPaint = Paint()
            debugPaint!!.textSize = 18f
            debugPaint!!.color = Color.MAGENTA
            debugPaint!!.style = Paint.Style.STROKE
        }
    }

    /**
     * Called on first draw when the view has dimensions. Calculates the initial sample size and starts async loading of
     * the base layer image - the whole source subsampled as necessary.
     */
    @Synchronized
    private fun initialiseBaseLayer(maxTileDimensions: Point) {
        debug("initialiseBaseLayer maxTileDimensions=%dx%d", maxTileDimensions.x, maxTileDimensions.y)
        satTemp = ScaleAndTranslate(0f, PointF(0, 0))
        fitToBounds(true, satTemp!!)

        // Load double resolution - next level will be split into four tiles and at the center all four are required,
        // so don't bother with tiling until the next level 16 tiles are needed.
        fullImageSampleSize = calculateInSampleSize(satTemp!!.scale)
        if (fullImageSampleSize > 1) {
            fullImageSampleSize /= 2
        }
        if (fullImageSampleSize == 1 && sRegion == null && sWidth() < maxTileDimensions.x && sHeight() < maxTileDimensions.y) {

            // Whole image is required at native resolution, and is smaller than the canvas max bitmap size.
            // Use BitmapDecoder for better image support.
            decoder!!.recycle()
            decoder = null
            val task = BitmapLoadTask(this, context, bitmapDecoderFactory, uri, false)
            execute(task)
        } else {
            initialiseTileMap(maxTileDimensions)
            val baseGrid = tileMap!![fullImageSampleSize]!!
            for (baseTile in baseGrid) {
                val task = TileLoadTask(this, decoder, baseTile)
                execute(task)
            }
            refreshRequiredTiles(true)
        }
    }

    /**
     * Loads the optimum tiles for display at the current scale and translate, so the screen can be filled with tiles
     * that are at least as high resolution as the screen. Frees up bitmaps that are now off the screen.
     *
     * @param load Whether to load the new tiles needed. Use false while scrolling/panning for performance.
     */
    private fun refreshRequiredTiles(load: Boolean) {
        if (decoder == null || tileMap == null) {
            return
        }
        val sampleSize = Math.min(fullImageSampleSize, calculateInSampleSize(scale))

        // Load tiles of the correct sample size that are on screen. Discard tiles off screen, and those that are higher
        // resolution than required, or lower res than required but not the base layer, so the base layer is always present.
        for ((_, value) in tileMap!!) {
            for (tile in value) {
                if (tile.sampleSize < sampleSize || tile.sampleSize > sampleSize && tile.sampleSize != fullImageSampleSize) {
                    tile.visible = false
                    if (tile.bitmap != null) {
                        tile.bitmap.recycle()
                        tile.bitmap = null
                    }
                }
                if (tile.sampleSize == sampleSize) {
                    if (tileVisible(tile)) {
                        tile.visible = true
                        if (!tile.loading && tile.bitmap == null && load) {
                            val task = TileLoadTask(this, decoder, tile)
                            execute(task)
                        }
                    } else if (tile.sampleSize != fullImageSampleSize) {
                        tile.visible = false
                        if (tile.bitmap != null) {
                            tile.bitmap.recycle()
                            tile.bitmap = null
                        }
                    }
                } else if (tile.sampleSize == fullImageSampleSize) {
                    tile.visible = true
                }
            }
        }
    }

    /**
     * Determine whether tile is visible.
     */
    private fun tileVisible(tile: Tile): Boolean {
        val sVisLeft = viewToSourceX(0f)
        val sVisRight = viewToSourceX(width.toFloat())
        val sVisTop = viewToSourceY(0f)
        val sVisBottom = viewToSourceY(height.toFloat())
        return !(sVisLeft > tile.sRect!!.right || tile.sRect.left > sVisRight || sVisTop > tile.sRect.bottom || tile.sRect.top > sVisBottom)
    }

    /**
     * Sets scale and translate ready for the next draw.
     */
    private fun preDraw() {
        if (width == 0 || height == 0 || sWidth <= 0 || sHeight <= 0) {
            return
        }

        // If waiting to translate to new center position, set translate now
        if (sPendingCenter != null && pendingScale != null) {
            scale = pendingScale
            if (vTranslate == null) {
                vTranslate = PointF()
            }
            vTranslate!!.x = width / 2 - scale * sPendingCenter!!.x
            vTranslate!!.y = height / 2 - scale * sPendingCenter!!.y
            sPendingCenter = null
            pendingScale = null
            fitToBounds(true)
            refreshRequiredTiles(true)
        }

        // On first display of base image set up position, and in other cases make sure scale is correct.
        fitToBounds(false)
    }

    /**
     * Calculates sample size to fit the source image in given bounds.
     */
    private fun calculateInSampleSize(scale: Float): Int {
        var scale = scale
        if (minimumTileDpi > 0) {
            val metrics = resources.displayMetrics
            val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
            scale = minimumTileDpi / averageDpi * scale
        }
        val reqWidth = (sWidth() * scale).toInt()
        val reqHeight = (sHeight() * scale).toInt()

        // Raw height and width of image
        var inSampleSize = 1
        if (reqWidth == 0 || reqHeight == 0) {
            return 32
        }
        if (sHeight() > reqHeight || sWidth() > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            val heightRatio = Math.round(sHeight().toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(sWidth().toFloat() / reqWidth.toFloat())

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        // We want the actual sample size that will be used, so round down to nearest power of 2.
        var power = 1
        while (power * 2 < inSampleSize) {
            power = power * 2
        }
        return power
    }

    /**
     * Adjusts hypothetical future scale and translate values to keep scale within the allowed range and the image on screen. Minimum scale
     * is set so one dimension fills the view and the image is centered on the other dimension. Used to calculate what the target of an
     * animation should be.
     *
     * @param center Whether the image should be centered in the dimension it's too small to fill. While animating this can be false to avoid changes in direction as bounds are reached.
     * @param sat    The scale we want and the translation we're aiming for. The values are adjusted to be valid.
     */
    private fun fitToBounds(center: Boolean, sat: ScaleAndTranslate) {
        var center = center
        if (panLimit == PAN_LIMIT_OUTSIDE && isReady()) {
            center = false
        }
        val vTranslate = sat.vTranslate
        val scale = limitedScale(sat.scale)
        val scaleWidth = scale * sWidth()
        val scaleHeight = scale * sHeight()
        if (panLimit == PAN_LIMIT_CENTER && isReady()) {
            vTranslate.x = Math.max(vTranslate.x, width / 2 - scaleWidth)
            vTranslate.y = Math.max(vTranslate.y, height / 2 - scaleHeight)
        } else if (center) {
            vTranslate.x = Math.max(vTranslate.x, width - scaleWidth)
            vTranslate.y = Math.max(vTranslate.y, height - scaleHeight)
        } else {
            vTranslate.x = Math.max(vTranslate.x, -scaleWidth)
            vTranslate.y = Math.max(vTranslate.y, -scaleHeight)
        }

        // Asymmetric padding adjustments
        val xPaddingRatio = if (paddingLeft > 0 || paddingRight > 0) paddingLeft / (paddingLeft + paddingRight).toFloat() else 0.5f
        val yPaddingRatio = if (paddingTop > 0 || paddingBottom > 0) paddingTop / (paddingTop + paddingBottom).toFloat() else 0.5f
        val maxTx: Float
        val maxTy: Float
        if (panLimit == PAN_LIMIT_CENTER && isReady()) {
            maxTx = Math.max(0, width / 2).toFloat()
            maxTy = Math.max(0, height / 2).toFloat()
        } else if (center) {
            maxTx = Math.max(0f, (width - scaleWidth) * xPaddingRatio)
            maxTy = Math.max(0f, (height - scaleHeight) * yPaddingRatio)
        } else {
            maxTx = Math.max(0, width).toFloat()
            maxTy = Math.max(0, height).toFloat()
        }
        vTranslate.x = Math.min(vTranslate.x, maxTx)
        vTranslate.y = Math.min(vTranslate.y, maxTy)
        sat.scale = scale
    }

    /**
     * Adjusts current scale and translate values to keep scale within the allowed range and the image on screen. Minimum scale
     * is set so one dimension fills the view and the image is centered on the other dimension.
     *
     * @param center Whether the image should be centered in the dimension it's too small to fill. While animating this can be false to avoid changes in direction as bounds are reached.
     */
    private fun fitToBounds(center: Boolean) {
        var init = false
        if (vTranslate == null) {
            init = true
            vTranslate = PointF(0, 0)
        }
        if (satTemp == null) {
            satTemp = ScaleAndTranslate(0, PointF(0, 0))
        }
        satTemp!!.scale = scale
        satTemp!!.vTranslate.set(vTranslate)
        fitToBounds(center, satTemp!!)
        scale = satTemp!!.scale
        vTranslate!!.set(satTemp!!.vTranslate)
        if (init) {
            vTranslate!!.set(vTranslateForSCenter((sWidth() / 2).toFloat(), (sHeight() / 2).toFloat(), scale))
        }
    }

    /**
     * Once source image and view dimensions are known, creates a map of sample size to tile grid.
     */
    private fun initialiseTileMap(maxTileDimensions: Point) {
        debug("initialiseTileMap maxTileDimensions=%dx%d", maxTileDimensions.x, maxTileDimensions.y)
        tileMap = LinkedHashMap()
        var sampleSize = fullImageSampleSize
        var xTiles = 1
        var yTiles = 1
        while (true) {
            var sTileWidth = sWidth() / xTiles
            var sTileHeight = sHeight() / yTiles
            var subTileWidth = sTileWidth / sampleSize
            var subTileHeight = sTileHeight / sampleSize
            while (subTileWidth + xTiles + 1 > maxTileDimensions.x || subTileWidth > width * 1.25 && sampleSize < fullImageSampleSize) {
                xTiles += 1
                sTileWidth = sWidth() / xTiles
                subTileWidth = sTileWidth / sampleSize
            }
            while (subTileHeight + yTiles + 1 > maxTileDimensions.y || subTileHeight > height * 1.25 && sampleSize < fullImageSampleSize) {
                yTiles += 1
                sTileHeight = sHeight() / yTiles
                subTileHeight = sTileHeight / sampleSize
            }
            val tileGrid: MutableList<Tile> = ArrayList(xTiles * yTiles)
            for (x in 0 until xTiles) {
                for (y in 0 until yTiles) {
                    val tile = Tile()
                    tile.sampleSize = sampleSize
                    tile.visible = sampleSize == fullImageSampleSize
                    tile.sRect = Rect(
                            x * sTileWidth,
                            y * sTileHeight,
                            if (x == xTiles - 1) sWidth() else (x + 1) * sTileWidth,
                            if (y == yTiles - 1) sHeight() else (y + 1) * sTileHeight
                    )
                    tile.vRect = Rect(0, 0, 0, 0)
                    tile.fileSRect = Rect(tile.sRect)
                    tileGrid.add(tile)
                }
            }
            tileMap[sampleSize] = tileGrid
            sampleSize /= if (sampleSize == 1) {
                break
            } else {
                2
            }
        }
    }

    /**
     * Async task used to get image details without blocking the UI thread.
     */
    private class TilesInitTask internal constructor(view: SubsamplingScaleImageView, context: Context, decoderFactory: DecoderFactory<out ImageRegionDecoder?>, source: Uri?) : AsyncTask<Void?, Void?, IntArray?>() {
        private val viewRef: WeakReference<SubsamplingScaleImageView>
        private val contextRef: WeakReference<Context>
        private val decoderFactoryRef: WeakReference<DecoderFactory<out ImageRegionDecoder?>>
        private val source: Uri?
        private var decoder: ImageRegionDecoder? = null
        private var exception: Exception? = null
        protected override fun doInBackground(vararg params: Void): IntArray? {
            try {
                val sourceUri = source.toString()
                val context = contextRef.get()
                val decoderFactory: DecoderFactory<out ImageRegionDecoder?>? = decoderFactoryRef.get()
                val view = viewRef.get()
                if (context != null && decoderFactory != null && view != null) {
                    view.debug("TilesInitTask.doInBackground")
                    decoder = decoderFactory.make()
                    val dimensions: Point = decoder!!.init(context, source)
                    var sWidth = dimensions.x
                    var sHeight = dimensions.y
                    val exifOrientation = view.getExifOrientation(context, sourceUri)
                    if (view.sRegion != null) {
                        sWidth = view.sRegion!!.width()
                        sHeight = view.sRegion!!.height()
                    }
                    return intArrayOf(sWidth, sHeight, exifOrientation)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialise bitmap decoder", e)
                exception = e
            }
            return null
        }

        override fun onPostExecute(xyo: IntArray?) {
            val view = viewRef.get()
            if (view != null) {
                if (decoder != null && xyo != null && xyo.size == 3) {
                    view.onTilesInited(decoder, xyo[0], xyo[1], xyo[2])
                } else if (exception != null && view.onImageEventListener != null) {
                    view.onImageEventListener!!.onImageLoadError(exception)
                }
            }
        }

        init {
            viewRef = WeakReference(view)
            contextRef = WeakReference(context)
            decoderFactoryRef = WeakReference<DecoderFactory<out ImageRegionDecoder?>>(decoderFactory)
            this.source = source
        }
    }

    /**
     * Called by worker task when decoder is ready and image size and EXIF orientation is known.
     */
    @Synchronized
    private fun onTilesInited(decoder: ImageRegionDecoder, sWidth: Int, sHeight: Int, sOrientation: Int) {
        debug("onTilesInited sWidth=%d, sHeight=%d, sOrientation=%d", sWidth, sHeight, orientation)
        // If actual dimensions don't match the declared size, reset everything.
        if (this.sWidth > 0 && this.sHeight > 0 && (this.sWidth != sWidth || this.sHeight != sHeight)) {
            reset(false)
            if (bitmap != null) {
                if (!bitmapIsCached) {
                    bitmap!!.recycle()
                }
                bitmap = null
                if (onImageEventListener != null && bitmapIsCached) {
                    onImageEventListener!!.onPreviewReleased()
                }
                bitmapIsPreview = false
                bitmapIsCached = false
            }
        }
        this.decoder = decoder
        this.sWidth = sWidth
        this.sHeight = sHeight
        this.sOrientation = sOrientation
        checkReady()
        if (!checkImageLoaded() && maxTileWidth > 0 && maxTileWidth != TILE_SIZE_AUTO && maxTileHeight > 0 && maxTileHeight != TILE_SIZE_AUTO && width > 0 && height > 0) {
            initialiseBaseLayer(Point(maxTileWidth, maxTileHeight))
        }
        invalidate()
        requestLayout()
    }

    /**
     * Async task used to load images without blocking the UI thread.
     */
    private class TileLoadTask internal constructor(view: SubsamplingScaleImageView, decoder: ImageRegionDecoder?, tile: Tile) : AsyncTask<Void?, Void?, Bitmap?>() {
        private val viewRef: WeakReference<SubsamplingScaleImageView>
        private val decoderRef: WeakReference<ImageRegionDecoder?>
        private val tileRef: WeakReference<Tile>
        private var exception: Exception? = null
        protected override fun doInBackground(vararg params: Void): Bitmap? {
            try {
                val view = viewRef.get()
                val decoder: ImageRegionDecoder? = decoderRef.get()
                val tile = tileRef.get()
                if (decoder != null && tile != null && view != null && decoder.isReady() && tile.visible) {
                    view.debug("TileLoadTask.doInBackground, tile.sRect=%s, tile.sampleSize=%d", tile.sRect!!, tile.sampleSize)
                    synchronized(view.decoderLock) {

                        // Update tile's file sRect according to rotation
                        view.fileSRect(tile.sRect, tile.fileSRect)
                        if (view.sRegion != null) {
                            tile.fileSRect!!.offset(view.sRegion!!.left, view.sRegion!!.top)
                        }
                        return decoder.decodeRegion(tile.fileSRect, tile.sampleSize)
                    }
                } else if (tile != null) {
                    tile.loading = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode tile", e)
                exception = e
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Failed to decode tile - OutOfMemoryError", e)
                exception = RuntimeException(e)
            }
            return null
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            val subsamplingScaleImageView = viewRef.get()
            val tile = tileRef.get()
            if (subsamplingScaleImageView != null && tile != null) {
                if (bitmap != null) {
                    tile.bitmap = bitmap
                    tile.loading = false
                    subsamplingScaleImageView.onTileLoaded()
                } else if (exception != null && subsamplingScaleImageView.onImageEventListener != null) {
                    subsamplingScaleImageView.onImageEventListener!!.onTileLoadError(exception)
                }
            }
        }

        init {
            viewRef = WeakReference(view)
            decoderRef = WeakReference<ImageRegionDecoder?>(decoder)
            tileRef = WeakReference(tile)
            tile.loading = true
        }
    }

    /**
     * Called by worker task when a tile has loaded. Redraws the view.
     */
    @Synchronized
    private fun onTileLoaded() {
        debug("onTileLoaded")
        checkReady()
        checkImageLoaded()
        if (isBaseLayerReady() && bitmap != null) {
            if (!bitmapIsCached) {
                bitmap!!.recycle()
            }
            bitmap = null
            if (onImageEventListener != null && bitmapIsCached) {
                onImageEventListener!!.onPreviewReleased()
            }
            bitmapIsPreview = false
            bitmapIsCached = false
        }
        invalidate()
    }

    /**
     * Async task used to load bitmap without blocking the UI thread.
     */
    private class BitmapLoadTask internal constructor(view: SubsamplingScaleImageView, context: Context, decoderFactory: DecoderFactory<out ImageDecoder?>, source: Uri?, preview: Boolean) : AsyncTask<Void?, Void?, Int?>() {
        private val viewRef: WeakReference<SubsamplingScaleImageView>
        private val contextRef: WeakReference<Context>
        private val decoderFactoryRef: WeakReference<DecoderFactory<out ImageDecoder?>>
        private val source: Uri?
        private val preview: Boolean
        private var bitmap: Bitmap? = null
        private var exception: Exception? = null
        protected override fun doInBackground(vararg params: Void): Int? {
            try {
                val sourceUri = source.toString()
                val context = contextRef.get()
                val decoderFactory: DecoderFactory<out ImageDecoder?>? = decoderFactoryRef.get()
                val view = viewRef.get()
                if (context != null && decoderFactory != null && view != null) {
                    view.debug("BitmapLoadTask.doInBackground")
                    bitmap = decoderFactory.make()!!.decode(context, source)
                    return view.getExifOrientation(context, sourceUri)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bitmap", e)
                exception = e
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Failed to load bitmap - OutOfMemoryError", e)
                exception = RuntimeException(e)
            }
            return null
        }

        override fun onPostExecute(orientation: Int?) {
            val subsamplingScaleImageView = viewRef.get()
            if (subsamplingScaleImageView != null) {
                if (bitmap != null && orientation != null) {
                    if (preview) {
                        subsamplingScaleImageView.onPreviewLoaded(bitmap)
                    } else {
                        subsamplingScaleImageView.onImageLoaded(bitmap, orientation, false)
                    }
                } else if (exception != null && subsamplingScaleImageView.onImageEventListener != null) {
                    if (preview) {
                        subsamplingScaleImageView.onImageEventListener!!.onPreviewLoadError(exception)
                    } else {
                        subsamplingScaleImageView.onImageEventListener!!.onImageLoadError(exception)
                    }
                }
            }
        }

        init {
            viewRef = WeakReference(view)
            contextRef = WeakReference(context)
            decoderFactoryRef = WeakReference<DecoderFactory<out ImageDecoder?>>(decoderFactory)
            this.source = source
            this.preview = preview
        }
    }

    /**
     * Called by worker task when preview image is loaded.
     */
    @Synchronized
    private fun onPreviewLoaded(previewBitmap: Bitmap?) {
        debug("onPreviewLoaded")
        if (bitmap != null || imageLoadedSent) {
            previewBitmap!!.recycle()
            return
        }
        bitmap = if (pRegion != null) {
            Bitmap.createBitmap(previewBitmap!!, pRegion!!.left, pRegion!!.top, pRegion!!.width(), pRegion!!.height())
        } else {
            previewBitmap
        }
        bitmapIsPreview = true
        if (checkReady()) {
            invalidate()
            requestLayout()
        }
    }

    /**
     * Called by worker task when full size image bitmap is ready (tiling is disabled).
     */
    @Synchronized
    private fun onImageLoaded(bitmap: Bitmap?, sOrientation: Int, bitmapIsCached: Boolean) {
        debug("onImageLoaded")
        // If actual dimensions don't match the declared size, reset everything.
        if (sWidth > 0 && sHeight > 0 && (sWidth != bitmap!!.width || sHeight != bitmap.height)) {
            reset(false)
        }
        if (this.bitmap != null && !this.bitmapIsCached) {
            this.bitmap!!.recycle()
        }
        if (this.bitmap != null && this.bitmapIsCached && onImageEventListener != null) {
            onImageEventListener!!.onPreviewReleased()
        }
        bitmapIsPreview = false
        this.bitmapIsCached = bitmapIsCached
        this.bitmap = bitmap
        sWidth = bitmap!!.width
        sHeight = bitmap.height
        this.sOrientation = sOrientation
        val ready = checkReady()
        val imageLoaded = checkImageLoaded()
        if (ready || imageLoaded) {
            invalidate()
            requestLayout()
        }
    }

    /**
     * Helper method for load tasks. Examines the EXIF info on the image file to determine the orientation.
     * This will only work for external files, not assets, resources or other URIs.
     */
    @AnyThread
    private fun getExifOrientation(context: Context, sourceUri: String): Int {
        var exifOrientation = ORIENTATION_0
        if (sourceUri.startsWith(ContentResolver.SCHEME_CONTENT)) {
            var cursor: Cursor? = null
            try {
                val columns = arrayOf(MediaStore.Images.Media.ORIENTATION)
                cursor = context.contentResolver.query(Uri.parse(sourceUri), columns, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val orientation = cursor.getInt(0)
                        if (VALID_ORIENTATIONS.contains(orientation) && orientation != ORIENTATION_USE_EXIF) {
                            exifOrientation = orientation
                        } else {
                            Log.w(TAG, "Unsupported orientation: $orientation")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get orientation of image from media store")
            } finally {
                cursor?.close()
            }
        } else if (sourceUri.startsWith(ImageSource.FILE_SCHEME) && !sourceUri.startsWith(ImageSource.ASSET_SCHEME)) {
            try {
                val exifInterface: ExifInterface = ExifInterface(sourceUri.substring(ImageSource.FILE_SCHEME.length() - 1))
                val orientationAttr = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                if (orientationAttr == ExifInterface.ORIENTATION_NORMAL || orientationAttr == ExifInterface.ORIENTATION_UNDEFINED) {
                    exifOrientation = ORIENTATION_0
                } else if (orientationAttr == ExifInterface.ORIENTATION_ROTATE_90) {
                    exifOrientation = ORIENTATION_90
                } else if (orientationAttr == ExifInterface.ORIENTATION_ROTATE_180) {
                    exifOrientation = ORIENTATION_180
                } else if (orientationAttr == ExifInterface.ORIENTATION_ROTATE_270) {
                    exifOrientation = ORIENTATION_270
                } else {
                    Log.w(TAG, "Unsupported EXIF orientation: $orientationAttr")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get EXIF orientation of image")
            }
        }
        return exifOrientation
    }

    private fun execute(asyncTask: AsyncTask<Void, Void, *>) {
        if (parallelLoadingEnabled) {
            try {
                val executorField = AsyncTask::class.java.getField("THREAD_POOL_EXECUTOR")
                val executor = executorField[null] as Executor
                val executeMethod = AsyncTask::class.java.getMethod("executeOnExecutor", Executor::class.java, Array<Any>::class.java)
                executeMethod.invoke(asyncTask, executor, null)
                return
            } catch (e: Exception) {
                Log.i(TAG, "Failed to execute AsyncTask on thread pool executor, falling back to single threaded executor", e)
            }
        }
        asyncTask.execute()
    }

    private class Tile {
        val sRect: Rect? = null
        val sampleSize = 0
        val bitmap: Bitmap? = null
        val loading = false
        val visible = false

        // Volatile fields instantiated once then updated before use to reduce GC.
        val vRect: Rect? = null
        val fileSRect: Rect? = null
    }

    private class Anim {
        val scaleStart // Scale at start of anim
                = 0f
        val scaleEnd // Scale at end of anim (target)
                = 0f
        val sCenterStart // Source center point at start
                : PointF? = null
        val sCenterEnd // Source center point at end, adjusted for pan limits
                : PointF? = null
        val sCenterEndRequested // Source center point that was requested, without adjustment
                : PointF? = null
        val vFocusStart // View point that was double tapped
                : PointF? = null
        val vFocusEnd // Where the view focal point should be moved to during the anim
                : PointF? = null
        val duration: Long = 500 // How long the anim takes
        val interruptible = true // Whether the anim can be interrupted by a touch
        val easing = EASE_IN_OUT_QUAD // Easing style
        val origin = ORIGIN_ANIM // Animation origin (API, double tap or fling)
        val time = System.currentTimeMillis() // Start time
        val listener // Event listener
                : OnAnimationEventListener? = null
    }

    private class ScaleAndTranslate(val scale: Float, val vTranslate: PointF)

    /**
     * Set scale, center and orientation from saved state.
     */
    private fun restoreState(state: ImageViewState?) {
        if (state != null && state.getCenter() != null && VALID_ORIENTATIONS.contains(state.getOrientation())) {
            orientation = state.getOrientation()
            pendingScale = state.getScale()
            sPendingCenter = state.getCenter()
            invalidate()
        }
    }

    /**
     * By default the View automatically calculates the optimal tile size. Set this to override this, and force an upper limit to the dimensions of the generated tiles. Passing [.TILE_SIZE_AUTO] will re-enable the default behaviour.
     *
     * @param maxPixels Maximum tile size X and Y in pixels.
     */
    fun setMaxTileSize(maxPixels: Int) {
        maxTileWidth = maxPixels
        maxTileHeight = maxPixels
    }

    /**
     * By default the View automatically calculates the optimal tile size. Set this to override this, and force an upper limit to the dimensions of the generated tiles. Passing [.TILE_SIZE_AUTO] will re-enable the default behaviour.
     *
     * @param maxPixelsX Maximum tile width.
     * @param maxPixelsY Maximum tile height.
     */
    fun setMaxTileSize(maxPixelsX: Int, maxPixelsY: Int) {
        maxTileWidth = maxPixelsX
        maxTileHeight = maxPixelsY
    }

    /**
     * In SDK 14 and above, use canvas max bitmap width and height instead of the default 2048, to avoid redundant tiling.
     */
    private fun getMaxBitmapDimensions(canvas: Canvas): Point {
        var maxWidth = 2048
        var maxHeight = 2048
        if (VERSION.SDK_INT >= 14) {
            try {
                maxWidth = Canvas::class.java.getMethod("getMaximumBitmapWidth").invoke(canvas) as Int
                maxHeight = Canvas::class.java.getMethod("getMaximumBitmapHeight").invoke(canvas) as Int
            } catch (e: Exception) {
                // Return default
            }
        }
        return Point(Math.min(maxWidth, maxTileWidth), Math.min(maxHeight, maxTileHeight))
    }

    /**
     * Get source width taking rotation into account.
     */
    private fun sWidth(): Int {
        val rotation = getRequiredRotation()
        return if (rotation == 90 || rotation == 270) {
            sHeight
        } else {
            sWidth
        }
    }

    /**
     * Get source height taking rotation into account.
     */
    private fun sHeight(): Int {
        val rotation = getRequiredRotation()
        return if (rotation == 90 || rotation == 270) {
            sWidth
        } else {
            sHeight
        }
    }

    /**
     * Converts source rectangle from tile, which treats the image file as if it were in the correct orientation already,
     * to the rectangle of the image that needs to be loaded.
     */
    @AnyThread
    private fun fileSRect(sRect: Rect?, target: Rect?) {
        if (getRequiredRotation() == 0) {
            target!!.set(sRect)
        } else if (getRequiredRotation() == 90) {
            target!![sRect!!.top, sHeight - sRect.right, sRect.bottom] = sHeight - sRect.left
        } else if (getRequiredRotation() == 180) {
            target!![sWidth - sRect!!.right, sHeight - sRect.bottom, sWidth - sRect.left] = sHeight - sRect.top
        } else {
            target!![sWidth - sRect!!.bottom, sRect.left, sWidth - sRect.top] = sRect.right
        }
    }

    /**
     * Determines the rotation to be applied to tiles, based on EXIF orientation or chosen setting.
     */
    @AnyThread
    private fun getRequiredRotation(): Int {
        return if (orientation == ORIENTATION_USE_EXIF) {
            sOrientation
        } else {
            orientation
        }
    }

    /**
     * Pythagoras distance between two points.
     */
    private fun distance(x0: Float, x1: Float, y0: Float, y1: Float): Float {
        val x = x0 - x1
        val y = y0 - y1
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    /**
     * Releases all resources the view is using and resets the state, nulling any fields that use significant memory.
     * After you have called this method, the view can be re-used by setting a new image. Settings are remembered
     * but state (scale and center) is forgotten. You can restore these yourself if required.
     */
    fun recycle() {
        reset(true)
        bitmapPaint = null
        debugPaint = null
        tileBgPaint = null
    }

    /**
     * Convert screen to source x coordinate.
     */
    private fun viewToSourceX(vx: Float): Float {
        return if (vTranslate == null) {
            NaN
        } else (vx - vTranslate!!.x) / scale
    }

    /**
     * Convert screen to source y coordinate.
     */
    private fun viewToSourceY(vy: Float): Float {
        return if (vTranslate == null) {
            NaN
        } else (vy - vTranslate!!.y) / scale
    }

    /**
     * Convert screen coordinate to source coordinate.
     */
    fun viewToSourceCoord(vxy: PointF): PointF? {
        return viewToSourceCoord(vxy.x, vxy.y, PointF())
    }

    /**
     * Convert screen coordinate to source coordinate.
     */
    fun viewToSourceCoord(vxy: PointF, sTarget: PointF): PointF? {
        return viewToSourceCoord(vxy.x, vxy.y, sTarget)
    }
    /**
     * Convert screen coordinate to source coordinate.
     */
    /**
     * Convert screen coordinate to source coordinate.
     */
    @JvmOverloads
    fun viewToSourceCoord(vx: Float, vy: Float, sTarget: PointF = PointF()): PointF? {
        if (vTranslate == null) {
            return null
        }
        sTarget[viewToSourceX(vx)] = viewToSourceY(vy)
        return sTarget
    }

    /**
     * Convert source to screen x coordinate.
     */
    private fun sourceToViewX(sx: Float): Float {
        return if (vTranslate == null) {
            NaN
        } else sx * scale + vTranslate!!.x
    }

    /**
     * Convert source to screen y coordinate.
     */
    private fun sourceToViewY(sy: Float): Float {
        return if (vTranslate == null) {
            NaN
        } else sy * scale + vTranslate!!.y
    }

    /**
     * Convert source coordinate to screen coordinate.
     */
    fun sourceToViewCoord(sxy: PointF?): PointF? {
        return sourceToViewCoord(sxy!!.x, sxy.y, PointF())
    }

    /**
     * Convert source coordinate to screen coordinate.
     */
    fun sourceToViewCoord(sxy: PointF, vTarget: PointF): PointF? {
        return sourceToViewCoord(sxy.x, sxy.y, vTarget)
    }
    /**
     * Convert source coordinate to screen coordinate.
     */
    /**
     * Convert source coordinate to screen coordinate.
     */
    @JvmOverloads
    fun sourceToViewCoord(sx: Float, sy: Float, vTarget: PointF = PointF()): PointF? {
        if (vTranslate == null) {
            return null
        }
        vTarget[sourceToViewX(sx)] = sourceToViewY(sy)
        return vTarget
    }

    /**
     * Convert source rect to screen rect, integer values.
     */
    private fun sourceToViewRect(sRect: Rect?, vTarget: Rect?): Rect? {
        vTarget!![sourceToViewX(sRect!!.left.toFloat()).toInt(), sourceToViewY(sRect.top.toFloat()).toInt(), sourceToViewX(sRect.right.toFloat()).toInt()] = sourceToViewY(sRect.bottom.toFloat()).toInt()
        return vTarget
    }

    /**
     * Get the translation required to place a given source coordinate at the center of the screen, with the center
     * adjusted for asymmetric padding. Accepts the desired scale as an argument, so this is independent of current
     * translate and scale. The result is fitted to bounds, putting the image point as near to the screen center as permitted.
     */
    private fun vTranslateForSCenter(sCenterX: Float, sCenterY: Float, scale: Float): PointF {
        val vxCenter = paddingLeft + (width - paddingRight - paddingLeft) / 2
        val vyCenter = paddingTop + (height - paddingBottom - paddingTop) / 2
        if (satTemp == null) {
            satTemp = ScaleAndTranslate(0, PointF(0, 0))
        }
        satTemp!!.scale = scale
        satTemp!!.vTranslate[vxCenter - sCenterX * scale] = vyCenter - sCenterY * scale
        fitToBounds(true, satTemp!!)
        return satTemp!!.vTranslate
    }

    /**
     * Given a requested source center and scale, calculate what the actual center will have to be to keep the image in
     * pan limits, keeping the requested center as near to the middle of the screen as allowed.
     */
    private fun limitedSCenter(sCenterX: Float, sCenterY: Float, scale: Float, sTarget: PointF): PointF {
        val vTranslate = vTranslateForSCenter(sCenterX, sCenterY, scale)
        val vxCenter = paddingLeft + (width - paddingRight - paddingLeft) / 2
        val vyCenter = paddingTop + (height - paddingBottom - paddingTop) / 2
        val sx = (vxCenter - vTranslate.x) / scale
        val sy = (vyCenter - vTranslate.y) / scale
        sTarget[sx] = sy
        return sTarget
    }

    /**
     * Returns the minimum allowed scale.
     */
    private fun minScale(): Float {
        val vPadding = paddingBottom + paddingTop
        val hPadding = paddingLeft + paddingRight
        return if (minimumScaleType == SCALE_TYPE_CENTER_CROP) {
            Math.max((width - hPadding) / sWidth().toFloat(), (height - vPadding) / sHeight().toFloat())
        } else if (minimumScaleType == SCALE_TYPE_CUSTOM && minScale > 0) {
            minScale
        } else {
            Math.min((width - hPadding) / sWidth().toFloat(), (height - vPadding) / sHeight().toFloat())
        }
    }

    /**
     * Adjust a requested scale to be within the allowed limits.
     */
    private fun limitedScale(targetScale: Float): Float {
        var targetScale = targetScale
        targetScale = Math.max(minScale(), targetScale)
        targetScale = Math.min(maxScale, targetScale)
        return targetScale
    }

    /**
     * Apply a selected type of easing.
     *
     * @param type     Easing type, from static fields
     * @param time     Elapsed time
     * @param from     Start value
     * @param change   Target value
     * @param duration Anm duration
     * @return Current value
     */
    private fun ease(type: Int, time: Long, from: Float, change: Float, duration: Long): Float {
        return when (type) {
            EASE_IN_OUT_QUAD -> easeInOutQuad(time, from, change, duration)
            EASE_OUT_QUAD -> easeOutQuad(time, from, change, duration)
            else -> throw IllegalStateException("Unexpected easing type: $type")
        }
    }

    /**
     * Quadratic easing for fling. With thanks to Robert Penner - http://gizma.com/easing/
     *
     * @param time     Elapsed time
     * @param from     Start value
     * @param change   Target value
     * @param duration Anm duration
     * @return Current value
     */
    private fun easeOutQuad(time: Long, from: Float, change: Float, duration: Long): Float {
        val progress = time.toFloat() / duration.toFloat()
        return -change * progress * (progress - 2) + from
    }

    /**
     * Quadratic easing for scale and center animations. With thanks to Robert Penner - http://gizma.com/easing/
     *
     * @param time     Elapsed time
     * @param from     Start value
     * @param change   Target value
     * @param duration Anm duration
     * @return Current value
     */
    private fun easeInOutQuad(time: Long, from: Float, change: Float, duration: Long): Float {
        var timeF = time / (duration / 2f)
        return if (timeF < 1) {
            change / 2f * timeF * timeF + from
        } else {
            timeF--
            -change / 2f * (timeF * (timeF - 2) - 1) + from
        }
    }

    /**
     * Debug logger
     */
    @AnyThread
    private fun debug(message: String, vararg args: Any) {
        if (debug) {
            Log.d(TAG, String.format(message, *args))
        }
    }

    /**
     * Swap the default region decoder implementation for one of your own. You must do this before setting the image file or
     * asset, and you cannot use a custom decoder when using layout XML to set an asset name. Your class must have a
     * public default constructor.
     *
     * @param regionDecoderClass The [ImageRegionDecoder] implementation to use.
     */
    fun setRegionDecoderClass(regionDecoderClass: Class<out ImageRegionDecoder?>?) {
        requireNotNull(regionDecoderClass) { "Decoder class cannot be set to null" }
        regionDecoderFactory = CompatDecoderFactory(regionDecoderClass)
    }

    /**
     * Swap the default region decoder implementation for one of your own. You must do this before setting the image file or
     * asset, and you cannot use a custom decoder when using layout XML to set an asset name.
     *
     * @param regionDecoderFactory The [DecoderFactory] implementation that produces [ImageRegionDecoder]
     * instances.
     */
    fun setRegionDecoderFactory(regionDecoderFactory: DecoderFactory<out ImageRegionDecoder?>?) {
        requireNotNull(regionDecoderFactory) { "Decoder factory cannot be set to null" }
        this.regionDecoderFactory = regionDecoderFactory
    }

    /**
     * Swap the default bitmap decoder implementation for one of your own. You must do this before setting the image file or
     * asset, and you cannot use a custom decoder when using layout XML to set an asset name. Your class must have a
     * public default constructor.
     *
     * @param bitmapDecoderClass The [ImageDecoder] implementation to use.
     */
    fun setBitmapDecoderClass(bitmapDecoderClass: Class<out ImageDecoder?>?) {
        requireNotNull(bitmapDecoderClass) { "Decoder class cannot be set to null" }
        bitmapDecoderFactory = CompatDecoderFactory(bitmapDecoderClass)
    }

    /**
     * Swap the default bitmap decoder implementation for one of your own. You must do this before setting the image file or
     * asset, and you cannot use a custom decoder when using layout XML to set an asset name.
     *
     * @param bitmapDecoderFactory The [DecoderFactory] implementation that produces [ImageDecoder] instances.
     */
    fun setBitmapDecoderFactory(bitmapDecoderFactory: DecoderFactory<out ImageDecoder?>?) {
        requireNotNull(bitmapDecoderFactory) { "Decoder factory cannot be set to null" }
        this.bitmapDecoderFactory = bitmapDecoderFactory
    }

    /**
     * Set the pan limiting style. See static fields. Normally [.PAN_LIMIT_INSIDE] is best, for image galleries.
     */
    fun setPanLimit(panLimit: Int) {
        require(VALID_PAN_LIMITS.contains(panLimit)) { "Invalid pan limit: $panLimit" }
        this.panLimit = panLimit
        if (isReady()) {
            fitToBounds(true)
            invalidate()
        }
    }

    /**
     * Set the minimum scale type. See static fields. Normally [.SCALE_TYPE_CENTER_INSIDE] is best, for image galleries.
     */
    fun setMinimumScaleType(scaleType: Int) {
        require(VALID_SCALE_TYPES.contains(scaleType)) { "Invalid scale type: $scaleType" }
        minimumScaleType = scaleType
        if (isReady()) {
            fitToBounds(true)
            invalidate()
        }
    }

    /**
     * Set the maximum scale allowed. A value of 1 means 1:1 pixels at maximum scale. You may wish to set this according
     * to screen density - on a retina screen, 1:1 may still be too small. Consider using [.setMinimumDpi],
     * which is density aware.
     */
    fun setMaxScale(maxScale: Float) {
        this.maxScale = maxScale
    }

    /**
     * Set the minimum scale allowed. A value of 1 means 1:1 pixels at minimum scale. You may wish to set this according
     * to screen density. Consider using [.setMaximumDpi], which is density aware.
     */
    fun setMinScale(minScale: Float) {
        this.minScale = minScale
    }

    /**
     * This is a screen density aware alternative to [.setMaxScale]; it allows you to express the maximum
     * allowed scale in terms of the minimum pixel density. This avoids the problem of 1:1 scale still being
     * too small on a high density screen. A sensible starting point is 160 - the default used by this view.
     *
     * @param dpi Source image pixel density at maximum zoom.
     */
    fun setMinimumDpi(dpi: Int) {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        setMaxScale(averageDpi / dpi)
    }

    /**
     * This is a screen density aware alternative to [.setMinScale]; it allows you to express the minimum
     * allowed scale in terms of the maximum pixel density.
     *
     * @param dpi Source image pixel density at minimum zoom.
     */
    fun setMaximumDpi(dpi: Int) {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        setMinScale(averageDpi / dpi)
    }

    /**
     * Returns the maximum allowed scale.
     */
    fun getMaxScale(): Float {
        return maxScale
    }

    /**
     * Returns the minimum allowed scale.
     */
    fun getMinScale(): Float {
        return minScale()
    }

    /**
     * By default, image tiles are at least as high resolution as the screen. For a retina screen this may not be
     * necessary, and may increase the likelihood of an OutOfMemoryError. This method sets a DPI at which higher
     * resolution tiles should be loaded. Using a lower number will on average use less memory but result in a lower
     * quality image. 160-240dpi will usually be enough. This should be called before setting the image source,
     * because it affects which tiles get loaded. When using an untiled source image this method has no effect.
     *
     * @param minimumTileDpi Tile loading threshold.
     */
    fun setMinimumTileDpi(minimumTileDpi: Int) {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        this.minimumTileDpi = Math.min(averageDpi, minimumTileDpi.toFloat()).toInt()
        if (isReady()) {
            reset(false)
            invalidate()
        }
    }

    /**
     * Returns the source point at the center of the view.
     */
    fun getCenter(): PointF? {
        val mX = width / 2
        val mY = height / 2
        return viewToSourceCoord(mX.toFloat(), mY.toFloat())
    }

    /**
     * Returns the current scale value.
     */
    fun getScale(): Float {
        return scale
    }

    /**
     * Externally change the scale and translation of the source image. This may be used with getCenter() and getScale()
     * to restore the scale and zoom after a screen rotate.
     *
     * @param scale   New scale to set.
     * @param sCenter New source image coordinate to center on the screen, subject to boundaries.
     */
    fun setScaleAndCenter(scale: Float, sCenter: PointF?) {
        anim = null
        pendingScale = scale
        sPendingCenter = sCenter
        sRequestedCenter = sCenter
        invalidate()
    }

    /**
     * Fully zoom out and return the image to the middle of the screen. This might be useful if you have a view pager
     * and want images to be reset when the user has moved to another page.
     */
    fun resetScaleAndCenter() {
        anim = null
        pendingScale = limitedScale(0f)
        if (isReady()) {
            sPendingCenter = PointF((sWidth() / 2).toFloat(), (sHeight() / 2).toFloat())
        } else {
            sPendingCenter = PointF(0, 0)
        }
        invalidate()
    }

    /**
     * Call to find whether the view is initialised, has dimensions, and will display an image on
     * the next draw. If a preview has been provided, it may be the preview that will be displayed
     * and the full size image may still be loading. If no preview was provided, this is called once
     * the base layer tiles of the full size image are loaded.
     */
    fun isReady(): Boolean {
        return readySent
    }

    /**
     * Called once when the view is initialised, has dimensions, and will display an image on the
     * next draw. This is triggered at the same time as [OnImageEventListener.onReady] but
     * allows a subclass to receive this event without using a listener.
     */
    protected fun onReady() {}

    /**
     * Call to find whether the main image (base layer tiles where relevant) have been loaded. Before
     * this event the view is blank unless a preview was provided.
     */
    fun isImageLoaded(): Boolean {
        return imageLoadedSent
    }

    /**
     * Called once when the full size image or its base layer tiles have been loaded.
     */
    protected fun onImageLoaded() {}

    /**
     * Get source width, ignoring orientation. If [.getOrientation] returns 90 or 270, you can use [.getSHeight]
     * for the apparent width.
     */
    fun getSWidth(): Int {
        return sWidth
    }

    /**
     * Get source height, ignoring orientation. If [.getOrientation] returns 90 or 270, you can use [.getSWidth]
     * for the apparent height.
     */
    fun getSHeight(): Int {
        return sHeight
    }

    /**
     * Returns the orientation setting. This can return [.ORIENTATION_USE_EXIF], in which case it doesn't tell you
     * the applied orientation of the image. For that, use [.getAppliedOrientation].
     */
    fun getOrientation(): Int {
        return orientation
    }

    /**
     * Returns the actual orientation of the image relative to the source file. This will be based on the source file's
     * EXIF orientation if you're using ORIENTATION_USE_EXIF. Values are 0, 90, 180, 270.
     */
    fun getAppliedOrientation(): Int {
        return getRequiredRotation()
    }

    /**
     * Get the current state of the view (scale, center, orientation) for restoration after rotate. Will return null if
     * the view is not ready.
     */
    fun getState(): ImageViewState? {
        return if (vTranslate != null && sWidth > 0 && sHeight > 0) {
            ImageViewState(getScale(), getCenter(), getOrientation())
        } else null
    }

    /**
     * Returns true if zoom gesture detection is enabled.
     */
    fun isZoomEnabled(): Boolean {
        return zoomEnabled
    }

    /**
     * Enable or disable zoom gesture detection. Disabling zoom locks the the current scale.
     */
    fun setZoomEnabled(zoomEnabled: Boolean) {
        this.zoomEnabled = zoomEnabled
    }

    /**
     * Returns true if double tap &amp; swipe to zoom is enabled.
     */
    fun isQuickScaleEnabled(): Boolean {
        return quickScaleEnabled
    }

    /**
     * Enable or disable double tap &amp; swipe to zoom.
     */
    fun setQuickScaleEnabled(quickScaleEnabled: Boolean) {
        this.quickScaleEnabled = quickScaleEnabled
    }

    /**
     * Returns true if pan gesture detection is enabled.
     */
    fun isPanEnabled(): Boolean {
        return panEnabled
    }

    /**
     * Enable or disable pan gesture detection. Disabling pan causes the image to be centered.
     */
    fun setPanEnabled(panEnabled: Boolean) {
        this.panEnabled = panEnabled
        if (!panEnabled && vTranslate != null) {
            vTranslate!!.x = width / 2 - scale * (sWidth() / 2)
            vTranslate!!.y = height / 2 - scale * (sHeight() / 2)
            if (isReady()) {
                refreshRequiredTiles(true)
                invalidate()
            }
        }
    }

    /**
     * Set a solid color to render behind tiles, useful for displaying transparent PNGs.
     *
     * @param tileBgColor Background color for tiles.
     */
    fun setTileBackgroundColor(tileBgColor: Int) {
        if (Color.alpha(tileBgColor) == 0) {
            tileBgPaint = null
        } else {
            tileBgPaint = Paint()
            tileBgPaint!!.style = Paint.Style.FILL
            tileBgPaint!!.color = tileBgColor
        }
        invalidate()
    }

    /**
     * Set the scale the image will zoom in to when double tapped. This also the scale point where a double tap is interpreted
     * as a zoom out gesture - if the scale is greater than 90% of this value, a double tap zooms out. Avoid using values
     * greater than the max zoom.
     *
     * @param doubleTapZoomScale New value for double tap gesture zoom scale.
     */
    fun setDoubleTapZoomScale(doubleTapZoomScale: Float) {
        this.doubleTapZoomScale = doubleTapZoomScale
    }

    /**
     * A density aware alternative to [.setDoubleTapZoomScale]; this allows you to express the scale the
     * image will zoom in to when double tapped in terms of the image pixel density. Values lower than the max scale will
     * be ignored. A sensible starting point is 160 - the default used by this view.
     *
     * @param dpi New value for double tap gesture zoom scale.
     */
    fun setDoubleTapZoomDpi(dpi: Int) {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        setDoubleTapZoomScale(averageDpi / dpi)
    }

    /**
     * Set the type of zoom animation to be used for double taps. See static fields.
     *
     * @param doubleTapZoomStyle New value for zoom style.
     */
    fun setDoubleTapZoomStyle(doubleTapZoomStyle: Int) {
        require(VALID_ZOOM_STYLES.contains(doubleTapZoomStyle)) { "Invalid zoom style: $doubleTapZoomStyle" }
        this.doubleTapZoomStyle = doubleTapZoomStyle
    }

    /**
     * Set the duration of the double tap zoom animation.
     *
     * @param durationMs Duration in milliseconds.
     */
    fun setDoubleTapZoomDuration(durationMs: Int) {
        doubleTapZoomDuration = Math.max(0, durationMs)
    }

    /**
     * Toggle parallel loading. When enabled, tiles are loaded using the thread pool executor available
     * in SDK 11+. In older versions this has no effect. Parallel loading may use more memory and there
     * is a possibility that it will make the tile loading unreliable, but it reduces the chances of
     * an app's background processes blocking loading.
     *
     * @param parallelLoadingEnabled Whether to run AsyncTasks using a thread pool executor.
     */
    fun setParallelLoadingEnabled(parallelLoadingEnabled: Boolean) {
        this.parallelLoadingEnabled = parallelLoadingEnabled
    }

    /**
     * Enables visual debugging, showing tile boundaries and sizes.
     */
    fun setDebug(debug: Boolean) {
        this.debug = debug
    }

    /**
     * Check if an image has been set. The image may not have been loaded and displayed yet.
     *
     * @return If an image is currently set.
     */
    fun hasImage(): Boolean {
        return uri != null || bitmap != null
    }

    /**
     * {@inheritDoc}
     */
    override fun setOnLongClickListener(onLongClickListener: OnLongClickListener?) {
        this.onLongClickListener = onLongClickListener
    }

    /**
     * Add a listener allowing notification of load and error events.
     */
    fun setOnImageEventListener(onImageEventListener: OnImageEventListener?) {
        this.onImageEventListener = onImageEventListener
    }

    /**
     * Add a listener for pan and zoom events.
     */
    fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener?) {
        this.onStateChangedListener = onStateChangedListener
    }

    private fun sendStateChanged(oldScale: Float, oldVTranslate: PointF, origin: Int) {
        if (onStateChangedListener != null) {
            if (scale != oldScale) {
                onStateChangedListener!!.onScaleChanged(scale, origin)
            }
            if (vTranslate != oldVTranslate) {
                onStateChangedListener!!.onCenterChanged(getCenter(), origin)
            }
        }
    }

    /**
     * Creates a panning animation builder, that when started will animate the image to place the given coordinates of
     * the image in the center of the screen. If doing this would move the image beyond the edges of the screen, the
     * image is instead animated to move the center point as near to the center of the screen as is allowed - it's
     * guaranteed to be on screen.
     *
     * @param sCenter Target center point
     * @return [AnimationBuilder] instance. Call [AnimationBuilder.start] to start the anim.
     */
    fun animateCenter(sCenter: PointF?): AnimationBuilder? {
        return if (!isReady()) {
            null
        } else AnimationBuilder(sCenter)
    }

    /**
     * Creates a scale animation builder, that when started will animate a zoom in or out. If this would move the image
     * beyond the panning limits, the image is automatically panned during the animation.
     *
     * @param scale Target scale.
     * @return [AnimationBuilder] instance. Call [AnimationBuilder.start] to start the anim.
     */
    fun animateScale(scale: Float): AnimationBuilder? {
        return if (!isReady()) {
            null
        } else AnimationBuilder(scale)
    }

    /**
     * Creates a scale animation builder, that when started will animate a zoom in or out. If this would move the image
     * beyond the panning limits, the image is automatically panned during the animation.
     *
     * @param scale Target scale.
     * @return [AnimationBuilder] instance. Call [AnimationBuilder.start] to start the anim.
     */
    fun animateScaleAndCenter(scale: Float, sCenter: PointF?): AnimationBuilder? {
        return if (!isReady()) {
            null
        } else AnimationBuilder(scale, sCenter)
    }

    /**
     * Builder class used to set additional options for a scale animation. Create an instance using [.animateScale],
     * then set your options and call [.start].
     */
    inner class AnimationBuilder {
        private val targetScale: Float
        private val targetSCenter: PointF?
        private val vFocus: PointF?
        private var duration: Long = 500
        private var easing = EASE_IN_OUT_QUAD
        private var origin = ORIGIN_ANIM
        private var interruptible = true
        private var panLimited = true
        private var listener: OnAnimationEventListener? = null

        private constructor(sCenter: PointF) {
            targetScale = scale
            targetSCenter = sCenter
            vFocus = null
        }

        private constructor(scale: Float) {
            targetScale = scale
            targetSCenter = getCenter()
            vFocus = null
        }

        private constructor(scale: Float, sCenter: PointF) {
            targetScale = scale
            targetSCenter = sCenter
            vFocus = null
        }

        private constructor(scale: Float, sCenter: PointF, vFocus: PointF) {
            targetScale = scale
            targetSCenter = sCenter
            this.vFocus = vFocus
        }

        /**
         * Desired duration of the anim in milliseconds. Default is 500.
         *
         * @param duration duration in milliseconds.
         * @return this builder for method chaining.
         */
        fun withDuration(duration: Long): AnimationBuilder {
            this.duration = duration
            return this
        }

        /**
         * Whether the animation can be interrupted with a touch. Default is true.
         *
         * @param interruptible interruptible flag.
         * @return this builder for method chaining.
         */
        fun withInterruptible(interruptible: Boolean): AnimationBuilder {
            this.interruptible = interruptible
            return this
        }

        /**
         * Set the easing style. See static fields. [.EASE_IN_OUT_QUAD] is recommended, and the default.
         *
         * @param easing easing style.
         * @return this builder for method chaining.
         */
        fun withEasing(easing: Int): AnimationBuilder {
            require(VALID_EASING_STYLES.contains(easing)) { "Unknown easing type: $easing" }
            this.easing = easing
            return this
        }

        /**
         * Add an animation event listener.
         *
         * @param listener The listener.
         * @return this builder for method chaining.
         */
        fun withOnAnimationEventListener(listener: OnAnimationEventListener?): AnimationBuilder {
            this.listener = listener
            return this
        }

        /**
         * Only for internal use. When set to true, the animation proceeds towards the actual end point - the nearest
         * point to the center allowed by pan limits. When false, animation is in the direction of the requested end
         * point and is stopped when the limit for each axis is reached. The latter behaviour is used for flings but
         * nothing else.
         */
        private fun withPanLimited(panLimited: Boolean): AnimationBuilder {
            this.panLimited = panLimited
            return this
        }

        /**
         * Only for internal use. Indicates what caused the animation.
         */
        private fun withOrigin(origin: Int): AnimationBuilder {
            this.origin = origin
            return this
        }

        /**
         * Starts the animation.
         */
        fun start() {
            if (anim != null && anim!!.listener != null) {
                try {
                    anim!!.listener!!.onInterruptedByNewAnim()
                } catch (e: Exception) {
                    Log.w(TAG, "Error thrown by animation listener", e)
                }
            }
            val vxCenter = paddingLeft + (width - paddingRight - paddingLeft) / 2
            val vyCenter = paddingTop + (height - paddingBottom - paddingTop) / 2
            val targetScale = limitedScale(targetScale)
            val targetSCenter = if (panLimited) limitedSCenter(targetSCenter!!.x, targetSCenter.y, targetScale, PointF()) else targetSCenter!!
            anim = Anim()
            anim!!.scaleStart = scale
            anim!!.scaleEnd = targetScale
            anim!!.time = System.currentTimeMillis()
            anim!!.sCenterEndRequested = targetSCenter
            anim!!.sCenterStart = getCenter()
            anim!!.sCenterEnd = targetSCenter
            anim!!.vFocusStart = sourceToViewCoord(targetSCenter)
            anim!!.vFocusEnd = PointF(
                    vxCenter.toFloat(),
                    vyCenter.toFloat()
            )
            anim!!.duration = duration
            anim!!.interruptible = interruptible
            anim!!.easing = easing
            anim!!.origin = origin
            anim!!.time = System.currentTimeMillis()
            anim!!.listener = listener
            if (vFocus != null) {
                // Calculate where translation will be at the end of the anim
                val vTranslateXEnd = vFocus.x - targetScale * anim!!.sCenterStart!!.x
                val vTranslateYEnd = vFocus.y - targetScale * anim!!.sCenterStart!!.y
                val satEnd = ScaleAndTranslate(targetScale, PointF(vTranslateXEnd, vTranslateYEnd))
                // Fit the end translation into bounds
                fitToBounds(true, satEnd)
                // Adjust the position of the focus point at end so image will be in bounds
                anim!!.vFocusEnd = PointF(
                        vFocus.x + (satEnd.vTranslate.x - vTranslateXEnd),
                        vFocus.y + (satEnd.vTranslate.y - vTranslateYEnd)
                )
            }
            invalidate()
        }
    }

    /**
     * An event listener for animations, allows events to be triggered when an animation completes,
     * is aborted by another animation starting, or is aborted by a touch event. Note that none of
     * these events are triggered if the activity is paused, the image is swapped, or in other cases
     * where the view's internal state gets wiped or draw events stop.
     */
    interface OnAnimationEventListener {
        /**
         * The animation has completed, having reached its endpoint.
         */
        fun onComplete()

        /**
         * The animation has been aborted before reaching its endpoint because the user touched the screen.
         */
        fun onInterruptedByUser()

        /**
         * The animation has been aborted before reaching its endpoint because a new animation has been started.
         */
        fun onInterruptedByNewAnim()
    }

    /**
     * Default implementation of [OnAnimationEventListener] for extension. This does nothing in any method.
     */
    class DefaultOnAnimationEventListener : OnAnimationEventListener {
        override fun onComplete() {}
        override fun onInterruptedByUser() {}
        override fun onInterruptedByNewAnim() {}
    }

    /**
     * An event listener, allowing subclasses and activities to be notified of significant events.
     */
    interface OnImageEventListener {
        /**
         * Called when the dimensions of the image and view are known, and either a preview image,
         * the full size image, or base layer tiles are loaded. This indicates the scale and translate
         * are known and the next draw will display an image. This event can be used to hide a loading
         * graphic, or inform a subclass that it is safe to draw overlays.
         */
        fun onReady()

        /**
         * Called when the full size image is ready. When using tiling, this means the lowest resolution
         * base layer of tiles are loaded, and when tiling is disabled, the image bitmap is loaded.
         * This event could be used as a trigger to enable gestures if you wanted interaction disabled
         * while only a preview is displayed, otherwise for most cases [.onReady] is the best
         * event to listen to.
         */
        fun onImageLoaded()

        /**
         * Called when a preview image could not be loaded. This method cannot be relied upon; certain
         * encoding types of supported image formats can result in corrupt or blank images being loaded
         * and displayed with no detectable error. The view will continue to load the full size image.
         *
         * @param e The exception thrown. This error is logged by the view.
         */
        fun onPreviewLoadError(e: Exception?)

        /**
         * Indicates an error initiliasing the decoder when using a tiling, or when loading the full
         * size bitmap when tiling is disabled. This method cannot be relied upon; certain encoding
         * types of supported image formats can result in corrupt or blank images being loaded and
         * displayed with no detectable error.
         *
         * @param e The exception thrown. This error is also logged by the view.
         */
        fun onImageLoadError(e: Exception?)

        /**
         * Called when an image tile could not be loaded. This method cannot be relied upon; certain
         * encoding types of supported image formats can result in corrupt or blank images being loaded
         * and displayed with no detectable error. Most cases where an unsupported file is used will
         * result in an error caught by [.onImageLoadError].
         *
         * @param e The exception thrown. This error is logged by the view.
         */
        fun onTileLoadError(e: Exception?)

        /**
         * Called when a bitmap set using ImageSource.cachedBitmap is no longer being used by the View.
         * This is useful if you wish to manage the bitmap after the preview is shown
         */
        fun onPreviewReleased()
    }

    /**
     * Default implementation of [OnImageEventListener] for extension. This does nothing in any method.
     */
    class DefaultOnImageEventListener : OnImageEventListener {
        override fun onReady() {}
        override fun onImageLoaded() {}
        override fun onPreviewLoadError(e: Exception?) {}
        override fun onImageLoadError(e: Exception?) {}
        override fun onTileLoadError(e: Exception?) {}
        override fun onPreviewReleased() {}
    }

    /**
     * An event listener, allowing activities to be notified of pan and zoom events. Initialisation
     * and calls made by your code do not trigger events; touch events and animations do. Methods in
     * this listener will be called on the UI thread and may be called very frequently - your
     * implementation should return quickly.
     */
    interface OnStateChangedListener {
        /**
         * The scale has changed. Use with [.getMaxScale] and [.getMinScale] to determine
         * whether the image is fully zoomed in or out.
         *
         * @param newScale The new scale.
         * @param origin   Where the event originated from - one of [.ORIGIN_ANIM], [.ORIGIN_TOUCH].
         */
        fun onScaleChanged(newScale: Float, origin: Int)

        /**
         * The source center has been changed. This can be a result of panning or zooming.
         *
         * @param newCenter The new source center point.
         * @param origin    Where the event originated from - one of [.ORIGIN_ANIM], [.ORIGIN_TOUCH].
         */
        fun onCenterChanged(newCenter: PointF?, origin: Int)
    }

    /**
     * Default implementation of [OnStateChangedListener]. This does nothing in any method.
     */
    class DefaultOnStateChangedListener : OnStateChangedListener {
        override fun onCenterChanged(newCenter: PointF?, origin: Int) {}
        override fun onScaleChanged(newScale: Float, origin: Int) {}
    }

    companion object {
        private val TAG = SubsamplingScaleImageView::class.java.simpleName

        /**
         * Attempt to use EXIF information on the image to rotate it. Works for external files only.
         */
        const val ORIENTATION_USE_EXIF = -1

        /**
         * Display the image file in its native orientation.
         */
        const val ORIENTATION_0 = 0

        /**
         * Rotate the image 90 degrees clockwise.
         */
        const val ORIENTATION_90 = 90

        /**
         * Rotate the image 180 degrees.
         */
        const val ORIENTATION_180 = 180

        /**
         * Rotate the image 270 degrees clockwise.
         */
        const val ORIENTATION_270 = 270
        private val VALID_ORIENTATIONS = Arrays.asList(ORIENTATION_0, ORIENTATION_90, ORIENTATION_180, ORIENTATION_270, ORIENTATION_USE_EXIF)

        /**
         * During zoom animation, keep the point of the image that was tapped in the same place, and scale the image around it.
         */
        const val ZOOM_FOCUS_FIXED = 1

        /**
         * During zoom animation, move the point of the image that was tapped to the center of the screen.
         */
        const val ZOOM_FOCUS_CENTER = 2

        /**
         * Zoom in to and center the tapped point immediately without animating.
         */
        const val ZOOM_FOCUS_CENTER_IMMEDIATE = 3
        private val VALID_ZOOM_STYLES = Arrays.asList(ZOOM_FOCUS_FIXED, ZOOM_FOCUS_CENTER, ZOOM_FOCUS_CENTER_IMMEDIATE)

        /**
         * Quadratic ease out. Not recommended for scale animation, but good for panning.
         */
        const val EASE_OUT_QUAD = 1

        /**
         * Quadratic ease in and out.
         */
        const val EASE_IN_OUT_QUAD = 2
        private val VALID_EASING_STYLES = Arrays.asList(EASE_IN_OUT_QUAD, EASE_OUT_QUAD)

        /**
         * Don't allow the image to be panned off screen. As much of the image as possible is always displayed, centered in the view when it is smaller. This is the best option for galleries.
         */
        const val PAN_LIMIT_INSIDE = 1

        /**
         * Allows the image to be panned until it is just off screen, but no further. The edge of the image will stop when it is flush with the screen edge.
         */
        const val PAN_LIMIT_OUTSIDE = 2

        /**
         * Allows the image to be panned until a corner reaches the center of the screen but no further. Useful when you want to pan any spot on the image to the exact center of the screen.
         */
        const val PAN_LIMIT_CENTER = 3
        private val VALID_PAN_LIMITS = Arrays.asList(PAN_LIMIT_INSIDE, PAN_LIMIT_OUTSIDE, PAN_LIMIT_CENTER)

        /**
         * Scale the image so that both dimensions of the image will be equal to or less than the corresponding dimension of the view. The image is then centered in the view. This is the default behaviour and best for galleries.
         */
        const val SCALE_TYPE_CENTER_INSIDE = 1

        /**
         * Scale the image uniformly so that both dimensions of the image will be equal to or larger than the corresponding dimension of the view. The image is then centered in the view.
         */
        const val SCALE_TYPE_CENTER_CROP = 2

        /**
         * Scale the image so that both dimensions of the image will be equal to or less than the maxScale and equal to or larger than minScale. The image is then centered in the view.
         */
        const val SCALE_TYPE_CUSTOM = 3
        private val VALID_SCALE_TYPES = Arrays.asList(SCALE_TYPE_CENTER_CROP, SCALE_TYPE_CENTER_INSIDE, SCALE_TYPE_CUSTOM)

        /**
         * State change originated from animation.
         */
        const val ORIGIN_ANIM = 1

        /**
         * State change originated from touch gesture.
         */
        const val ORIGIN_TOUCH = 2

        /**
         * State change originated from a fling momentum anim.
         */
        const val ORIGIN_FLING = 3

        /**
         * State change originated from a double tap zoom anim.
         */
        const val ORIGIN_DOUBLE_TAP_ZOOM = 4

        // overrides for the dimensions of the generated tiles
        var TILE_SIZE_AUTO = Int.MAX_VALUE
        private const val MESSAGE_LONG_CLICK = 1
    }

    init {
        density = resources.displayMetrics.density
        setMinimumDpi(160)
        setDoubleTapZoomDpi(160)
        setGestureDetector(context)
        handler = Handler { message ->
            if (message.what == MESSAGE_LONG_CLICK && onLongClickListener != null) {
                maxTouchCount = 0
                super@SubsamplingScaleImageView.setOnLongClickListener(onLongClickListener)
                performLongClick()
                super@SubsamplingScaleImageView.setOnLongClickListener(null)
            }
            true
        }
        // Handle XML attributes
        if (attr != null) {
            val typedAttr = getContext().obtainStyledAttributes(attr, R.styleable.PictureLongScaleImageView)
            if (typedAttr.hasValue(R.styleable.PictureLongScaleImageView_assetName)) {
                val assetName = typedAttr.getString(R.styleable.PictureLongScaleImageView_assetName)
                if (assetName != null && assetName.length > 0) {
                    setImage(ImageSource.asset(assetName).tilingEnabled())
                }
            }
            if (typedAttr.hasValue(R.styleable.PictureLongScaleImageView_src)) {
                val resId = typedAttr.getResourceId(R.styleable.PictureLongScaleImageView_src, 0)
                if (resId > 0) {
                    setImage(ImageSource.resource(resId).tilingEnabled())
                }
            }
            if (typedAttr.hasValue(R.styleable.PictureLongScaleImageView_panEnabled)) {
                setPanEnabled(typedAttr.getBoolean(R.styleable.PictureLongScaleImageView_panEnabled, true))
            }
            if (typedAttr.hasValue(R.styleable.PictureLongScaleImageView_zoomEnabled)) {
                setZoomEnabled(typedAttr.getBoolean(R.styleable.PictureLongScaleImageView_zoomEnabled, true))
            }
            if (typedAttr.hasValue(R.styleable.PictureLongScaleImageView_quickScaleEnabled)) {
                setQuickScaleEnabled(typedAttr.getBoolean(R.styleable.PictureLongScaleImageView_quickScaleEnabled, true))
            }
            if (typedAttr.hasValue(R.styleable.PictureLongScaleImageView_tileBackgroundColor)) {
                setTileBackgroundColor(typedAttr.getColor(R.styleable.PictureLongScaleImageView_tileBackgroundColor, Color.argb(0, 0, 0, 0)))
            }
            typedAttr.recycle()
        }
        quickScaleThreshold = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics)
    }
}