package com.github.bjoernpetersen.jmusicbot.playback


object DummyPlayback : Playback {
    override fun play() {}

    override fun pause() {}

    @Throws(InterruptedException::class)
    override fun waitForFinish() {
        Thread.sleep(2000)
    }

    override fun close() {}

}