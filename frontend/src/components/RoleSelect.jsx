import { useState } from 'react';
import './RoleSelect.css';

const ROLES = [
  { name: 'Backend Engineering', icon: '⚙️', color: 'var(--role-backend)' },
  { name: 'Frontend Engineering', icon: '🎨', color: 'var(--role-frontend)' },
  { name: 'Full Stack Development', icon: '🔗', color: 'var(--role-fullstack)' },
  { name: 'Data Science', icon: '📊', color: 'var(--role-datascience)' },
  { name: 'DevOps', icon: '🚀', color: 'var(--role-devops)' },
  { name: 'UI/UX Design', icon: '✏️', color: 'var(--role-design)' },
];

export default function RoleSelect({ onJoinQueue }) {
  const [selectedRole, setSelectedRole] = useState(null);

  const handleStart = () => {
    if (selectedRole) {
      onJoinQueue(selectedRole);
    }
  };

  return (
    <div className="role-select animate-in">
      <div className="role-select__header">
        <h1 className="role-select__title">RoleMate</h1>
        <p className="role-select__subtitle">
          Select the role you're preparing for and get matched with a practice partner instantly.
        </p>
      </div>

      <div className="role-select__grid">
        {ROLES.map((role) => (
          <button
            key={role.name}
            className={`role-card ${selectedRole === role.name ? 'role-card--selected' : ''}`}
            style={{ '--role-color': role.color }}
            onClick={() => setSelectedRole(role.name)}
            id={`role-${role.name.toLowerCase().replace(/[\s/]+/g, '-')}`}
          >
            <span className="role-card__icon">{role.icon}</span>
            <span className="role-card__name">{role.name}</span>
          </button>
        ))}
      </div>

      <button
        className="role-select__cta"
        disabled={!selectedRole}
        onClick={handleStart}
        id="find-partner-btn"
      >
        Find Partner
      </button>
    </div>
  );
}
