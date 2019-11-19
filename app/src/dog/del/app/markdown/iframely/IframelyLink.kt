package dog.del.app.markdown.iframely

import com.vladsch.flexmark.ast.InlineLinkNode
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.util.sequence.BasedSequence
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class IframelyLink(val link: Link) : InlineLinkNode(
    link.chars,
    link.textOpeningMarker,
    link.text,
    link.textClosingMarker,
    link.linkOpeningMarker,
    link.url,
    link.titleOpeningMarker,
    link.title,
    link.titleClosingMarker,
    link.linkClosingMarker
), KoinComponent {
    private val iframely by inject<Iframely>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val infoDeferred = scope.async { iframely.getEmbed(link.url.toString()) }
    val info get() = runBlocking { infoDeferred.await() }
    val shouldRenderEmbed get() = info?.html != null

    override fun setTextChars(textChars: BasedSequence) {
        textOpeningMarker = textChars.subSequence(0, 1)
        text = textChars.subSequence(2, textChars.length -1).trim()
        textClosingMarker = textChars.subSequence(textChars.length -1, textChars.length)
    }

}