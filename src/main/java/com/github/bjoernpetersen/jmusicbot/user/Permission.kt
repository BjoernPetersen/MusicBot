package com.github.bjoernpetersen.jmusicbot.user


enum class Permission(val label: String) {
    SKIP("skip"), DISLIKE("dislike");

    companion object {
        @JvmStatic
        fun matchByLabel(label: String): Permission {
            for (permission in enumValues<Permission>()) {
                if (permission.label == label) {
                    return permission
                }
            }
            throw IllegalArgumentException()
        }
    }
}
