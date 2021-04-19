import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object IssueParser {
    @JvmStatic
    fun main(args: Array<String>? = null) {
        val allItems = fetchAllIssues()
        writeFormattedIssuesOnDisk(allItems)
    }

    private fun parseJson(jsonValue: String): List<Issue> {
        val type = object : TypeToken<List<Issue>>() {}.type
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .create()
            .fromJson(jsonValue, type)
    }

    private fun writeFormattedIssuesOnDisk(issueList: List<Issue>) {
        issueList.forEach { issue ->
            val htmlContent = buildString {
                appendLine("---")
                appendLine("layout: post")
                appendLine("title: ${issue.title}")
                if (issue.labels!!.isNotEmpty()) appendLine(
                    "tags: ${
                        issue.labels.joinToString(
                            prefix = "[",
                            postfix = "]",
                            separator = ", "
                        ) { it.name!! }
                    }"
                )
                appendLine("---")
                appendLine()
                appendLine()
                appendLine(issue.body)
                issue.commentsList?.takeIf { it.isNotEmpty() }?.forEach { comment ->
                    appendLine("<!-- comment #${comment.id} -->")
                    appendLine(comment.body)
                }
            }

            val date = SimpleDateFormat("yyyy-MM-dd").format(issue.createdAt)
            File("_posts/${date}-${issue.number}.html")
                .writeText(htmlContent)
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(Interceptor {
            val newRequest = it.request().newBuilder()
                .addHeader("Authorization", "")
                .build()
            it.proceed(newRequest)
        })
        .addNetworkInterceptor(Interceptor {
            it.proceed(it.request()).also { response ->
                if (response.isSuccessful)
                    println("Successful ${response.request.url}")
                else
                    println("Error: ${response.code} ${response.message}")
            }
        })
        .build()

    private fun fetchAllIssues(): List<Issue> {
        val sumResponses = mutableListOf<List<Issue>>()
        do {
            val nextPage = sumResponses.size + 1
            val request = Request.Builder()
                .get()
                .url("https://api.github.com/repos/irlogcat/android-faq/issues?per_page=100&page=$nextPage")
                .build()
            val response = httpClient.newCall(request).execute()
            when {
                response.isSuccessful -> {
                    val pageItems = parseJson(response.body!!.string())
                        .map { issue ->
                            if (issue.comments ?: 0 > 0)
                                issue.copy(commentsList = fetchComments(issue.number!!))
                            else
                                issue
                        }
                    sumResponses.add(pageItems)
                }
                else ->
                    throw RuntimeException(response.message)
            }
        } while (sumResponses.lastOrNull()?.isNotEmpty() == true)

        return sumResponses.flatten()
    }

    private fun fetchComments(issueNumber: Int): List<Issue> {
        val request = Request.Builder()
            .get()
            .url("https://api.github.com/repos/irlogcat/Android-FAQ/issues/$issueNumber/comments")
            .build()
        val response = httpClient.newCall(request).execute()
        when {
            response.isSuccessful ->
                return parseJson(response.body!!.string())
            else ->
                throw RuntimeException(response.message)
        }
    }
}

data class Issue(
    @field:SerializedName("assignees")
    val assignees: List<Any>?,
    @field:SerializedName("created_at")
    val createdAt: Date?,
    @field:SerializedName("title")
    val title: String?,
    @field:SerializedName("body")
    val body: String?,
    @field:SerializedName("labels_url")
    val labelsUrl: String?,
    @field:SerializedName("author_association")
    val authorAssociation: String?,
    @field:SerializedName("number")
    val number: Int?,
    @field:SerializedName("updated_at")
    val updatedAt: String?,
    @field:SerializedName("performed_via_github_app")
    val performedViaGithubApp: Any?,
    @field:SerializedName("comments_url")
    val commentsUrl: String?,
    @field:SerializedName("active_lock_reason")
    val activeLockReason: Any?,
    @field:SerializedName("repository_url")
    val repositoryUrl: String?,
    @field:SerializedName("id")
    val id: Int?,
    @field:SerializedName("state")
    val state: String?,
    @field:SerializedName("locked")
    val locked: Boolean?,
    @field:Transient
    val commentsList: List<Issue>? = null,
    @field:SerializedName("comments")
    val comments: Int?,
    @field:SerializedName("closed_at")
    val closedAt: Any?,
    @field:SerializedName("url")
    val url: String?,
    @field:SerializedName("labels")
    val labels: List<Label>?,
    @field:SerializedName("milestone")
    val milestone: Any?,
    @field:SerializedName("events_url")
    val eventsUrl: String?,
    @field:SerializedName("html_url")
    val htmlUrl: String?,
    @field:SerializedName("assignee")
    val assignee: Any?,
    @field:SerializedName("user")
    val user: User?,
    @field:SerializedName("node_id")
    val nodeId: String?,
    @field:SerializedName("pull_request")
    val pullRequest: PullRequest?
)

data class Label(
    @field:SerializedName("id")
    val id: Long?,
    @field:SerializedName("node_id")
    val nodeId: String?,
    @field:SerializedName("url")
    val url: String?,
    @field:SerializedName("name")
    val name: String?,
    @field:SerializedName("color")
    val color: String?,
    @field:SerializedName("default")
    val isDefault: Boolean?,
    @field:SerializedName("description")
    val description: String?,
)

data class User(

    @field:SerializedName("gists_url")
    val gistsUrl: String?,
    @field:SerializedName("repos_url")
    val reposUrl: String?,
    @field:SerializedName("following_url")
    val followingUrl: String?,
    @field:SerializedName("starred_url")
    val starredUrl: String?,
    @field:SerializedName("login")
    val login: String?,
    @field:SerializedName("followers_url")
    val followersUrl: String?,
    @field:SerializedName("type")
    val type: String?,
    @field:SerializedName("url")
    val url: String?,
    @field:SerializedName("subscriptions_url")
    val subscriptionsUrl: String?,
    @field:SerializedName("received_events_url")
    val receivedEventsUrl: String?,
    @field:SerializedName("avatar_url")
    val avatarUrl: String?,
    @field:SerializedName("events_url")
    val eventsUrl: String?,
    @field:SerializedName("html_url")
    val htmlUrl: String?,
    @field:SerializedName("site_admin")
    val siteAdmin: Boolean?,
    @field:SerializedName("id")
    val id: Int?,
    @field:SerializedName("gravatar_id")
    val gravatarId: String?,
    @field:SerializedName("node_id")
    val nodeId: String?,
    @field:SerializedName("organizations_url")
    val organizationsUrl: String
)

data class PullRequest(
    @field:SerializedName("patch_url")
    val patchUrl: String?,
    @field:SerializedName("html_url")
    val htmlUrl: String?,
    @field:SerializedName("diff_url")
    val diffUrl: String?,
    @field:SerializedName("url")
    val url: String?
)