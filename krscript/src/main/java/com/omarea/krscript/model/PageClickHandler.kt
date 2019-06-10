package com.omarea.krscript.model

import com.omarea.krscript.model.PageInfo

interface PageClickHandler {
    fun openPage(pageInfo: PageInfo)
    fun openPage(title:String, config:String)
}
