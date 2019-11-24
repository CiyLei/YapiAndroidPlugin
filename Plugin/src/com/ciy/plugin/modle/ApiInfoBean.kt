package com.ciy.plugin.modle

data class ApiInfoBean(
    val __v: Int,
    val _id: Int,
    val add_time: Int,
    val api_opened: Boolean,
    val catid: Int,
    val desc: String,
    val edit_uid: Int,
    val index: Int,
    val markdown: String,
    val method: String,
    var path: String,
    val project_id: Int,
    val req_body_form: List<ApiInfoBody>,
    val req_body_is_json_schema: Boolean,
    val req_body_other: String,
    val req_body_type: String,
    val req_headers: List<ApiInfoBody>,
    val req_params: List<ApiInfoBody>,
    val req_query: List<ApiInfoBody>,
    val res_body: String,
    val res_body_is_json_schema: Boolean,
    val res_body_type: String,
    val status: String,
    val tag: List<Any>,
    val title: String,
    val type: String,
    val uid: Int,
    val up_time: Int,
    val username: String
)

data class ApiInfoBody(
    val _id: String,
    val name: String,
    val required: String,
    val value: String,
    val type: String,
    val desc: String
)