package com.ciy.plugin.utils

import com.ciy.plugin.modle.ApiInfoBean
import com.ciy.plugin.modle.JsonSchemaBean
import com.google.gson.Gson
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File

object ResponseModelGenerate {

    /**
     * 数据模型里面如果有多个类的话
     * 在类中会import 主类以外的类 从而导致报错
     * 不知道怎么处理，里面就手动去除一下然后写入文件
     */
    fun writeTo(sourceFile: FileSpec, rootDir: File, cacheTypeList: List<TypeSpec>) {
        var sourceCode = sourceFile.toString()
        for (i in 1 until cacheTypeList.size) {
            sourceCode = sourceCode.replace("import ${cacheTypeList[i].name}\n", "")
        }
        var file = File(rootDir.absolutePath + File.separator +
                sourceFile.packageName.replace(".", File.separator) + File.separator + sourceFile.name + ".kt")
        if (file.exists()) {
            file.delete()
        }
        file = File(rootDir.absolutePath + File.separator +
                sourceFile.packageName.replace(".", File.separator) + File.separator)
        file.mkdirs()
        file = File(file.absolutePath + File.separator + sourceFile.name + ".kt")
        if (file.createNewFile()) {
            file.writeText(sourceCode)
        }
    }

    /**
     * 创建模型类
     */
    fun createResponseModel(className: String, rootDir: File, packName: String, apiInfo: ApiInfoBean): FileSpec? {
        if (apiInfo.res_body_is_json_schema) {
            val cacheTypeList = ArrayList<TypeSpec>()
            val jsonSchema = Gson().fromJson(apiInfo.res_body, JsonSchemaBean::class.java)
            // response 只考虑只有 object 的情况
            analysisJsonSchema(jsonSchema, className, cacheTypeList)
            val responseFileBuilder = FileSpec.builder(packName, className)
            cacheTypeList.forEach {
                responseFileBuilder.addType(it)
            }
            return responseFileBuilder.build().apply {
                writeTo(this, rootDir, cacheTypeList)
            }
        }
        return null
    }

    /**
     * 分析JsonSchema
     */
    fun analysisJsonSchema(jsonSchema: JsonSchemaBean, name: String, cacheTypeList: ArrayList<TypeSpec>, hostClassName: String = ""): Any? {
        when (jsonSchema.type) {
            // 对象
            "object" -> {
                if (jsonSchema.properties.isEmpty()) {
                    // 没有字段的话，视为Any
                    return PropertySpec.builder(name, Any::class.asTypeName()).addKdoc(jsonSchema.description ?: "")
                        .initializer(name).build()
                }
                // 构造方法
                val constructorFunBuilder = FunSpec.constructorBuilder()
                // 所有字段
                val propertyList = ArrayList<PropertySpec>()
                // 循环所有字段
                for ((key, value) in jsonSchema.properties) {
                    // 如果有多个类 那么下一个类名前面加上一个类的名称
                    val result = analysisJsonSchema(value, key, cacheTypeList, "$hostClassName${captureName(name)}")
                    if (result is PropertySpec) {
                        constructorFunBuilder.addParameter(key, result.type).addKdoc(value.description ?: "")
                        propertyList.add(result)
                    } else if (result is TypeSpec) {
                        val className = ClassName("", result.name!!)
                        constructorFunBuilder.addParameter(key, className).addKdoc(value.description ?: "")
                        propertyList.add(
                            PropertySpec.builder(key, className).addKdoc(
                                value.description ?: ""
                            ).initializer(key).mutable().build()
                        )
                    }
                }
                val typeBuilder = TypeSpec.classBuilder("$hostClassName${captureName(name)}").addModifiers(KModifier.DATA)
                    .primaryConstructor(constructorFunBuilder.build()).addKdoc(jsonSchema.description ?: "")
                typeBuilder.addProperties(propertyList)
                val type = typeBuilder.build()
                cacheTypeList.add(0, type)
                return type
            }
            // 列表
            "array" -> {
                val result = analysisJsonSchema(jsonSchema.items, captureName(name), cacheTypeList, hostClassName)
                if (result is PropertySpec) {
                    val listProperty = LIST.parameterizedBy(result.type)
                    return PropertySpec.builder(name, listProperty).addKdoc(jsonSchema.description ?: "")
                        .mutable().initializer(name).build()
                } else if (result is TypeSpec) {
                    val listProperty = LIST.parameterizedBy(ClassName("", result.name!!))
                    return PropertySpec.builder(name, listProperty).addKdoc(jsonSchema.description ?: "")
                        .mutable().initializer(name).build()
                }
            }
            // 基本类型
            "string", "integer", "boolean", "number" -> {
                return PropertySpec.builder(name, getType(jsonSchema.type)).initializer(name)
                    .addKdoc(jsonSchema.description ?: "").mutable()
                    .build()
            }
        }
        return null
    }

    /**
     * 首字母大写
     */
    fun captureName(name: String): String {
        if (name.isNotEmpty()) {
            return name.substring(0, 1).toUpperCase() + name.substring(1)
        }
        return name
    }

    /**
     * 根据json返回的类型得出对应Kotlin的类型
     */
    private fun getType(type: String): ClassName = when (type) {
        "string" -> String::class.asTypeName()
        "integer" -> Int::class.asTypeName()
        "boolean" -> Boolean::class.asTypeName()
        "number" -> Double::class.asTypeName()
        else -> Any::class.asTypeName()
    }
}