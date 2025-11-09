package dev.kdriver.proxy.relay

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Handles bidirectional data relay between two sockets
 */
internal object BidirectionalRelay {

    private const val BUFFER_SIZE = 32 * 1024 // 32KB buffer like gost

    /**
     * Relay data bidirectionally between two sockets until one closes or an error occurs
     *
     * This function launches two concurrent coroutines:
     * - One copying from socket1 to socket2
     * - One copying from socket2 to socket1
     *
     * The function returns when both directions complete or when the scope is cancelled.
     *
     * @param scope Coroutine scope for launching relay jobs
     * @param socket1 First socket (typically client)
     * @param socket2 Second socket (typically target/upstream)
     * @param onBytesTransferred Optional callback invoked with (fromSocket1, fromSocket2) byte counts
     */
    suspend fun relay(
        scope: CoroutineScope,
        socket1: Socket,
        socket2: Socket,
        onBytesTransferred: ((Long, Long) -> Unit)? = null,
    ) {
        val readChannel1 = socket1.openReadChannel()
        val writeChannel1 = socket1.openWriteChannel(autoFlush = false)
        val readChannel2 = socket2.openReadChannel()
        val writeChannel2 = socket2.openWriteChannel(autoFlush = false)

        var bytesFromSocket1 = 0L
        var bytesFromSocket2 = 0L

        try {
            // Launch two concurrent relay jobs
            coroutineScope {
                // Socket1 -> Socket2
                val job1 = launch {
                    try {
                        bytesFromSocket1 = copyData(readChannel1, writeChannel2)
                    } catch (e: Exception) {
                        // Expected when connection closes
                    } finally {
                        // Close write side to signal EOF
                        writeChannel2.flushAndClose()
                    }
                }

                // Socket2 -> Socket1
                val job2 = launch {
                    try {
                        bytesFromSocket2 = copyData(readChannel2, writeChannel1)
                    } catch (e: Exception) {
                        // Expected when connection closes
                    } finally {
                        // Close write side to signal EOF
                        writeChannel1.flushAndClose()
                    }
                }

                // Wait for both directions to complete
                job1.join()
                job2.join()
            }
        } finally {
            // Notify caller of bytes transferred
            onBytesTransferred?.invoke(bytesFromSocket1, bytesFromSocket2)

            // Ensure both sockets are closed
            try {
                socket1.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
            try {
                socket2.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    /**
     * Copy data from one channel to another
     * Returns the total number of bytes copied
     */
    private suspend fun copyData(
        readChannel: ByteReadChannel,
        writeChannel: ByteWriteChannel,
    ): Long {
        var totalBytes = 0L
        val buffer = ByteArray(BUFFER_SIZE)

        try {
            while (!readChannel.isClosedForRead) {
                val bytesRead = readChannel.readAvailable(buffer, 0, buffer.size)
                if (bytesRead == -1) {
                    // EOF reached
                    break
                }
                if (bytesRead > 0) {
                    writeChannel.writeFully(buffer, 0, bytesRead)
                    writeChannel.flush()
                    totalBytes += bytesRead
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Coroutine was cancelled, propagate
            throw e
        } catch (e: Exception) {
            // Connection error (closed by peer, timeout, etc.)
            // This is expected during normal connection closure
        }

        return totalBytes
    }

    /**
     * Relay data with a custom scope and exception handler
     *
     * This is useful when you want to handle exceptions differently or have custom cleanup logic.
     */
    suspend fun relayWithHandler(
        scope: CoroutineScope,
        socket1: Socket,
        socket2: Socket,
        onError: ((Throwable) -> Unit)? = null,
        onComplete: ((Long, Long) -> Unit)? = null,
    ) {
        try {
            relay(scope, socket1, socket2, onComplete)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Normal cancellation, don't report as error
            throw e
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }

}
