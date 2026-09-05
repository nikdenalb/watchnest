import type { CurrentUser } from "./types";
import { SessionAccountMenu } from "./SessionAccountMenu";
import { WatchArchiveSection } from "./WatchArchiveSection";

export function WatchDiary({ user }: { user: CurrentUser }) {
  return (
    <main className="page app-enter">
      <div className="page-top">
        <p className="eyebrow">WatchNest</p>
        <SessionAccountMenu username={user.username} />
      </div>
      <WatchArchiveSection />
    </main>
  );
}
