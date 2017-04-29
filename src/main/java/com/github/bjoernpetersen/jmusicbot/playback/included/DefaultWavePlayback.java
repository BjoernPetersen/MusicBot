package com.github.bjoernpetersen.jmusicbot.playback.included;

import com.github.bjoernpetersen.jmusicbot.playback.AbstractPlayback;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

final class DefaultWavePlayback extends AbstractPlayback {

  @Nonnull
  private final Clip clip;
  private boolean paused = false;

  DefaultWavePlayback(@Nonnull InputStream in) throws IOException, UnsupportedAudioFileException {
    AudioInputStream audio = AudioSystem.getAudioInputStream(in);
    try {
      this.clip = AudioSystem.getClip();
      clip.open(audio);
    } catch (LineUnavailableException e) {
      throw new IOException(e);
    }
    clip.addLineListener(event -> {
      if (event.getType() == Type.STOP && !paused) {
        markDone();
      }
    });
  }

  @Override
  public void play() {
    paused = false;
    clip.start();
  }

  @Override
  public void pause() {
    paused = true;
    clip.stop();
  }

  @Override
  public void close() throws Exception {
    clip.close();
    super.close();
  }
}
