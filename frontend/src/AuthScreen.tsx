import { FormEvent, useState } from "react";
import { ApiError } from "./api/errors";
import { login, register, type Credentials } from "./api/auth";
import type { CurrentUser } from "./types";

type Mode = "login" | "register";

/** Matches planner-app / identity username rules (canonicalized server-side). */
const USERNAME_PATTERN = "[A-Za-z0-9._\\-]{3,32}";

function authErrorMessage(error: unknown, mode: Mode): string {
  if (!(error instanceof ApiError)) {
    return "Something went wrong. Try again.";
  }
  if (error.code === "username_already_exists") {
    return "That username is already taken.";
  }
  if (error.code === "invalid_credentials") {
    return "Invalid username or password.";
  }
  if (error.code === "validation_failed") {
    return error.message || "Check username and password and try again.";
  }
  if (mode === "login" && error.status === 401) {
    return "Invalid username or password.";
  }
  return error.message || "Something went wrong. Try again.";
}

export function AuthScreen({
  onAuthenticated,
}: {
  onAuthenticated: (user: CurrentUser) => void;
}) {
  const [mode, setMode] = useState<Mode>("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const switchMode = (next: Mode) => {
    setMode(next);
    setError(null);
    setPassword("");
    setConfirmPassword("");
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (mode === "register" && password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    const credentials: Credentials = {
      username: username.trim(),
      password,
    };

    setPending(true);
    try {
      const user =
        mode === "register" ? await register(credentials) : await login(credentials);
      setPassword("");
      setConfirmPassword("");
      onAuthenticated(user);
    } catch (err) {
      setPassword("");
      setConfirmPassword("");
      setError(authErrorMessage(err, mode));
    } finally {
      setPending(false);
    }
  };

  return (
    <section className="card auth-card">
      <h1>{mode === "login" ? "Sign in" : "Create account"}</h1>
      <p className="hint">
        {mode === "login"
          ? "Use your WatchNest username and password."
          : "Choose a username and password. You will be signed in right away."}
      </p>

      <div className="auth-mode-row" role="tablist" aria-label="Authentication mode">
        <button
          type="button"
          role="tab"
          className={mode === "login" ? "auth-mode is-active" : "auth-mode"}
          aria-selected={mode === "login"}
          onClick={() => switchMode("login")}
        >
          Sign in
        </button>
        <button
          type="button"
          role="tab"
          className={mode === "register" ? "auth-mode is-active" : "auth-mode"}
          aria-selected={mode === "register"}
          onClick={() => switchMode("register")}
        >
          Register
        </button>
      </div>

      <form onSubmit={submit}>
        <label htmlFor="authUsername">Username</label>
        <input
          id="authUsername"
          name="username"
          autoComplete="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          pattern={USERNAME_PATTERN}
          title="3–32 characters: letters, digits, '.', '_', or '-'"
          minLength={3}
          maxLength={32}
          required
        />

        <label htmlFor="authPassword">Password</label>
        <input
          id="authPassword"
          name="password"
          type="password"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={8}
          required
        />

        {mode === "register" ? (
          <>
            <label htmlFor="authConfirmPassword">Confirm password</label>
            <input
              id="authConfirmPassword"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              minLength={8}
              required
            />
          </>
        ) : null}

        {error ? (
          <p className="status-note" role="alert">
            {error}
          </p>
        ) : null}

        <button type="submit" disabled={pending}>
          {mode === "login" ? "Sign in" : "Create account"}
        </button>
      </form>
    </section>
  );
}
