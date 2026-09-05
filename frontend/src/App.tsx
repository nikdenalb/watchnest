import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { fetchMe } from "./api/auth";
import { AuthScreen } from "./AuthScreen";
import { SplashScreen } from "./SplashScreen";
import { WatchDiary } from "./WatchDiary";
import { ME_QUERY_KEY } from "./session";

function PageShell({ children }: { children: ReactNode }) {
  return (
    <main className="page app-enter">
      <div className="page-top">
        <p className="eyebrow">WatchNest</p>
      </div>
      {children}
    </main>
  );
}

export function App() {
  const queryClient = useQueryClient();
  const [splashDismissed, setSplashDismissed] = useState(false);

  const meQuery = useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: fetchMe,
    retry: false,
  });

  const showSplash = !splashDismissed;
  const appReady = meQuery.isFetched;

  if (showSplash) {
    return (
      <SplashScreen appReady={appReady} onDismiss={() => setSplashDismissed(true)} />
    );
  }

  if (meQuery.isPending) {
    return (
      <PageShell>
        <p>Checking session...</p>
      </PageShell>
    );
  }

  if (meQuery.isError) {
    return (
      <PageShell>
        <p>Failed to resolve authentication state.</p>
      </PageShell>
    );
  }

  if (!meQuery.data) {
    return (
      <PageShell>
        <AuthScreen
          onAuthenticated={(nextUser) => {
            queryClient.setQueryData(ME_QUERY_KEY, nextUser);
          }}
        />
      </PageShell>
    );
  }

  return <WatchDiary user={meQuery.data} />;
}
