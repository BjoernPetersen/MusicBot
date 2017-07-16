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

        @Deprecated("Will be removed in 0.8.0", replaceWith = ReplaceWith("matchByLabel()"))
        @JvmStatic
        fun matchByName(name: String): Permission {
            return matchByLabel(name);
        }
    }

    @Deprecated("Will be removed in 0.8.0", replaceWith = ReplaceWith("getLabel()"))
    fun getName(): String = label
}
