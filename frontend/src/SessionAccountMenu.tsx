import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { logout } from "./api/auth";
import { clearUserScopedQueries } from "./session";

const BUTTON_ID = "session-account-button";
const PANEL_ID = "session-account-panel";

export function SessionAccountMenu({
  username,
}: {
  username: string;
}) {
  const queryClient = useQueryClient();
  const rootRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearUserScopedQueries(queryClient);
    },
  });

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    const onPointerDown = (event: PointerEvent) => {
      const root = rootRef.current;
      if (root && event.target instanceof Node && !root.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("pointerdown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open]);

  return (
    <div className="session-bar" ref={rootRef}>
      <button
        type="button"
        id={BUTTON_ID}
        className="session-user"
        aria-expanded={open}
        aria-controls={open ? PANEL_ID : undefined}
        onClick={() => setOpen((current) => !current)}
      >
        {username}
      </button>
      {open ? (
        <div id={PANEL_ID} className="session-account-panel" aria-labelledby={BUTTON_ID}>
          <button
            type="button"
            className="linkish"
            onClick={() => logoutMutation.mutate()}
            disabled={logoutMutation.isPending}
          >
            Log out
          </button>
        </div>
      ) : null}
    </div>
  );
}
