package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.common.AppSettingsStore
import com.example.common.ChangelogEntry
import com.example.common.GitHubApiParser
import com.example.common.GitHubBuildInfo
import com.example.common.VersionChangelog
import com.example.common.buildPendingUpdateInfo
import com.example.common.formatFriendlyChangelog
import com.example.common.formatStructuredChangelog
import com.example.common.isRemoteBuildNewer
import com.example.common.normalizeGithubRepo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitHubUpdaterTest {

    @Before
    fun setUp() {
        AppSettingsStore.resetForTesting()
    }

    @Test
    fun testNormalizeGithubRepo() {
        assertEquals("kody/daily-boardgames", normalizeGithubRepo("kody/daily-boardgames"))
        assertEquals("kody/daily-boardgames", normalizeGithubRepo("https://github.com/kody/daily-boardgames"))
        assertEquals("kody/daily-boardgames", normalizeGithubRepo("https://github.com/kody/daily-boardgames.git"))
        assertEquals("kody/daily-boardgames", normalizeGithubRepo("github.com/kody/daily-boardgames/releases"))
        assertNull(normalizeGithubRepo(""))
        assertNull(normalizeGithubRepo("only-owner"))
        assertNull(normalizeGithubRepo("invalid repo/name"))
    }

    @Test
    fun testParseGitHubReleaseAndVersionComparison() {
        val json = """
            {
              "tag_name": "build-5",
              "name": "Daily Boardgames v1.5 (Build #5)",
              "body": "Automated APK build from workflow run #5 (commit abc1234).",
              "html_url": "https://github.com/kody/daily-boardgames/releases/tag/build-5",
              "published_at": "2026-11-25T12:00:00Z",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "daily-boardgames.apk",
                  "browser_download_url": "https://github.com/kody/daily-boardgames/releases/download/build-5/daily-boardgames.apk"
                }
              ]
            }
        """.trimIndent()

        val info = GitHubApiParser.parseRelease("kody/daily-boardgames", json)
        assertNotNull(info)
        requireNotNull(info)
        assertEquals(GitHubBuildInfo.Source.RELEASE, info.source)
        assertEquals(5L, info.runNumber)
        assertEquals(6, info.versionCode)
        assertEquals("v1.5", info.tagOrVersion)
        assertEquals("abc1234", info.commitSha)
        assertEquals(
            "https://github.com/kody/daily-boardgames/releases/download/build-5/daily-boardgames.apk",
            info.apkDownloadUrl
        )

        // Newer than build #4
        assertTrue(
            isRemoteBuildNewer(
                remote = info,
                localVersionCode = 5,
                localVersionName = "1.4",
                localRunNumber = 4L,
                localCommitSha = "0001111"
            )
        )

        // Same build #5 and matching commit -> up to date
        assertFalse(
            isRemoteBuildNewer(
                remote = info,
                localVersionCode = 6,
                localVersionName = "1.5",
                localRunNumber = 5L,
                localCommitSha = "abc1234deadbeef"
            )
        )

        // Newer than base local build (versionCode 1, runNumber 0)
        assertTrue(
            isRemoteBuildNewer(
                remote = info,
                localVersionCode = 1,
                localVersionName = "1.0",
                localRunNumber = 0L,
                localCommitSha = ""
            )
        )
    }

    @Test
    fun testParseWorkflowRunsFallback() {
        val json = """
            {
              "workflow_runs": [
                {
                  "id": 999111,
                  "name": "Build Daily Boardgames APK",
                  "display_title": "Update Solitaire layout",
                  "run_number": 8,
                  "head_sha": "def5678901234",
                  "head_commit": {
                    "id": "def5678901234",
                    "message": "feat(solitaire): align waste pile closer to stock\nfix: disable OLED toggle in light mode\nCo-authored-by: Bot <bot@example.com>"
                  },
                  "html_url": "https://github.com/kody/daily-boardgames/actions/runs/999111",
                  "updated_at": "2026-11-25T12:30:00Z",
                  "conclusion": "success"
                }
              ]
            }
        """.trimIndent()

        val info = GitHubApiParser.parseLatestSuccessfulWorkflowRun("kody/daily-boardgames", json)
        assertNotNull(info)
        requireNotNull(info)
        assertEquals(GitHubBuildInfo.Source.WORKFLOW_RUN, info.source)
        assertEquals(8L, info.runNumber)
        assertEquals(9, info.versionCode)
        assertEquals("https://github.com/kody/daily-boardgames/actions/runs/999111", info.browserUrl)
        assertNull(info.apkDownloadUrl)
        assertEquals(
            listOf(
                "New: Align waste pile closer to stock",
                "Fixed: Disable OLED toggle in light mode"
            ),
            info.changelogItems
        )
    }

    @Test
    fun testFormatFriendlyChangelogCleansReleaseAndCommitMarkdown() {
        val releaseNotes = """
            ### What's New in Build #7
            - feat(settings): add dark mode toggle
            - fix: prevent OLED in light mode
            - improved landscape layout

            ---
            Build #7 • commit abc1234
        """.trimIndent()

        assertEquals(
            listOf(
                "New: Add dark mode toggle",
                "Fixed: Prevent OLED in light mode",
                "Improved landscape layout"
            ),
            formatFriendlyChangelog(releaseNotes)
        )
    }

    @Test
    fun testFormatStructuredChangelogPreservesIndentedReleaseSubBullets() {
        val releaseNotes = """
            ### What's New in Build #22
            - Optimize audio and haptic performance
              - Move SoundManager and HapticManager operations to dedicated background threads to prevent UI stutters.
            - Fix DailyStreakDialog clipping in landscape

            ---
            Build #22 • commit abc1234
        """.trimIndent()

        assertEquals(
            listOf(
                ChangelogEntry("Optimize audio and haptic performance", indentLevel = 0),
                ChangelogEntry(
                    "Move SoundManager and HapticManager operations to dedicated background threads to prevent UI stutters.",
                    indentLevel = 1
                ),
                ChangelogEntry("Fix DailyStreakDialog clipping in landscape", indentLevel = 0)
            ),
            formatStructuredChangelog(releaseNotes, isCommitMessage = false)
        )
    }

    @Test
    fun testFormatStructuredChangelogIndentsCommitBodyExplanations() {
        val commitMessage = """
            Optimize audio and haptic performance

            Move SoundManager and HapticManager operations to dedicated
            background threads to prevent UI stutters.
            - Throttle rapid tones during auto-clear
        """.trimIndent()

        assertEquals(
            listOf(
                ChangelogEntry("Optimize audio and haptic performance", indentLevel = 0),
                ChangelogEntry(
                    "Move SoundManager and HapticManager operations to dedicated background threads to prevent UI stutters.",
                    indentLevel = 1
                ),
                ChangelogEntry("Throttle rapid tones during auto-clear", indentLevel = 1)
            ),
            formatStructuredChangelog(commitMessage, isCommitMessage = true)
        )
    }

    @Test
    fun testBuildPendingUpdateInfoAggregatesChangelogsBetweenInstalledAndLatest() {
        val json = """
            [
              {
                "tag_name": "build-22",
                "name": "Daily Boardgames v1.22 (Build #22)",
                "body": "### What's New in Build #22\n- Indent changelog explanation bullets\n  - Update workflow and app UI",
                "html_url": "https://github.com/kody/daily-boardgames/releases/tag/build-22",
                "published_at": "2026-11-27T10:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": [
                  {
                    "name": "daily-boardgames.apk",
                    "browser_download_url": "https://github.com/kody/daily-boardgames/releases/download/build-22/daily-boardgames.apk"
                  }
                ]
              },
              {
                "tag_name": "build-21",
                "name": "Daily Boardgames v1.21 (Build #21)",
                "body": "### What's New in Build #21\n- Optimize audio and haptic performance",
                "html_url": "https://github.com/kody/daily-boardgames/releases/tag/build-21",
                "published_at": "2026-11-27T09:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": []
              },
              {
                "tag_name": "build-20",
                "name": "Daily Boardgames v1.20 (Build #20)",
                "body": "### What's New in Build #20\n- Fix DailyStreakDialog landscape clipping",
                "html_url": "https://github.com/kody/daily-boardgames/releases/tag/build-20",
                "published_at": "2026-11-27T08:00:00Z",
                "draft": false,
                "prerelease": false,
                "assets": []
              }
            ]
        """.trimIndent()

        val builds = GitHubApiParser.parseReleases("kody/daily-boardgames", json)
        val pending = buildPendingUpdateInfo(
            builds = builds,
            localVersionCode = 21,
            localVersionName = "1.20",
            localRunNumber = 20L,
            localCommitSha = ""
        )

        assertNotNull(pending)
        requireNotNull(pending)
        assertEquals("v1.22", pending.tagOrVersion)
        assertEquals(
            listOf(
                VersionChangelog(
                    versionLabel = "v1.22",
                    title = "Daily Boardgames v1.22 (Build #22)",
                    entries = listOf(
                        ChangelogEntry("Indent changelog explanation bullets", indentLevel = 0),
                        ChangelogEntry("Update workflow and app UI", indentLevel = 1)
                    )
                ),
                VersionChangelog(
                    versionLabel = "v1.21",
                    title = "Daily Boardgames v1.21 (Build #21)",
                    entries = listOf(
                        ChangelogEntry("Optimize audio and haptic performance", indentLevel = 0)
                    )
                )
            ),
            pending.releaseChangelogs
        )
    }

    @Test
    fun testGithubRepoPersistsInAppSettingsStore() {
        val fakeContext = InMemoryPrefsContext()
        AppSettingsStore.init(fakeContext)

        val saved = AppSettingsStore.setGithubRepo(fakeContext, "https://github.com/kody/daily-boardgames")
        assertEquals("kody/daily-boardgames", saved)
        assertEquals("kody/daily-boardgames", AppSettingsStore.settings.value.githubRepo)

        AppSettingsStore.resetForTesting()
        AppSettingsStore.init(fakeContext)
        assertEquals("kody/daily-boardgames", AppSettingsStore.settings.value.githubRepo)
    }

    private class InMemoryPrefsContext : ContextWrapper(null) {
        private val prefsMap = mutableMapOf<String, InMemorySharedPreferences>()

        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return prefsMap.getOrPut(name) { InMemorySharedPreferences() }
        }
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { if (key != null) pending[key] = values }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { if (key != null) pending[key] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { if (key != null) pending[key] = null }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearAll) data.clear()
                for ((k, v) in pending) {
                    if (v == null) data.remove(k) else data[k] = v
                }
            }
        }
    }
}
