package dog.del.app.markdown

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterNode
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import dog.del.app.markdown.iframely.IframelyExtensions
import dog.del.app.markdown.utils.mutableDataSetOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// TODO: cache rendered markdown
class MarkdownRenderer {
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    suspend fun render(markdown: String): MarkdownRenderResult = coroutineScope {
        val document = parser.parse(markdown)
        val renderDeferred = async { renderer.render(document) }
        val visitor = AbstractYamlFrontMatterVisitor()
        visitor.visit(document)
        val frontMatter = visitor.data
        MarkdownRenderResult(
            title = frontMatter["title"]?.get(0),
            description = frontMatter["description"]?.get(0),
            coverImage = frontMatter["coverImage"]?.get(0),
            content = renderDeferred.await()
        )
    }

    companion object {
        private val options = mutableDataSetOf(
            // TODO: Render code blocks using our highlighter service
            Parser.EXTENSIONS to listOf(
                TablesExtension.create(),
                StrikethroughSubscriptExtension.create(),
                // AnchorLinkExtension.create(),
                AutolinkExtension.create(),
                // TODO: proper styling (try to make it generate same markup as old dogbin)
                TaskListExtension.create(),
                YamlFrontMatterExtension.create(),
                IframelyExtensions.create()
            ),
            HtmlRenderer.ESCAPE_HTML_BLOCKS to true,
            HtmlRenderer.ESCAPE_INLINE_HTML to true,
            // GFM like tables
            TablesExtension.COLUMN_SPANS to false,
            TablesExtension.APPEND_MISSING_COLUMNS to true,
            TablesExtension.DISCARD_EXTRA_COLUMNS to true,
            TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH to true,
            // Anchor link generation
            HtmlRenderer.GENERATE_HEADER_ID to true
        ).toImmutable()
    }

    data class MarkdownRenderResult(
        val title: String?,
        val description: String?,
        val coverImage: String?,
        val content: String
    )
}