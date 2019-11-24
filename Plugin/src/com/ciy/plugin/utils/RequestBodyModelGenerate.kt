package com.ciy.plugin.utils

import com.ciy.plugin.modle.ApiInfoBean
import com.ciy.plugin.modle.JsonSchemaBean
import com.google.gson.Gson
import com.squareup.kotlinpoet.*
import java.io.File

object RequestBodyModelGenerate {

    fun createRequestBodyModel(className: String, rootDir: File, packName: String, apiInfo: ApiInfoBean): FileSpec? {
        if (apiInfo.req_body_is_json_schema) {
            val cacheTypeList = ArrayList<TypeSpec>()
            val jsonSchema = Gson().fromJson(apiInfo.req_body_other, JsonSchemaBean::class.java)
            val req = ResponseModelGenerate.analysisJsonSchema(jsonSchema, className, cacheTypeList)
            val requestBodyFileBuilder = FileSpec.builder(packName, className)
            if (req is TypeSpec) {
                // 普通类
                cacheTypeList.forEach {
                    requestBodyFileBuilder.addType(it)
                }
                return requestBodyFileBuilder.build().apply {
                    ResponseModelGenerate.writeTo(this, rootDir, cacheTypeList)
                }
            } else if (req is PropertySpec && req.type.toString() == LIST.toString()) {
                // List
                println(1)
            }
        }
        return null
    }
}