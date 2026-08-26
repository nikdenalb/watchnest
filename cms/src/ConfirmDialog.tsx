import { OverlayDialog } from "./OverlayDialog";

export function ConfirmDialog({
  title,
  titleId,
  message,
  confirmLabel,
  pending,
  onConfirm,
  onClose,
}: {
  title: string;
  titleId: string;
  message: string;
  confirmLabel: string;
  pending: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <OverlayDialog labelledBy={titleId} onClose={pending ? () => undefined : onClose} isTop>
      <div className="dialog-head">
        <h2 id={titleId}>{title}</h2>
      </div>
      <p>{message}</p>
      <div className="dialog-actions">
        <button type="button" className="linkish" onClick={onClose} disabled={pending}>
          Cancel
        </button>
        <button type="button" className="dialog-danger" onClick={onConfirm} disabled={pending}>
          {confirmLabel}
        </button>
      </div>
    </OverlayDialog>
  );
}
