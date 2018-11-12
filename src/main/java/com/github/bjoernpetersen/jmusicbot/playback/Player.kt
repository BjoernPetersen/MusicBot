package com.github.bjoernpetersen.jmusicbot.playback

import com.github.bjoernpetersen.jmusicbot.Song
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackStateListener.PlaybackState
import com.github.bjoernpetersen.jmusicbot.provider.BrokenSuggesterException
import com.github.bjoernpetersen.jmusicbot.provider.Suggester
import com.google.common.util.concurrent.ThreadFactoryBuilder
import mu.KotlinLogging
import java.io.Closeable
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

class Player(private val songPlayedNotifier: Consumer<SongEntry>, private val suggester: Suggester?) :
    Closeable {

  private val logger = KotlinLogging.logger {}
  private val autoPlayer: ExecutorService = Executors.newSingleThreadExecutor(
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("playerPool-%d")
          .build()
  )

  /**
   * This player's queue.
   */
  val queue: Queue = Queue()

  private val stateLock: Lock = ReentrantLock()
  /**
   * The current state of this player. This might be play, pause, stop or error.
   */
  var state: PlayerState = StopState()
    private set(value) {
      field = value
      for (listener in stateListeners) {
        listener.onChanged(value)
      }
    }
  private var playback: Playback = DummyPlayback

  private val stateListeners: MutableSet<PlayerStateListener> = HashSet()

  init {
    autoPlayer.submit { this.autoPlay() }
    queue.addListener(object : QueueChangeListener {
      override fun onAdd(entry: QueueEntry) {
        entry.song.load()
      }

      override fun onRemove(entry: QueueEntry) {}

      override fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int) {}
    })

    if (suggester != null) {
      addListener(PlayerStateListener { _ -> preloadSuggestion(suggester) })
    }
  }

  private fun preloadSuggestion(suggester: Suggester) {
    if (queue.isEmpty) {
      val suggestions: List<Song>
      try {
        suggestions = suggester.getNextSuggestions(1)
      } catch (e: BrokenSuggesterException) {
        return
      }

      suggestions[0].load()
    }
  }

  /**
   * Adds a [PlayerStateListener] which will be called everytime the [state] changes.
   */
  fun addListener(listener: PlayerStateListener) {
    stateListeners.add(listener)
  }

  /**
   * Removes a [PlayerStateListener] previously registered with [addListener].
   */
  fun removeListener(listener: PlayerStateListener) {
    stateListeners.remove(listener)
  }

  /**
   * Pauses the player.
   *
   * If the player is not currently playing anything, nothing will be done.
   *
   * This method blocks until the playback is paused.
   */
  @Throws(InterruptedException::class)
  fun pause() {
    stateLock.withLock {
      logger.debug("Pausing...")
      val state = state
      if (state is PauseState) {
        logger.trace("Already paused.")
        return
      } else if (state !is PlayState) {
        logger.info { "Tried to pause player in state $state" }
        return
      }
      playback.pause()
      this.state = state.pause()
    }
  }

  /**
   * Resumes the playback.
   *
   * If the player is not currently pausing, nothing will be done.
   *
   * This method blocks until the playback is resumed.
   */
  @Throws(InterruptedException::class)
  fun play() {
    stateLock.withLock {
      logger.debug("Playing...")
      val state = state
      if (state is PlayState) {
        logger.trace("Already playing.")
        return
      } else if (state !is PauseState) {
        logger.info { "Tried to play in state $state" }
        return
      }
      playback.play()
      this.state = state.play()
    }
  }

  /**
   * Plays the next song.
   *
   * This method will play the next song from the queue.
   * If the queue is empty, the next suggested song from the primary [suggester] will be used.
   * If there is no primary suggester, the player will transition into the [StopState].
   *
   * This method blocks until either a new song is playing or the StopState is reached.
   */
  @Throws(InterruptedException::class)
  fun next() {
    val state = this.state
    stateLock.withLock {
      logger.debug("Next...")
      val newState = this.state
      if (isSignificantlyDifferent(state, newState)) {
        logger.debug("Skipping next call due to state change while waiting for lock.")
        return
      }

      try {
        playback.close()
      } catch (e: Exception) {
        logger.warn(e) { "Error closing playback" }
      }

      val nextOptional = queue.pop()
      if (!nextOptional.isPresent && suggester == null) {
        logger.info("Queue is empty. Stopping.")
        playback = DummyPlayback
        this.state = StopState()
        return
      }

      val nextEntry: SongEntry = if (nextOptional.isPresent) {
        nextOptional.get()
      } else try {
        SuggestedSongEntry(suggester!!.suggestNext())
      } catch (e: BrokenSuggesterException) {
        logger.warn("Default suggester could not suggest anything. Stopping.")
        playback = DummyPlayback
        this.state = StopState()
        return
      }

      val nextSong = nextEntry.song
      songPlayedNotifier.accept(nextEntry)
      logger.debug("Next song is: $nextSong")
      try {
        playback = nextSong.playback
      } catch (e: IOException) {
        logger.warn(e) { "Error creating playback" }

        this.state = ErrorState()
        playback = DummyPlayback
        return
      }

      playback.setPlaybackStateListener { this.onPlaybackFeedback(it) }

      this.state = PauseState(nextEntry)
      play()
    }
  }

  private fun onPlaybackFeedback(feedback: PlaybackState) {
    logger.debug { "Playback state update: $feedback" }
    stateLock.withLock {
      val state = state
      when (feedback) {
        PlaybackStateListener.PlaybackState.PLAY -> if (state is PauseState) {
          this.state = state.play()
        }
        PlaybackStateListener.PlaybackState.PAUSE -> if (state is PlayState) {
          this.state = state.pause()
        }
      }
    }
  }

  private fun isSignificantlyDifferent(state: PlayerState, other: PlayerState): Boolean {
    val stateSong = state.entry
    val otherSong = other.entry
    // TODO check for correctness
    return ((otherSong == null) != (stateSong == null)) || ((otherSong != null) && (stateSong != otherSong))
  }

  private fun autoPlay() {
    try {
      val state = this.state
      logger.debug("Waiting for song to finish")
      playback.waitForFinish()
      logger.trace("Waiting done")

      stateLock.withLock {
        // Prevent auto next calls if next was manually called
        if (isSignificantlyDifferent(this.state, state)) {
          logger.debug("Skipping auto call to next()")
        } else {
          logger.debug("Auto call to next()")
          next()
        }
      }

      autoPlayer.submit { this.autoPlay() }
    } catch (e: InterruptedException) {
      logger.info("autoPlay interrupted", e)
    }
  }

  @Throws(IOException::class)
  override fun close() {
    try {
      autoPlayer.shutdownNow()
      playback.close()
    } catch (e: Exception) {
      throw IOException(e)
    }
  }
}

private object DummyPlayback : Playback {
  override fun play() {}

  override fun pause() {}

  @Throws(InterruptedException::class)
  override fun waitForFinish() {
    Thread.sleep(2000)
  }

  override fun close() {}
}
