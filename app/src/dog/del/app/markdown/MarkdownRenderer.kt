package dog.del.app.markdown

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import dog.del.app.markdown.iframely.IframelyExtensions
import dog.del.app.markdown.utils.mutableDataSetOf

// TODO: cache rendered markdown
class MarkdownRenderer {
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    fun render(markdown: String): String {
        val document = parser.parse(markdown)
        // TODO: extract front matter
        return renderer.render(document)
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
}