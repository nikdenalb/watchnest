import { useEffect, useRef, type MouseEvent, type ReactNode } from "react";

function focusableIn(container: HTMLElement): HTMLElement[] {
  return Array.from(
    container.querySelectorAll<HTMLElement>(
      "button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex=\"-1\"])",
    ),
  ).filter((el) => el.tabIndex !== -1 && !el.hasAttribute("disabled"));
}

export function OverlayDialog({
  labelledBy,
  onClose,
  isTop,
  children,
}: {
  labelledBy: string;
  onClose: () => void;
  isTop: boolean;
  children: ReactNode;
}) {
  const panelRef = useRef<HTMLDivElement>(null);
  const returnFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    returnFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    panelRef.current?.focus();
    return () => {
      returnFocusRef.current?.focus();
    };
  }, []);

  useEffect(() => {
    if (!isTop) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        event.stopImmediatePropagation();
        onClose();
        return;
      }
      if (event.key !== "Tab" || !panelRef.current) {
        return;
      }
      const nodes = focusableIn(panelRef.current);
      if (nodes.length === 0) {
        event.preventDefault();
        panelRef.current.focus();
        return;
      }
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", onKeyDown, true);
    return () => document.removeEventListener("keydown", onKeyDown, true);
  }, [isTop, onClose]);

  const onBackdrop = (event: MouseEvent<HTMLDivElement>) => {
    if (isTop && event.target === event.currentTarget) {
      onClose();
    }
  };

  return (
    <div className={isTop ? "dialog-backdrop" : "dialog-backdrop dialog-backdrop--under"} onMouseDown={onBackdrop}>
      <div
        ref={panelRef}
        className="dialog-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        tabIndex={-1}
      >
        {children}
      </div>
    </div>
  );
}
