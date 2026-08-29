import { useCallback, useEffect, useState } from "react";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import API, { getToken, setToken, setUnauthorizedHandler } from "./api/api";
import "./styles.css";

function App() {
  const [token, setTokenState] = useState(() => getToken());
  const [user, setUser] = useState(null);
  const [booting, setBooting] = useState(Boolean(getToken()));

  const signOut = useCallback(() => {
    setToken(null);
    setTokenState(null);
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(signOut);
  }, [signOut]);

  // A token in localStorage is not proof of a live session: confirm it against /me
  // before showing the dashboard.
  useEffect(() => {
    // No token means there is nothing to restore: `booting` already initialised to false.
    if (!token) {
      return undefined;
    }

    let cancelled = false;

    API.get("/api/auth/me")
      .then((res) => {
        if (!cancelled) setUser(res.data);
      })
      .catch(() => {
        if (!cancelled) signOut();
      })
      .finally(() => {
        if (!cancelled) setBooting(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token, signOut]);

  const handleAuthenticated = (newToken, profile) => {
    setToken(newToken);
    setTokenState(newToken);
    setUser(profile);
  };

  if (booting) {
    return (
      <div className="boot">
        <div className="spinner" aria-hidden="true" />
        <p>Restoring your session…</p>
      </div>
    );
  }

  if (!token) {
    return <Login onAuthenticated={handleAuthenticated} />;
  }

  return <Dashboard user={user} onSignOut={signOut} />;
}

export default App;
