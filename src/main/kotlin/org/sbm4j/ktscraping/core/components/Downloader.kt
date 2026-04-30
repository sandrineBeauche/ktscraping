package org.sbm4j.ktscraping.core.components

import org.sbm4j.ktscraping.core.processors.RequestReceiver
import org.sbm4j.meercat.nodes.logger

/**
 * Defines the type of content carried by a [DownloadingResponse].
 *
 * Used by downloaders to indicate the nature of the downloaded content, and by
 * consumers to determine how to handle it (via [DownloadingResponse.isText] and
 * [DownloadingResponse.isByteArray]).
 *
 * @see DownloadingResponse
 * @see AbstractDownloader
 */
enum class ContentType{
    /** HTML page content. */
    HTML,
    /** XML document content. */
    XML,
    /** JSON document content. */
    JSON,
    /** SVG image, handled as text. */
    SVG_IMAGE,
    /** Bitmap image, handled as a [ByteArray]. */
    BITMAP_IMAGE,
    /** Generic image, handled as a [ByteArray]. */
    IMAGE,
    /** Generic file, handled as a [ByteArray]. */
    FILE,
    /** Plain string content. */
    STRING,
    /** No content available (used for error responses or uninitialized backs). */
    NOTHING
}


/**
 * Base abstract class for Downloader branch sink nodes.
 *
 * An [AbstractDownloader] is the terminal node of the Downloader branch. It receives
 * [AbstractRequest] messages from the Spider via [RequestReceiver], performs the actual
 * download, and returns a [DownloadingResponse] populated with the retrieved content.
 *
 * Three standard keys are defined as companions for use in [AbstractRequest.parameters]
 * and [DownloadingResponse.contents]:
 * - [PAYLOAD]: the raw downloaded content.
 * - [FRAMES]: browser frames, used by Playwright-based downloaders.
 * - [CONTENT_TYPE]: the expected [ContentType] of the response, set by the requester.
 *
 * @param name The name of this downloader node.
 *
 * @see RequestReceiver
 * @see AbstractSinkComponent
 * @see DownloadingResponse
 * @see ContentType
 */
abstract class AbstractDownloader(
    name: String
): AbstractSinkComponent(name), RequestReceiver {

    companion object{
        /** Key in [AbstractRequest.parameters] or [DownloadingResponse.contents] for the raw downloaded content. */
        val PAYLOAD: String = "payload"
        /** Key in [AbstractRequest.parameters] or [DownloadingResponse.contents] for browser frames (Playwright). */
        val FRAMES: String = "frames"
        /**
         * Key in [AbstractRequest.parameters] indicating the expected [ContentType] of the response.
         * Set by the requester (e.g. [GoogleSearchImageRequest]) to guide the downloader.
         */
        val CONTENT_TYPE: String = "contentType"
    }

    override suspend fun run() {
        logger.info{"${name}: Starting downloader"}
        super<RequestReceiver>.run()
        super<AbstractSinkComponent>.run()
    }

    override suspend fun stop() {
        logger.info{"${name}: Stopping downloader"}
        super<AbstractSinkComponent>.stop()
    }
}