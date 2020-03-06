package com.abelhu.travel.ui.main

import android.content.Context
import com.abelhu.travel.utils.getJson
import com.google.gson.Gson
import com.qicode.merge.ui.ToolsFragment

class MergeFragment : ToolsFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 这里将来要从服务器获取
        userTool = Gson().fromJson(getJson(context, "userTool.json"), UserTool::class.java).apply { initTool(this@MergeFragment) }
    }
}