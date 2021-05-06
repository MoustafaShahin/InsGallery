package com.luck.picture.lib.widget

import android.content.Context
import android.util.AttributeSet
import com.luck.picture.lib.listener.OnRecyclerViewPreloadMoreListener

/**
 * @author：luck
 * @date：2020-04-14 18:43
 * @describe：RecyclerPreloadView
 */
class RecyclerPreloadView : RecyclerView {
    var isInTheBottom = false
    var isEnabledLoadMore = false
    private var mFirstVisiblePosition = 0
    private var mLastVisiblePosition = 0

    /**
     * reachBottomRow = 1;(default)
     * mean : when the lastVisibleRow is lastRow , call the onReachBottom();
     * reachBottomRow = 2;
     * mean : when the lastVisibleRow is Penultimate Row , call the onReachBottom();
     * And so on
     */
    private var reachBottomRow = BOTTOM_DEFAULT

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

    fun setReachBottomRow(reachBottomRow: Int) {
        var reachBottomRow = reachBottomRow
        if (reachBottomRow < 1) reachBottomRow = 1
        this.reachBottomRow = reachBottomRow
    }

    /**
     * Whether to load more
     *
     * @param isEnabledLoadMore
     */
    fun setEnabledLoadMore(isEnabledLoadMore: Boolean) {
        this.isEnabledLoadMore = isEnabledLoadMore
    }

    /**
     * Whether to load more
     */
    fun isEnabledLoadMore(): Boolean {
        return isEnabledLoadMore
    }

    override fun onScrollStateChanged(newState: Int) {
        super.onScrollStateChanged(newState)
        if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            val layoutManager: RecyclerView.LayoutManager = getLayoutManager()
            if (layoutManager is GridLayoutManager) {
                val linearManager: GridLayoutManager = layoutManager as GridLayoutManager
                mFirstVisiblePosition = linearManager.findFirstVisibleItemPosition()
                mLastVisiblePosition = linearManager.findLastVisibleItemPosition()
            }
        }
    }

    /**
     * Gets the first visible position index
     *
     * @return
     */
    fun getFirstVisiblePosition(): Int {
        return mFirstVisiblePosition
    }

    /**
     * Gets the last visible position index
     *
     * @return
     */
    fun getLastVisiblePosition(): Int {
        return mLastVisiblePosition
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        if (onRecyclerViewPreloadListener != null) {
            if (isEnabledLoadMore) {
                val layoutManager: RecyclerView.LayoutManager = getLayoutManager()
                        ?: throw RuntimeException("LayoutManager is null,Please check it!")
                val adapter: RecyclerView.Adapter<*> = getAdapter()
                        ?: throw RuntimeException("Adapter is null,Please check it!")
                var isReachBottom = false
                if (layoutManager is GridLayoutManager) {
                    val gridLayoutManager: GridLayoutManager = layoutManager as GridLayoutManager
                    val rowCount: Int = adapter.getItemCount() / gridLayoutManager.getSpanCount()
                    val lastVisibleRowPosition: Int = gridLayoutManager.findLastVisibleItemPosition() / gridLayoutManager.getSpanCount()
                    isReachBottom = lastVisibleRowPosition >= rowCount - reachBottomRow
                }
                if (!isReachBottom) {
                    isInTheBottom = false
                } else if (!isInTheBottom) {
                    onRecyclerViewPreloadListener.onRecyclerViewPreloadMore()
                    if (dy > 0) {
                        isInTheBottom = true
                    }
                } else {
                    // 属于首次进入屏幕未滑动且内容未超过一屏，用于确保分页数设置过小导致内容不足二次上拉加载...
                    if (dy == 0) {
                        isInTheBottom = false
                    }
                }
            }
        }
    }

    private var onRecyclerViewPreloadListener: OnRecyclerViewPreloadMoreListener? = null
    fun setOnRecyclerViewPreloadListener(onRecyclerViewPreloadListener: OnRecyclerViewPreloadMoreListener?) {
        this.onRecyclerViewPreloadListener = onRecyclerViewPreloadListener
    }

    companion object {
        private val TAG = RecyclerPreloadView::class.java.simpleName
        private const val BOTTOM_DEFAULT = 1
        const val BOTTOM_PRELOAD = 2
    }
}