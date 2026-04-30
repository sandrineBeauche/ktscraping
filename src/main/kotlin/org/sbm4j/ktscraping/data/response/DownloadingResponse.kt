package org.sbm4j.ktscraping.data.response

import org.sbm4j.ktscraping.core.components.ContentType
import org.sbm4j.meercat.data.Status
import org.sbm4j.meercat.data.ErrorInfo
import org.sbm4j.ktscraping.data.request.DownloadingRequest

/**
 * Back message returned by the Downloader branch in response to a [DownloadingRequest].
 *
 * Carries the downloaded content in [contents], a map allowing multiple content items
 * to be returned in a single response (e.g. several images extracted from a page,
 * multiple JSON fragments, etc.). The [type] property describes the nature of the
 * content, and utility methods [isText] and [isByteArray] allow consumers to determine
 * how to handle it without inspecting [type] directly.
 *
 * @property send The original [DownloadingRequest] this response is answering.
 * @property type The type of content held in [contents], defaults to [ContentType.HTML].
 * @property status Overall processing status, defaults to [Status.OK].
 * @property errorInfos List of errors accumulated during the download.
 * @property name Technical name of this response, defaults to `"<request name>-DownloadingResponse"`.
 *
 * @see DownloadingRequest
 * @see ContentType
 */
data class DownloadingResponse(
    override val send: DownloadingRequest,
    var type: ContentType = ContentType.HTML,
    override var status: Status = Status.OK,
    override val errorInfos: MutableList<ErrorInfo> = mutableListOf(),
    override val name: String = "${send.name}-DownloadingResponse"
): Response(send, status, errorInfos) {

    /**
     * Map holding the downloaded content items.
     *
     * A single response may contain multiple entries (e.g. several images extracted
     * from a page, or multiple JSON fragments). The keys and value types depend on
     * the [type] and the Downloader implementation.
     */
    val contents: MutableMap<String, Any> = mutableMapOf()

    /**
     * Returns `true` if the content is text-based and can be handled as a [String].
     *
     * Text-based content types: [ContentType.XML], [ContentType.JSON],
     * [ContentType.SVG_IMAGE], [ContentType.HTML], [ContentType.STRING].
     */
    fun isText(): Boolean{
        return when(type){
            ContentType.XML,
            ContentType.JSON,
            ContentType.SVG_IMAGE,
            ContentType.HTML,
            ContentType.STRING -> true

            ContentType.FILE,
            ContentType.IMAGE,
            ContentType.BITMAP_IMAGE,
            ContentType.NOTHING -> false
        }
    }

    /**
     * Returns `true` if the content is binary and should be handled as a [ByteArray].
     *
     * Binary content types: [ContentType.FILE], [ContentType.IMAGE], [ContentType.BITMAP_IMAGE].
     */
    fun isByteArray(): Boolean{
        return when(type){
            ContentType.XML,
            ContentType.JSON,
            ContentType.SVG_IMAGE,
            ContentType.HTML,
            ContentType.NOTHING,
            ContentType.STRING -> false

            ContentType.FILE,
            ContentType.IMAGE,
            ContentType.BITMAP_IMAGE -> true
        }
    }

    /** Creates a copy of this response via [copy]. */
    override fun clone(): DownloadingResponse {
        return this.copy()
    }
}




