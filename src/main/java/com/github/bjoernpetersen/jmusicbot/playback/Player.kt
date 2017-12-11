package com.github.bjoernpetersen.jmusicbot.playback

import com.github.bjoernpetersen.jmusicbot.Loggable
import com.github.bjoernpetersen.jmusicbot.Song
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackStateListener.PlaybackState
import com.github.bjoernpetersen.jmusicbot.provider.BrokenSuggesterException
import com.github.bjoernpetersen.jmusicbot.provider.Suggester
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.io.Closeable
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import java.util.logging.Logger
import kotlin.concurrent.withLock

class Player(private val songPlayedNotifier: Consumer<SongEntry>, private val suggester: Suggester?) : Loggable,
    Closeable {

  private val logger: Logger = createLogger()
  private val autoPlayer: ExecutorService = Executors.newSingleThreadExecutor(
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("playerPool-%d")
          .build()
  )

  val queue: Queue = Queue()

  private val stateLock: Lock = ReentrantLock()
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

  override fun getLogger(): Logger {
    return logger
  }

  fun addListener(listener: PlayerStateListener) {
    stateListeners.add(listener)

  }

  fun removeListener(listener: PlayerStateListener) {
    stateListeners.remove(listener)
  }

  fun pause() {
    stateLock.withLock {
      logFinest("Pausing...")
      val state = state
      if (state is PauseState) {
        logFinest("Already paused.")
        return
      } else if (state !is PlayState) {
        logFiner("Tried to pause player in state %s", state)
        return
      }
      playback.pause()
      this.state = state.pause()
    }
  }

  fun play() {
    stateLock.withLock {
      logFinest("Playing...")
      val state = state
      if (state is PlayState) {
        logFinest("Already playing.")
        return
      } else if (state !is PauseState) {
        logFiner("Tried to play in state %s", state)
        return
      }
      playback.play()
      this.state = state.play()
    }
  }

  @Throws(InterruptedException::class)
  fun next() {
    val state = this.state
    stateLock.withLock {
      logFiner("Next...")
      val newState = this.state
      if (isSignificantlyDifferent(state, newState)) {
        logFinest("Skipping next call due to state change while waiting for lock.")
        return
      }

      try {
        playback.close()
      } catch (e: Exception) {
        logWarning(e, "Error closing playback")
      }

      val nextOptional = queue.pop()
      if (!nextOptional.isPresent && suggester == null) {
        logFinest("Queue is empty. Stopping.")
        playback = DummyPlayback
        this.state = StopState()
        return
      }

      val nextEntry: SongEntry = if (nextOptional.isPresent) {
        nextOptional.get()
      } else try {
        SuggestedSongEntry(suggester!!.suggestNext())
      } catch (e: BrokenSuggesterException) {
        logFine("Default suggester could not suggest anything. Stopping.")
        playback = DummyPlayback
        this.state = StopState()
        return
      }

      val nextSong = nextEntry.song
      songPlayedNotifier.accept(nextEntry)
      logFine("Next song is: " + nextSong)
      try {
        playback = nextSong.playback
      } catch (e: IOException) {
        logWarning(e, "Error creating playback")

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
    logFinest("Playback state update: %s", feedback)
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
      logFinest("Waiting for song to finish")
      playback.waitForFinish()
      logFinest("Waiting done")

      stateLock.withLock {
        // Prevent auto next calls if next was manually called
        if (isSignificantlyDifferent(this.state, state)) {
          logFinest("Skipping auto call to next()")
        } else {
          logFinest("Auto call to next()")
          next()
        }
      }

      autoPlayer.submit { this.autoPlay() }
    } catch (e: InterruptedException) {
      logFine("autoPlay interrupted", e)
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
