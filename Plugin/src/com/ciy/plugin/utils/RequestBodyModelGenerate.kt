package com.ciy.plugin.utils

import com.ciy.plugin.modle.ApiInfoBean
import com.ciy.plugin.modle.JsonSchemaBean
import com.google.gson.Gson
import com.squareup.kotlinpoet.*
import java.io.File

object RequestBodyModelGenerate {

    /**
     * @return Boolean 是否是List
     */
    fun createRequestBodyModel(className: String, rootDir: File, packName: String, apiInfo: ApiInfoBean): Pair<FileSpec?, Boolean> {
        if (apiInfo.req_body_is_json_schema) {
            val cacheTypeList = ArrayList<TypeSpec>()
            val jsonSchema = Gson().fromJson(apiInfo.req_body_other, JsonSchemaBean::class.java)
            if (jsonSchema.type == "object") {
                ResponseModelGenerate.analysisJsonSchema(jsonSchema, className, cacheTypeList)
                val requestBodyFileBuilder = FileSpec.builder(packName, className)
                // 普通类
                cacheTypeList.forEach {
                    requestBodyFileBuilder.addType(it)
                }
                return Pair(requestBodyFileBuilder.build().apply {
                    ResponseModelGenerate.writeTo(this, rootDir, cacheTypeList)
                }, false)
            } else if (jsonSchema.type == "array") {
                // 不支持List套List
                if (jsonSchema.items.type == "object") {
                    ResponseModelGenerate.analysisJsonSchema(jsonSchema.items, className, cacheTypeList)
                    val requestBodyFileBuilder = FileSpec.builder(packName, className)
                    // 普通类
                    cacheTypeList.forEach {
                        requestBodyFileBuilder.addType(it)
                    }
                    return Pair(requestBodyFileBuilder.build().apply {
                        ResponseModelGenerate.writeTo(this, rootDir, cacheTypeList)
                    }, true)
                }
            }
        }
        return Pair(null, false)
    }
}