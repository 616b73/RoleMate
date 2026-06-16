import { useRef, useEffect } from 'react';
import './VideoPanel.css';

/**
 * Renders local and remote video streams.
 * Remote video is the main (large) view; local video is a small PiP overlay.
 */
export default function VideoPanel({
  localStream,
  remoteStream,
  isPartnerVideoActive,
  videoError,
}) {
  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);

  // Attach local stream to video element
  useEffect(() => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
    }
  }, [localStream]);

  // Attach remote stream to video element
  useEffect(() => {
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
    }
  }, [remoteStream]);

  return (
    <div className="video-panel animate-in">
      {/* Error State */}
      {videoError && (
        <div className="video-panel__error">
          <span className="video-panel__error-icon">⚠️</span>
          <span>{videoError}</span>
        </div>
      )}

      {/* Remote Video (main view) */}
      <div className="video-panel__remote">
        {remoteStream && isPartnerVideoActive ? (
          <video
            ref={remoteVideoRef}
            className="video-panel__video video-panel__video--remote"
            autoPlay
            playsInline
            id="remote-video"
          />
        ) : (
          <div className="video-panel__placeholder">
            <div className="video-panel__placeholder-icon">📹</div>
            <p className="video-panel__placeholder-text">
              {isPartnerVideoActive
                ? 'Connecting to partner...'
                : 'Waiting for partner to start video...'}
            </p>
          </div>
        )}

        {/* Local Video (PiP overlay) */}
        {localStream && (
          <div className="video-panel__local">
            <video
              ref={localVideoRef}
              className="video-panel__video video-panel__video--local"
              autoPlay
              playsInline
              muted
              id="local-video"
            />
            <span className="video-panel__local-label">You</span>
          </div>
        )}
      </div>
    </div>
  );
}
