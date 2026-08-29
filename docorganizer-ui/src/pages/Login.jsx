import { useState } from "react";
import API, { errorMessage } from "../api/api";

const EMPTY = { name: "", email: "", password: "" };

function Login({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const isRegister = mode === "register";

  const update = (field) => (event) => {
    setForm({ ...form, [field]: event.target.value });
  };

  const switchMode = () => {
    setMode(isRegister ? "login" : "register");
    setError("");
  };

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setBusy(true);

    try {
      if (isRegister) {
        await API.post("/api/auth/register", {
          name: form.name.trim(),
          email: form.email.trim(),
          password: form.password,
        });

        // Sign the new account straight in rather than making them retype it.
        const res = await API.post("/api/auth/login", {
          email: form.email.trim(),
          password: form.password,
        });

        onAuthenticated(res.data.token, res.data.user);
      } else {
        const res = await API.post("/api/auth/login", {
          email: form.email.trim(),
          password: form.password,
        });

        onAuthenticated(res.data.token, res.data.user);
      }
    } catch (err) {
      setError(errorMessage(err, isRegister ? "Could not create the account" : "Could not sign in"));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="auth-logo" aria-hidden="true">
            ◆
          </span>
          <div>
            <h1>Document Organizer</h1>
            <p className="muted">Private, tagged, searchable document storage.</p>
          </div>
        </div>

        <div className="tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={!isRegister}
            className={!isRegister ? "tab active" : "tab"}
            onClick={() => !isRegister || switchMode()}
          >
            Sign in
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={isRegister}
            className={isRegister ? "tab active" : "tab"}
            onClick={() => isRegister || switchMode()}
          >
            Create account
          </button>
        </div>

        <form onSubmit={submit} className="auth-form">
          {isRegister && (
            <label className="field">
              <span>Name</span>
              <input
                value={form.name}
                onChange={update("name")}
                placeholder="Vivek Kumar"
                autoComplete="name"
                required
              />
            </label>
          )}

          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={form.email}
              onChange={update("email")}
              placeholder="you@example.com"
              autoComplete="username"
              required
            />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={form.password}
              onChange={update("password")}
              placeholder={isRegister ? "At least 8 characters" : "••••••••"}
              autoComplete={isRegister ? "new-password" : "current-password"}
              minLength={isRegister ? 8 : undefined}
              required
            />
          </label>

          {error && (
            <p className="alert error" role="alert">
              {error}
            </p>
          )}

          <button type="submit" className="btn primary block" disabled={busy}>
            {busy ? "Working…" : isRegister ? "Create account" : "Sign in"}
          </button>
        </form>

        <p className="muted center small">
          {isRegister ? "Already registered? " : "No account yet? "}
          <button type="button" className="link" onClick={switchMode}>
            {isRegister ? "Sign in" : "Create one"}
          </button>
        </p>
      </div>
    </div>
  );
}

export default Login;
