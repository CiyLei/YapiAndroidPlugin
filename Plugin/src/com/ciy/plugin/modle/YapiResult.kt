package com.ciy.plugin.modle

data class YapiResult<T>(val errcode: Int, val errmsg: String, val data: T)