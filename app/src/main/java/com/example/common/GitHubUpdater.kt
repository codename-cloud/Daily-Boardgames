package com.example.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubBuildInfo(
    val repo: String,
    val source: Source,
    val title: String,
    val tagOrVersion: String,
    val runNumber: Long?,
    val versionCode: Int?,
    val commitSha: String?,
    val notes: String,
    val publishedAt: String,
    val browserUrl: String,
    val apkDownloadUrl: String?
) {
    enum class Source {
        RELEASE,
        WORKFLOW_RUN
    }
}

sealed interface GitHubCheckStatus {
    data object Idle : GitHubCheckStatus
    data object Checking : GitHubCheckStatus
    data class UpToDate(
        val repo: String,
        val latestLabel: String
    ) : GitHubCheckStatus

    data class UpdateAvailable(
        val build: GitHubBuildInfo
    ) : GitHubCheckStatus

    data class Error(
        val message: String
    ) : GitHubCheckStatus
}

data class GitHubUpdateUiState(
    val status: GitHubCheckStatus = GitHubCheckStatus.Idle,
    val lastCheckedRepo: String = ""
) {
    val availableUpdate: GitHubBuildInfo?
        get() = (status as? GitHubCheckStatus.UpdateAvailable)?.build
}

internal data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

internal data class GitHubReleaseAssetDto(
    val name: String? = null,
    @Json(name = "browser_download_url") val browserDownloadUrl: String? = null
)

internal data class GitHubWorkflowRunsResponseDto(
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRunDto> = emptyList()
)

internal data class GitHubWorkflowRunDto(
    val id: Long = 0L,
    val name: String? = null,
    @Json(name = "display_title") val displayTitle: String? = null,
    @Json(name = "run_number") val runNumber: Long = 0L,
    @Json(name = "head_sha") val headSha: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val conclusion: String? = null
)

internal fun normalizeGithubRepo(raw: String): String? {
    var trimmed = raw.trim()
    if (trimmed.isEmpty()) return null

    trimmed = trimmed
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
    if (trimmed.startsWith("github.com/", ignoreCase = true)) {
        trimmed = trimmed.substring("github.com/".length)
    }
    trimmed = trimmed.trim('/').removeSuffix(".git")
    val segments = trimmed.split('/').filter { it.isNotBlank() }
    if (segments.size < 2) return null

    val candidate = "${segments[0]}/${segments[1]}"
    val validPattern = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    return if (validPattern.matches(candidate)) candidate else null
}

internal fun parseBuildRunNumber(tagOrTitle: String): Long? {
    val buildTagMatch = Regex("""(?:build-|#)(\d+)""", RegexOption.IGNORE_CASE).find(tagOrTitle)
    if (buildTagMatch != null) {
        return buildTagMatch.groupValues[1].toLongOrNull()
    }
    val versionMinorMatch = Regex("""^v?\d+\.(\d+)(?:\.\d+)?$""", RegexOption.IGNORE_CASE).find(tagOrTitle.trim())
    return versionMinorMatch?.groupValues?.get(1)?.toLongOrNull()
}

internal fun compareVersionStrings(remote: String, local: String): Int {
    val remoteParts = Regex("""\d+""").findAll(remote).mapNotNull { it.value.toIntOrNull() }.toList()
    val localParts = Regex("""\d+""").findAll(local).mapNotNull { it.value.toIntOrNull() }.toList()
    if (remoteParts.isEmpty() || localParts.isEmpty()) return 0
    val maxLen = maxOf(remoteParts.size, localParts.size)
    for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val l = localParts.getOrElse(i) { 0 }
        if (r != l) return r.compareTo(l)
    }
    return 0
}

internal fun isRemoteBuildNewer(
    remote: GitHubBuildInfo,
    localVersionCode: Int,
    localVersionName: String,
    localRunNumber: Long,
    localCommitSha: String
): Boolean {
    val remoteRun = remote.runNumber
    if (remoteRun != null && localRunNumber > 0L) {
        if (remoteRun > localRunNumber) return true
        if (remoteRun < localRunNumber) return false
        if (!remote.commitSha.isNullOrBlank() && localCommitSha.isNotBlank()) {
            return !remote.commitSha.startsWith(localCommitSha, ignoreCase = true) &&
                !localCommitSha.startsWith(remote.commitSha, ignoreCase = true)
        }
        return false
    }

    val remoteCode = remote.versionCode
    if (remoteCode != null && remoteCode != localVersionCode) {
        return remoteCode > localVersionCode
    }

    val versionCmp = compareVersionStrings(remote.tagOrVersion, localVersionName)
    if (versionCmp != 0) {
        return versionCmp > 0
    }

    if (remoteRun != null && remoteRun > 0L && localRunNumber == 0L) {
        if (localCommitSha.isBlank()) return true
        return remote.commitSha.isNullOrBlank() ||
            (!remote.commitSha.startsWith(localCommitSha, ignoreCase = true) &&
                !localCommitSha.startsWith(remote.commitSha, ignoreCase = true))
    }

    return false
}

internal object GitHubApiParser {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val releaseAdapter = moshi.adapter(GitHubReleaseDto::class.java)
    private val workflowRunsAdapter = moshi.adapter(GitHubWorkflowRunsResponseDto::class.java)

    fun parseRelease(repo: String, json: String): GitHubBuildInfo? {
        val dto = releaseAdapter.fromJson(json) ?: return null
        if (dto.draft) return null
        val tag = dto.tagName?.takeIf { it.isNotBlank() } ?: return null
        val htmlUrl = dto.htmlUrl?.takeIf { it.isNotBlank() } ?: return null
        val apkUrl = dto.assets.firstOrNull {
            it.name?.endsWith(".apk", ignoreCase = true) == true &&
                !it.browserDownloadUrl.isNullOrBlank()
        }?.browserDownloadUrl

        val runNumber = parseBuildRunNumber(tag) ?: dto.name?.let(::parseBuildRunNumber)
        val derivedVersionCode = if (tag.startsWith("build-", ignoreCase = true) && runNumber != null) {
            (11L + runNumber).toInt()
        } else {
            null
        }
        val shaMatch = Regex("""commit\s+([0-9a-fA-F]{7,40})""").find(dto.body.orEmpty())

        return GitHubBuildInfo(
            repo = repo,
            source = GitHubBuildInfo.Source.RELEASE,
            title = dto.name?.takeIf { it.isNotBlank() } ?: tag,
            tagOrVersion = if (tag.startsWith("build-", ignoreCase = true) && runNumber != null) {
                "v11.$runNumber"
            } else {
                tag
            },
            runNumber = runNumber,
            versionCode = derivedVersionCode,
            commitSha = shaMatch?.groupValues?.get(1),
            notes = dto.body?.trim().orEmpty(),
            publishedAt = dto.publishedAt.orEmpty(),
            browserUrl = htmlUrl,
            apkDownloadUrl = apkUrl
        )
    }

    fun parseLatestSuccessfulWorkflowRun(repo: String, json: String): GitHubBuildInfo? {
        val dto = workflowRunsAdapter.fromJson(json) ?: return null
        val run = dto.workflowRuns.firstOrNull {
            it.conclusion == null || it.conclusion.equals("success", ignoreCase = true)
        } ?: return null
        val htmlUrl = run.htmlUrl?.takeIf { it.isNotBlank() } ?: return null
        return GitHubBuildInfo(
            repo = repo,
            source = GitHubBuildInfo.Source.WORKFLOW_RUN,
            title = run.displayTitle?.takeIf { it.isNotBlank() }
                ?: run.name?.takeIf { it.isNotBlank() }
                ?: "Workflow Build #${run.runNumber}",
            tagOrVersion = "Build #${run.runNumber}",
            runNumber = run.runNumber,
            versionCode = if (run.runNumber > 0L) (11L + run.runNumber).toInt() else null,
            commitSha = run.headSha,
            notes = "Built by GitHub Actions workflow run #${run.runNumber}.",
            publishedAt = run.updatedAt.orEmpty(),
            browserUrl = htmlUrl,
            apkDownloadUrl = null
        )
    }
}

object GitHubUpdateManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(GitHubUpdateUiState())
    val uiState: StateFlow<GitHubUpdateUiState> = _uiState.asStateFlow()

    private var hasAutoCheckedForRepo: String? = null

    fun currentVersionSummary(): String {
        val runPart = if (BuildConfig.CI_RUN_NUMBER > 0L) {
            "Build #${BuildConfig.CI_RUN_NUMBER}"
        } else {
            "Local Build"
        }
        return "v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE} • $runPart)"
    }

    fun autoCheckIfNeeded(repoInput: String) {
        val normalized = normalizeGithubRepo(repoInput) ?: return
        if (hasAutoCheckedForRepo == normalized) return
        hasAutoCheckedForRepo = normalized
        checkForUpdates(normalized)
    }

    fun checkForUpdates(repoInput: String) {
        val normalized = normalizeGithubRepo(repoInput)
        if (normalized == null) {
            _uiState.update {
                it.copy(
                    status = GitHubCheckStatus.Error(
                        "Enter a valid GitHub repository (owner/repo) or URL."
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                status = GitHubCheckStatus.Checking,
                lastCheckedRepo = normalized
            )
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchCheckResult(normalized)
            }
            _uiState.update {
                it.copy(status = result, lastCheckedRepo = normalized)
            }
        }
    }

    private fun fetchCheckResult(repo: String): GitHubCheckStatus {
        return try {
            val releaseBuild = fetchLatestRelease(repo)
            val latestBuild = releaseBuild ?: fetchLatestWorkflowRun(repo)
                ?: return GitHubCheckStatus.Error(
                    "No GitHub Releases or completed APK workflow runs found for $repo."
                )

            val isNewer = isRemoteBuildNewer(
                remote = latestBuild,
                localVersionCode = BuildConfig.VERSION_CODE,
                localVersionName = BuildConfig.VERSION_NAME,
                localRunNumber = BuildConfig.CI_RUN_NUMBER,
                localCommitSha = BuildConfig.CI_COMMIT_SHA
            )

            if (isNewer) {
                GitHubCheckStatus.UpdateAvailable(latestBuild)
            } else {
                GitHubCheckStatus.UpToDate(
                    repo = repo,
                    latestLabel = latestBuild.tagOrVersion
                )
            }
        } catch (e: Exception) {
            GitHubCheckStatus.Error(
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not reach GitHub. Check your network connection."
            )
        }
    }

    private fun fetchLatestRelease(repo: String): GitHubBuildInfo? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DailyBoardgames-Android/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API error (${response.code}) checking releases.")
            }
            val body = response.body?.string().orEmpty()
            return GitHubApiParser.parseRelease(repo, body)
        }
    }

    private fun fetchLatestWorkflowRun(repo: String): GitHubBuildInfo? {
        val urls = listOf(
            "https://api.github.com/repos/$repo/actions/workflows/build-apk.yml/runs?status=success&per_page=1",
            "https://api.github.com/repos/$repo/actions/runs?status=success&per_page=1"
        )
        for (url in urls) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DailyBoardgames-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@use
                if (!response.isSuccessful) {
                    throw IllegalStateException("GitHub API error (${response.code}) checking workflow runs.")
                }
                val body = response.body?.string().orEmpty()
                val parsed = GitHubApiParser.parseLatestSuccessfulWorkflowRun(repo, body)
                if (parsed != null) return parsed
            }
        }
        return null
    }

    fun openExternalUrl(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
