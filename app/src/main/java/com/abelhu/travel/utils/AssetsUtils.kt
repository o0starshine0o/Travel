package com.abelhu.travel.utils

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


/**
 * 读取assets本地json
 */
fun getJson(context: Context, fileName: String): String {
    val stringBuilder = StringBuilder()
    try {
        //通过管理器打开文件并读取
        val bf = BufferedReader(InputStreamReader(context.assets.open(fileName)))
        while (true) {
            val text = bf.readLine()?.trim()
            if (text == null) break
            else stringBuilder.append(text)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return stringBuilder.toString()
}