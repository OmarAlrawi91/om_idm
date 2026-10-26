package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.DownloadEngine
import com.example.engine.MagnetParser
import com.example.model.FileType
import com.example.util.NetworkMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OM IDM", appName)

    val devCredit = context.getString(R.string.developer_credit)
    assertEquals("Developed by Omar Alrawi", devCredit)
  }

  @Test
  fun `test file type detection`() {
    assertEquals(FileType.VIDEO, FileType.fromFileNameOrMime("movie.mp4"))
    assertEquals(FileType.AUDIO, FileType.fromFileNameOrMime("song.mp3"))
    assertEquals(FileType.DOCUMENT, FileType.fromFileNameOrMime("manual.pdf"))
    assertEquals(FileType.ARCHIVE, FileType.fromFileNameOrMime("archive.zip"))
    assertEquals(FileType.TORRENT, FileType.fromFileNameOrMime("test.torrent"))
  }

  @Test
  fun `test auto-folder sorting paths`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = DownloadEngine(context)

    val videoDir = engine.getDownloadDirectory(FileType.VIDEO)
    assertTrue(videoDir.path.endsWith("OM_IDM/Videos") || videoDir.path.endsWith("OM_IDM" + File.separator + "Videos"))

    val musicDir = engine.getDownloadDirectory(FileType.AUDIO)
    assertTrue(musicDir.path.endsWith("OM_IDM/Music") || musicDir.path.endsWith("OM_IDM" + File.separator + "Music"))

    val torrentDir = engine.getDownloadDirectory(FileType.TORRENT)
    assertTrue(torrentDir.path.endsWith("OM_IDM/Torrents") || torrentDir.path.endsWith("OM_IDM" + File.separator + "Torrents"))
  }

  @Test
  fun `test magnet link parsing`() {
    val magnetUri = "magnet:?xt=urn:btih:2b2496a928e461a6b0c2a7f5cf411c5e53303730&dn=ubuntu-24.04-desktop-amd64.iso"
    val magnetInfo = MagnetParser.parse(magnetUri)
    assertNotNull(magnetInfo)
    assertEquals("2b2496a928e461a6b0c2a7f5cf411c5e53303730", magnetInfo?.infoHash)
    assertEquals("ubuntu-24.04-desktop-amd64.iso", magnetInfo?.displayName)
  }

  @Test
  fun `test network monitor initial check`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val networkMonitor = NetworkMonitor(context)
    assertNotNull(networkMonitor.isOnline.value)
  }
}
