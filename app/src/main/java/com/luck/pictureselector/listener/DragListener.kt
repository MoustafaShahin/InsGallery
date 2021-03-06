package com.luck.pictureselector.listener

/**
 * @author：luck
 * @date：2020-01-13 17:00
 * @describe：拖拽监听事件
 */
interface DragListener {
    /**
     * 是否将 item拖动到删除处，根据状态改变颜色
     *
     * @param isDelete
     */
    fun deleteState(isDelete: Boolean)

    /**
     * 是否于拖拽状态
     *
     * @param start
     */
    fun dragState(isStart: Boolean)
}