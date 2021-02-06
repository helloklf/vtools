package com.omarea.krscript.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.omarea.krscript.R
import com.omarea.krscript.model.*

class PageLayoutRender(private val mContext: Context,
                       private val itemConfigList: ArrayList<NodeInfoBase>,
                       private val clickListener: OnItemClickListener,
                       private val rootGroup: ListItemGroup) {

    interface OnItemClickListener {
        fun onPageClick(item: PageNode, onCompleted: Runnable)
        fun onActionClick(item: ActionNode, onCompleted: Runnable)
        fun onSwitchClick(item: SwitchNode, onCompleted: Runnable)
        fun onPickerClick(item: PickerNode, onCompleted: Runnable)
        fun onItemLongClick(clickableNode: ClickableNode)
    }


    private fun findItemByDynamicIndex(key: String, actionInfos: ArrayList<NodeInfoBase>): NodeInfoBase? {
        for (item in actionInfos) {
            if (item.index == key) {
                return item
            } else if (item is GroupNode && item.children.size > 0) {
                val result = findItemByDynamicIndex(key, item.children)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    private fun getCommonOnExitRunnable(item: NodeInfoBase, node: ListItemClickable): Runnable {
        val handler = Handler(Looper.getMainLooper())
        return Runnable {
            handler.post {
                node.updateViewByShell()

                if (item is RunnableNode && item.updateBlocks != null) {
                    rootGroup.triggerUpdateByKey(item.updateBlocks!!)
                }
            }
        }
    }

    private fun onItemClick(item: NodeInfoBase, listItemView: ListItemClickable) {
        when (item) {
            is PageNode -> clickListener.onPageClick(item, getCommonOnExitRunnable(item, listItemView))
            is ActionNode -> clickListener.onActionClick(item, getCommonOnExitRunnable(item, listItemView))
            is PickerNode -> clickListener.onPickerClick(item, getCommonOnExitRunnable(item, listItemView))
            is SwitchNode -> clickListener.onSwitchClick(item, getCommonOnExitRunnable(item, listItemView))
        }
    }

    private val onItemClickListener: ListItemClickable.OnClickListener = object : ListItemClickable.OnClickListener {
        override fun onClick(listItemView: ListItemClickable) {
            val key = listItemView.index
            try {
                val item = findItemByDynamicIndex(key, itemConfigList)
                if (item == null) {
                    Log.e("onItemClick", "找不到指定ID的项 index: " + key)
                    return
                } else {
                    onItemClick(item, listItemView)
                }
            } catch (ex: Exception) {
            }
        }
    }

    private val onItemLongClickListener = object : ListItemClickable.OnLongClickListener {
        override fun onLongClick(listItemView: ListItemClickable) {
            val item = findItemByDynamicIndex(listItemView.index, itemConfigList)
            if (item is ClickableNode) {
                clickListener.onItemLongClick(item)
            }
        }
    }

    private fun mapConfigList(parent: ListItemGroup, actionInfos: ArrayList<NodeInfoBase>) {
        for (index in 0 until actionInfos.size) {
            val it = actionInfos[index]
            try {
                var uiRender: ListItemView? = null
                if (it is PageNode) {
                    uiRender = createPageItem(it)
                } else if (it is SwitchNode) {
                    uiRender = createSwitchItem(it)
                } else if (it is ActionNode) {
                    uiRender = createActionItem(it)
                } else if (it is PickerNode) {
                    uiRender = createListItem(it)
                } else if (it is TextNode) {
                    uiRender = if (parent.isRootGroup) createTextItem(it) else createTextItemWhite(it)
                } else if (it is GroupNode) {
                    val subGroup = createItemGroup(it)
                    if (it.children.size > 0) {
                        parent.addView(subGroup)
                        mapConfigList(subGroup, it.children)
                    }
                }

                if (uiRender != null) {
                    if (uiRender is ListItemClickable) {
                        uiRender.setOnClickListener(this.onItemClickListener)
                        uiRender.setOnLongClickListener(this.onItemLongClickListener)
                    }
                    parent.addView(uiRender)
                }
            } catch (ex: Exception) {
                Toast.makeText(mContext, it.title + "界面渲染异常" + ex.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTextItem(node: TextNode): ListItemView {
        return ListItemText(mContext, R.layout.kr_text_list_item, node)
    }

    private fun createTextItemWhite(node: TextNode): ListItemView {
        return ListItemText(mContext, R.layout.kr_text_list_item_white, node)
    }

    private fun createListItem(node: PickerNode): ListItemView {
        return ListItemPicker(mContext, node)
    }

    private fun createPageItem(node: PageNode): ListItemView {
        return ListItemPage(mContext, node)
    }

    private fun createSwitchItem(node: SwitchNode): ListItemView {
        return ListItemSwitch(mContext, node)
    }

    private fun createActionItem(node: ActionNode): ListItemView {
        return ListItemAction(mContext, node)
    }

    private fun createItemGroup(node: GroupNode): ListItemGroup {
        return ListItemGroup(mContext, false, node)
    }

    init {
        mapConfigList(rootGroup, itemConfigList)
    }
}
