import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-100 p-8">
      <div className="mx-auto max-w-4xl rounded-lg bg-white p-8 shadow-md">
        <div className="mb-4 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-slate-900">Dashboard</h1>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-700"
          >
            Log out
          </button>
        </div>
        {user && <p className="mb-2 text-sm text-slate-500">Signed in as {user.email}</p>}
        <p className="text-sm text-slate-500">Dashboard sections go here (Phase 5).</p>
      </div>
    </div>
  );
}
