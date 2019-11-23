package com.ciy.plugin.utils

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ciy.plugin.modle.ApiInfoBean
import com.squareup.kotlinpoet.*

object ApiServiceGenerate {

    fun createApiService(
        className: String,
        packName: String,
        urlConstantFile: FileSpec,
        apiInfoList: List<ApiInfoBean>
    ): FileSpec {
        val apiServiceBuilder = TypeSpec.interfaceBuilder(className).addKdoc("Retrofit ApiService")
        val propertyMap = ArrayList<String>()
        apiInfoList.forEach {
            val responseType = Any::class.asTypeName()
            val observableClass = ClassName("io.reactivex", "Observable").parameterizedBy(responseType)
            val funSpecBuilder = FunSpec.builder(urlToMethodName(it.path)).addKdoc(it.title)
                .addModifiers(KModifier.ABSTRACT).returns(observableClass)
            if (it.method == "POST" || it.method == "GET" || it.method == "DELETE" || it.method == "PUT") {
                val methodClass = ClassName("retrofit2.http", it.method)
                URLConstantGenerate.propertyMap[it]?.name?.let { itU ->
                    propertyMap.add(itU)
                    funSpecBuilder.addAnnotation(AnnotationSpec.builder(methodClass).addMember(CodeBlock.of(itU)).build())
                }
            }
            apiServiceBuilder.addFunction(funSpecBuilder.build())
        }
        return FileSpec.builder(packName, className).addType(apiServiceBuilder.build())
            .apply {
                propertyMap.forEach { itP ->
                    addImport("${urlConstantFile.packageName}.${urlConstantFile.name}", itP)
                }
            }.build()
    }

    /**
     * url转方法名称
     */
    fun urlToMethodName(url:String): String {
        var result = url
        if (result.startsWith("/")) {
            result = result.substring(1, result.length)
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length - 1)
        }
        val spIndex = result.indexOf("/")
        if (spIndex != -1) {
            return urlToMethodName("${result.subSequence(0, spIndex)}${
            result.subSequence(spIndex + 1, result.length).mapIndexed { index, c ->
                if (index == 0)
                    return@mapIndexed c.toUpperCase()
                return@mapIndexed c
            }.joinToString("")
            }")
        }
        return result
    }
}