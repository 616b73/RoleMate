import { useRef, useCallback } from 'react';

/**
 * Custom hook for synthesizing notification sounds using the Web Audio API.
 * No external audio files needed — all sounds are generated programmatically.
 */
export function useSoundEffects() {
  const audioContextRef = useRef(null);

  const getContext = useCallback(() => {
    if (!audioContextRef.current) {
      audioContextRef.current = new (window.AudioContext || window.webkitAudioContext)();
    }
    return audioContextRef.current;
  }, []);

  /**
   * Plays a short tone at the given frequency and duration.
   */
  const playTone = useCallback((frequency, startTime, duration, volume = 0.15) => {
    const ctx = getContext();

    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);

    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(frequency, startTime);

    // Fade in/out to avoid clicks
    gainNode.gain.setValueAtTime(0, startTime);
    gainNode.gain.linearRampToValueAtTime(volume, startTime + 0.02);
    gainNode.gain.linearRampToValueAtTime(0, startTime + duration);

    oscillator.start(startTime);
    oscillator.stop(startTime + duration);
  }, [getContext]);

  /**
   * Match found — pleasant ascending two-tone chime.
   */
  const playMatchSound = useCallback(() => {
    try {
      const ctx = getContext();
      const now = ctx.currentTime;
      playTone(523, now, 0.15, 0.12);        // C5
      playTone(659, now + 0.12, 0.2, 0.12);  // E5
    } catch (err) {
      console.warn('[Sound] Failed to play match sound:', err);
    }
  }, [getContext, playTone]);

  /**
   * Message received — subtle short pop.
   */
  const playMessageSound = useCallback(() => {
    try {
      const ctx = getContext();
      const now = ctx.currentTime;
      playTone(880, now, 0.08, 0.06);  // A5, very short and quiet
    } catch (err) {
      console.warn('[Sound] Failed to play message sound:', err);
    }
  }, [getContext, playTone]);

  return { playMatchSound, playMessageSound };
}
