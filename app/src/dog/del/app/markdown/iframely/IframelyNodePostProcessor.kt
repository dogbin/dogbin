package dog.del.app.markdown.iframely

import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker

class IframelyNodePostProcessor : NodePostProcessor() {

    override fun process(state: NodeTracker, node: Node) {
        if (node is Link) {
            if (node.text.toString() == "!embed") {
                val newNode = IframelyLink(node)

                newNode.takeChildren(node)

                node.insertBefore(newNode)
                state.nodeAddedWithChildren(newNode)

                node.unlink()
                state.nodeRemoved(node)
            }
        }
    }

    class Factory : NodePostProcessorFactory(false) {
        init {
            addNodes(Link::class.java)
        }

        override fun apply(document: Document): NodePostProcessor = IframelyNodePostProcessor()
    }
}