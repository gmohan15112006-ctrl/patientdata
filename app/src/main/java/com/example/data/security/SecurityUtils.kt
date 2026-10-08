package com.example.data.security

import java.security.MessageDigest

object SecurityUtils {
    private const val SALT = "HospitalPDMS_HIPAA_Secure_Salt_2026"

    fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltedBytes = (password + SALT).toByteArray(Charsets.UTF_8)
        val hashBytes = md.digest(saltedBytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        return hashPassword(password) == storedHash
    }
}
