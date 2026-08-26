import { FormEvent, useState } from "react";
import { ApiError } from "./api/errors";
import { login, type Credentials } from "./api/auth";
import type { CmsUser } from "./types";

/** Matches identity username rules (canonicalized server-side). */
const USERNAME_PATTERN = "[A-Za-z0-9._\\-]{3,32}";

function signInErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return "Something went wrong. Try again.";
  }
  if (error.code === "invalid_credentials" || error.status === 401) {
    return "Invalid username or password.";
  }
  if (error.code === "validation_failed") {
    return error.message || "Check username and password and try again.";
  }
  return error.message || "Something went wrong. Try again.";
}

export function SignInScreen({
  onAuthenticated,
}: {
  onAuthenticated: (user: CmsUser) => void;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    const credentials: Credentials = {
      username: username.trim(),
      password,
    };

    setPending(true);
    try {
      const user = await login(credentials);
      setPassword("");
      onAuthenticated(user);
    } catch (err) {
      setPassword("");
      setError(signInErrorMessage(err));
    } finally {
      setPending(false);
    }
  };

  return (
    <section className="card auth-card">
      <h1>Sign in</h1>
      <p className="hint">Use your CMS username and password.</p>

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
          disabled={pending}
        />

        <label htmlFor="authPassword">Password</label>
        <input
          id="authPassword"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={8}
          required
          disabled={pending}
        />

        {error ? (
          <p className="status-note" role="alert">
            {error}
          </p>
        ) : null}

        <button type="submit" disabled={pending}>
          Sign in
        </button>
      </form>
    </section>
  );
}
