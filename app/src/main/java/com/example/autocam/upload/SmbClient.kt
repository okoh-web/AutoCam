
package com.example.autocam.upload

import android.content.Context
import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumSet

class SmbClient(private val context: Context) {

    private companion object {
        private const val BUF_SIZE = 8 * 1024
    }

    suspend fun connectivityCheck(
        base: String,
        deviceId: String,
        dateDir: String,
        username: String,
        passwordAlias: String
    ) = withContext(Dispatchers.IO) {
        val parsed = parseSmbUrl(base)
        useShare(parsed.host, parsed.share, username, passwordAlias) { share ->
            ensureDirectories(share, "$deviceId/$dateDir")
        }
    }

    suspend fun upload(
        contentUri: Uri,
        displayName: String,
        dateDir: String,
        deviceId: String,
        server: String,
        username: String,
        passwordAlias: String
    ): Boolean = withContext(Dispatchers.IO) {
        val parsed = parseSmbUrl(server)
        useShare(parsed.host, parsed.share, username, passwordAlias) { share ->
            val dirPath = "$deviceId/$dateDir"
            ensureDirectories(share, dirPath)
            val remoteName = uniqueName(share, dirPath, displayName)
            val remotePath = "$dirPath/$remoteName"

            context.contentResolver.openInputStream(contentUri).use { input ->
                requireNotNull(input) { "Cannot open input: $contentUri" }
                writeStream(share, remotePath, input)
            }
        }
        true
    }

    private data class ParsedUrl(val host: String, val share: String)

    private fun parseSmbUrl(url: String): ParsedUrl {
        val u = url.trim().removeSuffix("/")
        require(u.lowercase().startsWith("smb://")) { "サーバーは smb://HOST/Share で指定してください" }
        val path = u.removePrefix("smb://")
        val parts = path.split('/')
        require(parts.size >= 2) { "smb://HOST/Share の形式が必要です" }
        val host = parts[0]
        val share = parts[1]
        return ParsedUrl(host, share)
    }

    private inline fun <T> useShare(host: String, shareName: String, username: String, passwordAlias: String, block: (DiskShare) -> T): T {
        val client = SMBClient()
        var connection: Connection? = null
        var session: Session? = null
        var share: DiskShare? = null
        try {
            connection = client.connect(host)
            val (domain, user) = splitDomain(username)
            val pass: CharArray = resolvePassword(passwordAlias)?.takeIf { it.isNotEmpty() } ?: charArrayOf()
            val ac = AuthenticationContext(user, pass, domain)
            session = connection.authenticate(ac)
            share = session.connectShare(shareName) as DiskShare
            return block(share)
        } finally {
            try { share?.close() } catch (_: Throwable) {}
            try { session?.close() } catch (_: Throwable) {}
            try { connection?.close() } catch (_: Throwable) {}
            try { client.close() } catch (_: Throwable) {}
        }
    }

    private fun ensureDirectories(share: DiskShare, path: String) {
        var cur = ""
        for (seg in path.split('/')) {
            if (seg.isEmpty()) continue
            cur = if (cur.isEmpty()) seg else "$cur/$seg"
            if (!share.folderExists(cur)) {
                share.mkdir(cur)
            }
        }
    }

    private fun uniqueName(share: DiskShare, dirPath: String, fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        val stem = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext  = if (dot > 0) fileName.substring(dot) else ""
        var candidate = fileName
        var idx = 1
        while (share.fileExists("$dirPath/$candidate")) {
            candidate = "${stem}_$idx$ext"; idx++
        }
        return candidate
    }

    private fun writeStream(share: DiskShare, remotePath: String, input: InputStream) {
        val file: File = share.openFile(
            remotePath,
            EnumSet.of(AccessMask.GENERIC_WRITE, AccessMask.FILE_WRITE_DATA),
            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_CREATE,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        )
        file.use { f ->
            f.outputStream.use { os ->
                val buf = ByteArray(BUF_SIZE)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    os.write(buf, 0, n)
                }
                os.flush()
            }
        }
    }


    private fun splitDomain(user: String): Pair<String?, String> = when {
        // DOMAIN\user → バックスラッシュは Char オーバーロードで安全に判定・分割
        user.contains('\\') -> user.split('\\', limit = 2).let { parts ->
            // parts[0] = DOMAIN, parts[1] = user
            parts[0] to parts.getOrElse(1) { "" }
        }

        // user@domain → domain は null 扱い（SMBJ の AuthenticationContext に委譲）
        '@' in user -> null to user

        // それ以外 → domain は null
        else -> null to user
    }


    @Suppress("UNUSED_PARAMETER")
    private fun resolvePassword(alias: String): CharArray? = null
}
