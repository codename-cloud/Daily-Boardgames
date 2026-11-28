package com.example.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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

data class ChangelogEntry(
    val text: String,
    val indentLevel: Int = 0
)

data class VersionChangelog(
    val versionLabel: String,
    val title: String,
    val entries: List<ChangelogEntry>
)

data class GitHubBuildInfo(
    val repo: String,
    val source: Source,
    val title: String,
    val tagOrVersion: String,
    val runNumber: Long?,
    val versionCode: Int?,
    val commitSha: String?,
    val notes: String,
    val changelogItems: List<String> = emptyList(),
    val changelogEntries: List<ChangelogEntry> = changelogItems.map { ChangelogEntry(it) },
    val releaseChangelogs: List<VersionChangelog> = emptyList(),
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
        val latestLabel: String,
        val changelogItems: List<String> = emptyList(),
        val changelogEntries: List<ChangelogEntry> = changelogItems.map { ChangelogEntry(it) }
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

internal data class GitHubHeadCommitDto(
    val id: String? = null,
    val message: String? = null
)

internal data class GitHubWorkflowRunDto(
    val id: Long = 0L,
    val name: String? = null,
    @Json(name = "display_title") val displayTitle: String? = null,
    @Json(name = "run_number") val runNumber: Long = 0L,
    @Json(name = "head_sha") val headSha: String? = null,
    @Json(name = "head_commit") val headCommit: GitHubHeadCommitDto? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val conclusion: String? = null
)

internal fun formatStructuredChangelog(
    rawText: String?,
    isCommitMessage: Boolean = false
): List<ChangelogEntry> {
    if (rawText.isNullOrBlank()) return emptyList()
    val entries = mutableListOf<ChangelogEntry>()
    val seen = mutableSetOf<String>()
    val conventionalRegex = Regex(
        """^(feat|feature|fix|bugfix|perf|refactor|style|ui)(?:\([^)]+\))?:\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    val otherPrefixRegex = Regex("""^([a-zA-Z]+)(?:\([^)]+\))?:\s+(.+)$""")
    val bulletRegex = Regex("""^\s*[-*•]\s+(.+)$""")

    fun formatLineText(content: String): Pair<String, Boolean>? {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("#") || trimmed == "---" || trimmed == "***") return null

        val lower = trimmed.lowercase()
        if (lower.startsWith("co-authored-by:") ||
            lower.startsWith("signed-off-by:") ||
            lower.startsWith("change-id:") ||
            lower.startsWith("merge pull request ") ||
            lower.startsWith("merge branch ") ||
            lower.startsWith("automated apk build from workflow run") ||
            Regex("""^build #\d+\s*[•·-]\s*commit\s+[0-9a-f]+$""").matches(lower)
        ) {
            return null
        }

        val convMatch = conventionalRegex.matchEntire(trimmed)
        if (convMatch != null) {
            val kind = convMatch.groupValues[1].lowercase()
            val rest = convMatch.groupValues[2].trim().replaceFirstChar { it.uppercaseChar() }
            val prefix = when (kind) {
                "feat", "feature" -> "New: "
                "fix", "bugfix" -> "Fixed: "
                "perf", "refactor", "style", "ui" -> "Improved: "
                else -> ""
            }
            return "$prefix$rest" to true
        }

        val otherMatch = otherPrefixRegex.matchEntire(trimmed)
        if (otherMatch != null) {
            val rest = otherMatch.groupValues[2].trim().replaceFirstChar { it.uppercaseChar() }
            return rest to false
        }

        return trimmed.replaceFirstChar { it.uppercaseChar() } to false
    }

    var seenCommitSubject = false
    var openBodyParagraph = false

    for (rawLine in rawText.lines()) {
        val leadingWhitespaceCount = rawLine.takeWhile { it == ' ' || it == '\t' }
            .sumOf { if (it == '\t') 4 else 1 }
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed == "---" || trimmed == "***") {
            openBodyParagraph = false
            continue
        }

        val bulletMatch = bulletRegex.matchEntire(rawLine)
        val isBullet = bulletMatch != null
        val content = if (bulletMatch != null) bulletMatch.groupValues[1].trim() else trimmed

        val (formatted, isConventional) = formatLineText(content) ?: run {
            openBodyParagraph = false
            continue
        }

        if (isCommitMessage) {
            if (!seenCommitSubject) {
                if (seen.add(formatted.lowercase())) {
                    entries.add(ChangelogEntry(text = formatted, indentLevel = 0))
                    seenCommitSubject = true
                }
                openBodyParagraph = false
            } else if (isConventional) {
                if (seen.add(formatted.lowercase())) {
                    entries.add(ChangelogEntry(text = formatted, indentLevel = 0))
                }
                openBodyParagraph = false
            } else if (!isBullet && openBodyParagraph && entries.isNotEmpty() && entries.last().indentLevel > 0) {
                val prev = entries.removeAt(entries.lastIndex)
                val merged = "${prev.text} $content"
                seen.remove(prev.text.lowercase())
                seen.add(merged.lowercase())
                entries.add(prev.copy(text = merged))
            } else {
                if (seen.add(formatted.lowercase())) {
                    entries.add(ChangelogEntry(text = formatted, indentLevel = 1))
                    openBodyParagraph = !isBullet
                }
            }
        } else {
            val indentLevel = if (leadingWhitespaceCount >= 2 && entries.isNotEmpty()) 1 else 0
            if (seen.add(formatted.lowercase())) {
                entries.add(ChangelogEntry(text = formatted, indentLevel = indentLevel))
            }
        }

        if (entries.size >= 16) break
    }
    return entries
}

internal fun formatFriendlyChangelog(rawText: String?): List<String> =
    formatStructuredChangelog(rawText, isCommitMessage = false).map { it.text }

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

internal fun isVersionStrictlyNewer(
    remote: GitHubBuildInfo,
    localVersionCode: Int,
    localVersionName: String,
    localRunNumber: Long
): Boolean {
    val remoteRun = remote.runNumber
    if (remoteRun != null && localRunNumber > 0L) {
        return remoteRun > localRunNumber
    }

    val remoteCode = remote.versionCode
    if (remoteCode != null && remoteCode != localVersionCode) {
        return remoteCode > localVersionCode
    }

    return compareVersionStrings(remote.tagOrVersion, localVersionName) > 0
}

internal fun buildPendingUpdateInfo(
    builds: List<GitHubBuildInfo>,
    localVersionCode: Int,
    localVersionName: String,
    localRunNumber: Long,
    localCommitSha: String
): GitHubBuildInfo? {
    val latest = builds.firstOrNull() ?: return null
    if (!isRemoteBuildNewer(latest, localVersionCode, localVersionName, localRunNumber, localCommitSha)) {
        return null
    }

    val newerBuilds = builds.filter {
        isVersionStrictlyNewer(
            remote = it,
            localVersionCode = localVersionCode,
            localVersionName = localVersionName,
            localRunNumber = localRunNumber
        )
    }.ifEmpty { listOf(latest) }

    val combinedChangelogs = newerBuilds.mapNotNull { build ->
        val entries = build.changelogEntries.ifEmpty {
            build.notes.takeIf { it.isNotBlank() }?.let { listOf(ChangelogEntry(it)) }.orEmpty()
        }
        if (entries.isEmpty()) {
            null
        } else {
            VersionChangelog(
                versionLabel = build.tagOrVersion,
                title = build.title,
                entries = entries
            )
        }
    }

    return latest.copy(releaseChangelogs = combinedChangelogs)
}

internal object GitHubApiParser {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val releaseAdapter = moshi.adapter(GitHubReleaseDto::class.java)
    private val releaseListAdapter = moshi.adapter<List<GitHubReleaseDto>>(
        Types.newParameterizedType(List::class.java, GitHubReleaseDto::class.java)
    )
    private val workflowRunsAdapter = moshi.adapter(GitHubWorkflowRunsResponseDto::class.java)

    private fun toBuildInfo(repo: String, dto: GitHubReleaseDto): GitHubBuildInfo? {
        if (dto.draft) return null
        val tag = dto.tagName?.takeIf { it.isNotBlank() } ?: return null
        val htmlUrl = dto.htmlUrl?.takeIf { it.isNotBlank() } ?: return null
        val apkUrl = dto.assets.firstOrNull {
            it.name?.endsWith(".apk", ignoreCase = true) == true &&
                !it.browserDownloadUrl.isNullOrBlank()
        }?.browserDownloadUrl

        val runNumber = parseBuildRunNumber(tag) ?: dto.name?.let(::parseBuildRunNumber)
        val derivedVersionCode = if (tag.startsWith("build-", ignoreCase = true) && runNumber != null) {
            (1L + runNumber).toInt()
        } else {
            null
        }
        val shaMatch = Regex("""commit\s+([0-9a-fA-F]{7,40})""").find(dto.body.orEmpty())

        val bodyText = dto.body?.trim().orEmpty()
        val changelogEntries = formatStructuredChangelog(bodyText, isCommitMessage = false)
        val tagOrVersion = if (tag.startsWith("build-", ignoreCase = true) && runNumber != null) {
            "v1.$runNumber"
        } else {
            tag
        }
        val title = dto.name?.takeIf { it.isNotBlank() } ?: tag

        return GitHubBuildInfo(
            repo = repo,
            source = GitHubBuildInfo.Source.RELEASE,
            title = title,
            tagOrVersion = tagOrVersion,
            runNumber = runNumber,
            versionCode = derivedVersionCode,
            commitSha = shaMatch?.groupValues?.get(1),
            notes = bodyText,
            changelogItems = changelogEntries.map { it.text },
            changelogEntries = changelogEntries,
            releaseChangelogs = if (changelogEntries.isNotEmpty()) {
                listOf(VersionChangelog(versionLabel = tagOrVersion, title = title, entries = changelogEntries))
            } else {
                emptyList()
            },
            publishedAt = dto.publishedAt.orEmpty(),
            browserUrl = htmlUrl,
            apkDownloadUrl = apkUrl
        )
    }

    fun parseRelease(repo: String, json: String): GitHubBuildInfo? {
        val dto = releaseAdapter.fromJson(json) ?: return null
        return toBuildInfo(repo, dto)
    }

    fun parseReleases(repo: String, json: String): List<GitHubBuildInfo> {
        val dtos = releaseListAdapter.fromJson(json).orEmpty().filter { !it.draft }
        if (dtos.isEmpty()) return emptyList()
        val preferredDtos = dtos.filter { !it.prerelease }.ifEmpty { dtos }
        return preferredDtos
            .mapNotNull { toBuildInfo(repo, it) }
            .sortedWith(
                compareByDescending<GitHubBuildInfo> { it.runNumber ?: -1L }
                    .thenByDescending { it.versionCode ?: -1 }
                    .thenByDescending { it.publishedAt }
            )
    }

    fun parseLatestSuccessfulWorkflowRun(repo: String, json: String): GitHubBuildInfo? =
        parseSuccessfulWorkflowRuns(repo, json).firstOrNull()

    fun parseSuccessfulWorkflowRuns(repo: String, json: String): List<GitHubBuildInfo> {
        val dto = workflowRunsAdapter.fromJson(json) ?: return emptyList()
        val seenRuns = mutableSetOf<Long>()
        return dto.workflowRuns
            .filter { it.conclusion == null || it.conclusion.equals("success", ignoreCase = true) }
            .mapNotNull { run ->
                val htmlUrl = run.htmlUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (!seenRuns.add(run.runNumber)) return@mapNotNull null
                val commitMessage = run.headCommit?.message?.takeIf { it.isNotBlank() }
                    ?: run.displayTitle?.takeIf { it.isNotBlank() }
                    ?: ""
                val changelogEntries = formatStructuredChangelog(commitMessage, isCommitMessage = true)
                val tagOrVersion = "Build #${run.runNumber}"
                val title = run.displayTitle?.takeIf { it.isNotBlank() }
                    ?: run.name?.takeIf { it.isNotBlank() }
                    ?: "Workflow Build #${run.runNumber}"
                GitHubBuildInfo(
                    repo = repo,
                    source = GitHubBuildInfo.Source.WORKFLOW_RUN,
                    title = title,
                    tagOrVersion = tagOrVersion,
                    runNumber = run.runNumber,
                    versionCode = if (run.runNumber > 0L) (1L + run.runNumber).toInt() else null,
                    commitSha = run.headSha,
                    notes = commitMessage.ifBlank { "Built by GitHub Actions workflow run #${run.runNumber}." },
                    changelogItems = changelogEntries.map { it.text },
                    changelogEntries = changelogEntries,
                    releaseChangelogs = if (changelogEntries.isNotEmpty()) {
                        listOf(VersionChangelog(versionLabel = tagOrVersion, title = title, entries = changelogEntries))
                    } else {
                        emptyList()
                    },
                    publishedAt = run.updatedAt.orEmpty(),
                    browserUrl = htmlUrl,
                    apkDownloadUrl = null
                )
            }
            .sortedByDescending { it.runNumber ?: -1L }
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
            val builds = fetchReleases(repo).ifEmpty { fetchWorkflowRuns(repo) }
            val latestBuild = builds.firstOrNull()
                ?: return GitHubCheckStatus.Error(
                    "No GitHub Releases or completed APK workflow runs found for $repo."
                )

            val pendingUpdate = buildPendingUpdateInfo(
                builds = builds,
                localVersionCode = BuildConfig.VERSION_CODE,
                localVersionName = BuildConfig.VERSION_NAME,
                localRunNumber = BuildConfig.CI_RUN_NUMBER,
                localCommitSha = BuildConfig.CI_COMMIT_SHA
            )

            if (pendingUpdate != null) {
                GitHubCheckStatus.UpdateAvailable(pendingUpdate)
            } else {
                GitHubCheckStatus.UpToDate(
                    repo = repo,
                    latestLabel = latestBuild.tagOrVersion,
                    changelogItems = latestBuild.changelogItems,
                    changelogEntries = latestBuild.changelogEntries
                )
            }
        } catch (e: Exception) {
            GitHubCheckStatus.Error(
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not reach GitHub. Check your network connection."
            )
        }
    }

    private fun fetchReleases(repo: String): List<GitHubBuildInfo> {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases?per_page=100")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DailyBoardgames-Android/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return emptyList()
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API error (${response.code}) checking releases.")
            }
            val body = response.body?.string().orEmpty()
            return GitHubApiParser.parseReleases(repo, body)
        }
    }

    private fun fetchWorkflowRuns(repo: String): List<GitHubBuildInfo> {
        val urls = listOf(
            "https://api.github.com/repos/$repo/actions/workflows/build-apk.yml/runs?status=success&per_page=30",
            "https://api.github.com/repos/$repo/actions/runs?status=success&per_page=30"
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
                val parsed = GitHubApiParser.parseSuccessfulWorkflowRuns(repo, body)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return emptyList()
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
