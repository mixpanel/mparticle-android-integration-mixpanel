package com.mparticle.kits

enum class UserIdentificationType(val value: String) {
    CUSTOMER_ID("CustomerId"),
    MPID("MPID"),
    OTHER("Other"),
    OTHER_2("Other2"),
    OTHER_3("Other3"),
    OTHER_4("Other4");

    companion object {
        fun fromValue(value: String?): UserIdentificationType? {
            if (value.isNullOrEmpty()) return null
            return entries.find { it.value == value }
        }
    }
}
