package me.simpleHook.core.constant

import android.os.Process

/**
 * 记录（record）文件的目录计算。
 *
 * 老实现把路径写死成 /storage/emulated/0/Android/media/...，
 * 在分身空间（user 128）里注入进程会往 user 0 的目录写，
 * sdcardfs 直接 Permission denied，记录既写不进去也读不出来。
 *
 * 这里统一按「当前进程所属用户」计算：
 *  - 生产端（注入在宿主进程里）走 /storage/emulated/<uid/100000>/...，
 *    分身里自然落到 /data/media/128/...；
 *  - 读取端（root）走 /data/media/<userId>/...，可以跨用户枚举，
 *    不受 /storage/self 在 su 里被解析成 user 0 的影响。
 */
object RecordPath {
    private const val PER_USER_RANGE = 100_000
    private const val MEDIA_DIR = "Android/media"
    private const val APP_DIR = "SimpleHook/record"

    fun currentUserId(): Int = Process.myUid() / PER_USER_RANGE

    /** 生产端：当前用户下，宿主 app 自己的 Android/media 记录根目录。 */
    fun recordRootForCurrentUser(packageName: String): String =
        "/storage/emulated/${currentUserId()}/$MEDIA_DIR/$packageName/$APP_DIR"

    /** 兼容旧路径（主空间下等价于 current user 0 的路径）。 */
    fun recordRootForSelfPrimary(packageName: String): String =
        "/storage/self/primary/$MEDIA_DIR/$packageName/$APP_DIR"

    /** 读取端：显式按用户拼，root 可直接读。 */
    fun recordRootForUser(userId: Int, packageName: String): String =
        "/data/media/$userId/$MEDIA_DIR/$packageName/$APP_DIR"

    fun sourceDir(recordRoot: String, sourceDirName: String): String =
        "$recordRoot/$sourceDirName"

    fun readyDir(recordRoot: String, sourceDirName: String): String =
        "${sourceDir(recordRoot, sourceDirName)}/ready"

    fun tmpDir(recordRoot: String, sourceDirName: String): String =
        "${sourceDir(recordRoot, sourceDirName)}/tmp"

    fun manifestPath(recordRoot: String, sourceDirName: String): String =
        "${sourceDir(recordRoot, sourceDirName)}/manifest.txt"
}
