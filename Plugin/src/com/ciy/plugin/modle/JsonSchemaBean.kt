package com.ciy.plugin.modle

data class JsonSchemaBean(
    val title: String,
    val description: String,
    val type: Any,
    val properties: Map<String, JsonSchemaBean>,
    val items: JsonSchemaBean,
    val required: List<String>
)