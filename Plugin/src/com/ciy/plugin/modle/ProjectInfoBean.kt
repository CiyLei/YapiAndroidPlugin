package com.ciy.plugin.modle

/**
 * 项目基本信息
 */
data class ProjectInfoBean(
    val _id: Int,
    val add_time: Int,
    val basepath: String,
    val cat: List<Any>,
    val color: String,
    val env: List<Env>,
    val group_id: Int,
    val icon: String,
    val is_json5: Boolean,
    val is_mock_open: Boolean,
    val name: String,
    val project_type: String,
    val role: Boolean,
    val strice: Boolean,
    val switch_notice: Boolean,
    val tag: List<Any>,
    val uid: Int,
    val up_time: Int
)

data class Env(
    val _id: String,
    val domain: String,
    val global: List<Any>,
    val header: List<Any>,
    val name: String
)