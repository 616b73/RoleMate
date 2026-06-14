import './WaitingScreen.css';

export default function WaitingScreen({ role, onCancel }) {
  return (
    <div className="waiting animate-in">
      <div className="waiting__content">
        <div className="waiting__spinner">
          <div className="waiting__dots">
            <span className="waiting__dot" style={{ animationDelay: '0s' }}></span>
            <span className="waiting__dot" style={{ animationDelay: '0.2s' }}></span>
            <span className="waiting__dot" style={{ animationDelay: '0.4s' }}></span>
          </div>
        </div>

        <h2 className="waiting__title">Searching for a partner...</h2>
        <p className="waiting__role">
          Role: <strong>{role}</strong>
        </p>
        <p className="waiting__hint">
          You'll be matched with someone preparing for the same role.
        </p>

        <button
          className="waiting__cancel"
          onClick={onCancel}
          id="cancel-queue-btn"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
