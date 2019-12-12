package com.ciy.plugin.utils

import com.squareup.kotlinpoet.*
import java.io.File
import java.io.Serializable

object BaseResponseGenerate {

    /**
     * 创建 BaseResponse<T>
     */
    fun createBaseResponse(className: String, rootDir: File, packName: String) {
        val dataP = PropertySpec.builder("data", TypeVariableName("T").copy(true)).mutable().initializer("null").build()
        val successP =
            PropertySpec.builder("success", ClassName("kotlin", "Boolean").copy(true)).mutable().initializer("false")
                .build()
        val msgP =
            PropertySpec.builder("msg", ClassName("kotlin", "String").copy(true)).mutable().initializer("\"\"").build()
        val codeP =
            PropertySpec.builder("code", ClassName("kotlin", "Int").copy(true)).mutable().initializer("0").build()
        val baseResponseType = TypeSpec.classBuilder("BaseResponse")
            .addModifiers(KModifier.OPEN)
            .addTypeVariable(TypeVariableName.invoke("T"))
            .addProperties(arrayListOf(codeP, successP, msgP, dataP))
            .addSuperinterface(Serializable::class).build()
        FileSpec.builder(packName, className).addType(baseResponseType).build().writeTo(rootDir)
    }
}