package com.omarea.krscript.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.omarea.krscript.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*

class PageLayoutRender(private val mContext: Context,
                       private val itemConfigList: ArrayList<ConfigItemBase>,
                       private val clickListener: OnItemClickListener,
                       private val parent: ListItemView) {

    interface OnItemClickListener {
        fun onPageClick(item: PageInfo, onCompleted: Runnable)
        fun onActionClick(item: ActionInfo, onCompleted: Runnable)
        fun onSwitchClick(item: SwitchInfo, onCompleted: Runnable)
        fun onPickerClick(item: PickerInfo, onCompleted: Runnable)
        fun onItemLongClick(item: ConfigItemBase)
    }


    private fun findItemByDynamicIndex(key: String, actionInfos: ArrayList<ConfigItemBase>): ConfigItemBase? {
        for (item in actionInfos) {
            if (item.index == key) {
                return item
            } else if (item is GroupInfo && item.children.size > 0) {
                val result = findItemByDynamicIndex(key, item.children)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }


    private fun onItemClick(item: ConfigItemBase, listItemView: ListItemView) {
        val handler = Handler(Looper.getMainLooper())
        when (item) {
            is PageInfo -> clickListener.onPageClick(item, Runnable {
                handler.post {
                    if (item.descPollingShell.isNotEmpty()) {
                        item.desc = ScriptEnvironmen.executeResultRoot(mContext, item.descPollingShell)
                    }
                    listItemView.summary = item.desc
                }
            })
            is ActionInfo -> clickListener.onActionClick(item, Runnable {
                handler.post {
                    if (item.descPollingShell.isNotEmpty()) {
                        item.desc = ScriptEnvironmen.executeResultRoot(mContext, item.descPollingShell)
                    }
                    listItemView.summary = item.desc
                }
            })
            is PickerInfo -> clickListener.onPickerClick(item, Runnable {
                handler.post {
                    if (item.descPollingShell.isNotEmpty()) {
                        item.desc = ScriptEnvironmen.executeResultRoot(mContext, item.descPollingShell)
                    }
                    listItemView.summary = item.desc
                }
            })
            is SwitchInfo -> clickListener.onSwitchClick(item, Runnable {
                handler.post {
                    if (item.descPollingShell.isNotEmpty()) {
                        item.desc = ScriptEnvironmen.executeResultRoot(mContext, item.descPollingShell)
                    }
                    if (item.getState != null && !item.getState.isEmpty()) {
                        val shellResult = ScriptEnvironmen.executeResultRoot(mContext, item.getState)
                        item.checked = shellResult == "1" || shellResult.toLowerCase() == "true"
                    }
                    listItemView.summary = item.desc
                    (listItemView as ListItemSwitch).checked = item.checked
                }
            })
        }
    }

    private val onItemClickListener: ListItemView.OnClickListener = object : ListItemView.OnClickListener {
        override fun onClick(listItemView: ListItemView) {
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

    private val onItemLongClickListener = object : ListItemView.OnLongClickListener {
        override fun onLongClick(listItemView: ListItemView) {
            val item = findItemByDynamicIndex(listItemView.index, itemConfigList)
            item?.run {
                clickListener.onItemLongClick(item)
            }
        }
    }

    private fun mapConfigList(parent: ListItemView, actionInfos: ArrayList<ConfigItemBase>) {
        var subGroup: ListItemGroup? = null
        for (index in 0 until actionInfos.size) {
            val it = actionInfos[index]
            try {
                var uiRender: ListItemView? = null
                if (it is PageInfo) {
                    uiRender = createPageItem(it)
                } else if (it is SwitchInfo) {
                    uiRender = createSwitchItem(it)
                } else if (it is ActionInfo) {
                    uiRender = createActionItem(it)
                } else if (it is PickerInfo) {
                    uiRender = createListItem(it)
                } else if (it is TextInfo) {
                    uiRender = if (parent is ListItemGroup) {
                        createTextItemWhite(it)
                    } else {
                        createTextItem(it)
                    }
                } else if (it is GroupInfo) {
                    subGroup = createItemGroup(it)
                    parent.addView(subGroup)
                    if (it.children.size > 0) {
                        mapConfigList(subGroup, it.children)
                    }
                }

                if (uiRender != null) {
                    uiRender.setOnClickListener(this.onItemClickListener)
                    uiRender.setOnLongClickListener(this.onItemLongClickListener)
                    if (subGroup == null) {
                        parent.addView(uiRender)
                    } else {
                        subGroup.addView(uiRender)
                    }
                }
            } catch (ex: Exception) {
                Toast.makeText(mContext, it.title + "界面渲染异常" + ex.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTextItem(info: TextInfo): ListItemView {
        return ListItemText(mContext, R.layout.kr_text_list_item, info)
    }

    private fun createTextItemWhite(info: TextInfo): ListItemView {
        return ListItemText(mContext, R.layout.kr_text_list_item_white, info)
    }

    private fun createListItem(info: PickerInfo): ListItemView {
        return ListItemPicker(mContext, R.layout.kr_action_list_item, info)
    }

    private fun createPageItem(info: PageInfo): ListItemView {
        return ListItemView(mContext, R.layout.kr_page_list_item, info)
    }

    private fun createSwitchItem(info: SwitchInfo): ListItemView {
        return ListItemSwitch(mContext, R.layout.kr_switch_list_item, info)
    }

    private fun createActionItem(info: ActionInfo): ListItemView {
        return ListItemAction(mContext, R.layout.kr_action_list_item, info)
    }

    private fun createItemGroup(info: GroupInfo): ListItemGroup {
        return ListItemGroup(mContext, R.layout.kr_group_list_item, info)
    }

    init {
        mapConfigList(parent, itemConfigList)
    }
}
