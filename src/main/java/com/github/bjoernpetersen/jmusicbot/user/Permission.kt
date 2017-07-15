package com.github.bjoernpetersen.jmusicbot.user


enum class Permission(val label: String) {
    SKIP("skip"), DISLIKE("dislike");

    @Deprecated("Will be removed in 0.8.0", replaceWith = ReplaceWith("getLabel()"))
    fun getName(): String = label
}
