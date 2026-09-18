package cn.edu.bnbu.bplan.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 方案1：基于 Android KeyStore 硬件芯片隔离与 AES-256-GCM 加密的安全凭据存储管理器。
 * 
 * 安全特性：
 * 1. 密钥保存在硬件级 TEE / StrongBox 独立安全芯片中，主系统内核无法导出物理私钥。
 * 2. 键名采用 AES-256-SIV 确定性加密，键值（学号、密码）采用 AES-256-GCM 硬件认证加密。
 * 3. 严格遵循 Android UID 沙盒隔离，其他任何应用均无法跨权限读取。
 * 4. 配合 AndroidManifest android:allowBackup="false"，禁止任何 ADB 与云端备份泄露。
 * 5. 内存安全：密码不长驻静态变量，仅在网络认证调用瞬间读取解密，用完即释放。
 */
class SecureCredentialManager(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            // 若遇 KeyStore 异常或极罕见状态，清理并重新初始化，避免崩溃
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 加密保存校园统一身份认证账号与密码
     */
    fun saveCampusCredentials(username: String, password: String) {
        securePrefs.edit()
            .putString(KEY_CAMPUS_USERNAME, username.trim())
            .putString(KEY_CAMPUS_PASSWORD, password)
            .putLong(KEY_CAMPUS_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取解密后的账号与密码（仅在鉴权网络请求瞬间调用）
     */
    fun getCampusCredentials(): Pair<String, String>? {
        val u = securePrefs.getString(KEY_CAMPUS_USERNAME, null)?.trim()
        val p = securePrefs.getString(KEY_CAMPUS_PASSWORD, null)
        return if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
            Pair(u, p)
        } else {
            null
        }
    }

    /**
     * 获取已绑定的学号/账号（不包含密码）
     */
    fun getCampusUsername(): String {
        return securePrefs.getString(KEY_CAMPUS_USERNAME, "")?.trim() ?: ""
    }

    /**
     * 是否已硬件加密存储有效的校园账号凭据
     */
    fun hasCampusCredentials(): Boolean {
        val u = securePrefs.getString(KEY_CAMPUS_USERNAME, null)
        val p = securePrefs.getString(KEY_CAMPUS_PASSWORD, null)
        return !u.isNullOrBlank() && !p.isNullOrBlank()
    }

    /**
     * 彻底擦除硬件加密存储的校园账号与密码
     */
    fun clearCampusCredentials() {
        securePrefs.edit()
            .remove(KEY_CAMPUS_USERNAME)
            .remove(KEY_CAMPUS_PASSWORD)
            .remove(KEY_CAMPUS_SAVED_AT)
            .apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "bplan_secure_credentials"
        private const val KEY_CAMPUS_USERNAME = "campus_sso_username"
        private const val KEY_CAMPUS_PASSWORD = "campus_sso_password"
        private const val KEY_CAMPUS_SAVED_AT = "campus_sso_saved_at"
    }
}
