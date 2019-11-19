package dog.del.app.markdown.iframely

import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.util.data.DataHolder

// TODO: render reader mode and other iframely renders as well
class IframelyNodeRenderer : NodeRenderer {
    override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> = mutableSetOf(
        // TODO: move link resolving here using context.resolveLink
        NodeRenderingHandler(IframelyLink::class.java) { node: IframelyLink, context: NodeRendererContext, html: HtmlWriter ->
            if (context.isDoNotRenderLinks) {
                context.renderChildren(node)
            } else if (!node.shouldRenderEmbed) {
                // TODO: set iframely resolved title as link text
                node.link.takeChildren(node)
                context.render(node.link)
            } else {
                html.attr("class", "md-embed")
                    .withAttr()
                    .tag("div")
                html.append(node.info!!.html!!)
                html.closeTag("div")
            }
        }
    )
    class Factory : NodeRendererFactory {
        override fun apply(options: DataHolder): NodeRenderer = IframelyNodeRenderer()
    }

}