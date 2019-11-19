package dog.del.app.markdown.iframely

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataHolder

class IframelyExtensions : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessorFactory(IframelyNodePostProcessor.Factory())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        if (rendererBuilder.isRendererType("HTML")) {
            rendererBuilder.nodeRendererFactory(IframelyNodeRenderer.Factory())
        }
    }

    override fun parserOptions(options: MutableDataHolder?) {

    }

    override fun rendererOptions(options: MutableDataHolder?) {

    }

    companion object {
        @JvmStatic
        fun create()  = IframelyExtensions()
    }
}