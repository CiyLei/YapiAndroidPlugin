package com.ciy.plugin.utils

import com.ciy.plugin.ShowInputDialogAction
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ciy.plugin.modle.ApiInfoBean
import com.squareup.kotlinpoet.*
import java.io.File

object ApiServiceGenerate {

    fun createApiService(
        className: String,
        rootDir: File,
        packName: String,
        urlConstantFile: FileSpec,
        apiInfoList: List<ApiInfoBean>
    ): FileSpec {
        val apiServiceBuilder = TypeSpec.interfaceBuilder(className).addKdoc("Retrofit ApiService")
        val propertyMap = ArrayList<String>()
        apiInfoList.forEach {
            try {
                val urlName = url2Name(it.path)
                // 创建返回值模型
                val modelPackName = "$packName.model"
                val responseFile = ResponseModelGenerate.createResponseModel(ResponseModelGenerate.captureName("${urlName}ResModel")
                    , rootDir, modelPackName, it)
                var responseType = Any::class.asTypeName()
                if (responseFile != null) {
//                responseFile.writeTo(rootDir)
                    responseType = ClassName(responseFile.packageName, responseFile.name)
                }
                val observableClass = ClassName("io.reactivex", "Observable").parameterizedBy(responseType)
                // 创建接口方法
                val funSpecBuilder = FunSpec.builder(urlName).addKdoc(it.title)
                    .addModifiers(KModifier.ABSTRACT).returns(observableClass)
                if (it.method == "POST" || it.method == "GET" || it.method == "DELETE" || it.method == "PUT") {
                    val methodClass = ClassName("retrofit2.http", it.method)
                    URLConstantGenerate.propertyMap[it]?.name?.let { itU ->
                        propertyMap.add(itU)
                        funSpecBuilder.addAnnotation(AnnotationSpec.builder(methodClass).addMember(CodeBlock.of(itU)).build())
                    }
                }
                // 传参模型
                if (it.method == "GET") {
                    it.req_query.forEach { req_it ->
                        val queryAnnotationType = ClassName("retrofit2.http", "Query")
                        val queryAnnotation = AnnotationSpec.builder(queryAnnotationType).addMember("%S", req_it.name).build()
                        // 必传
                        val required = req_it.required == "1"
                        funSpecBuilder.addParameter(ParameterSpec.builder(req_it.name, String::class.asTypeName().copy(!required))
                            .addAnnotation(queryAnnotation).addKdoc(req_it.desc).apply {
                                if (!required) {
                                    defaultValue("null")
                                }
                            }.build())
                    }
                } else {
                    val (requestBodyFile, isList) = RequestBodyModelGenerate.createRequestBodyModel(ResponseModelGenerate.captureName("${urlName}ReqModel")
                        , rootDir, modelPackName, it)
                    if (requestBodyFile != null) {
                        val requestBodyType = ClassName(requestBodyFile.packageName, requestBodyFile.name)
                        val bodyAnnotationType = ClassName("retrofit2.http", "Body")
                        if (!isList) {
                            funSpecBuilder.addParameter(ParameterSpec.builder("parameter",requestBodyType.copy(true))
                                .defaultValue("null").addAnnotation(bodyAnnotationType).build())
                        } else {
                            funSpecBuilder.addParameter(ParameterSpec.builder("parameter",LIST.parameterizedBy(requestBodyType).copy(true))
                                .defaultValue("null").addAnnotation(bodyAnnotationType).build())
                        }
                    }
                }
                apiServiceBuilder.addFunction(funSpecBuilder.build())
            } catch (e: Throwable) {
                e.printStackTrace()
                ShowInputDialogAction.generateSourceCodeErrorList.add(Throwable("${it.path} $e"))
            }
        }
        return FileSpec.builder(packName, className).addType(apiServiceBuilder.build())
            .apply {
                propertyMap.forEach { itP ->
                    addImport("${urlConstantFile.packageName}.${urlConstantFile.name}", itP)
                }
            }.build()
    }

    /**
     * url转变量名称
     */
    fun url2Name(url:String): String {
        var result = url.replace(".", "_")
        if (result.startsWith("/")) {
            result = result.substring(1, result.length)
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length - 1)
        }
        val spIndex = result.indexOf("/")
        if (spIndex != -1) {
            return url2Name("${result.subSequence(0, spIndex)}${
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