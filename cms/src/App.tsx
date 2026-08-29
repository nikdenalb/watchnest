import { useQuery, useQueryClient } from "@tanstack/react-query";
import { type ReactNode, useEffect } from "react";
import { fetchMe } from "./api/auth";
import { CatalogEditor } from "./CatalogEditor";
import { SignInScreen } from "./SignInScreen";
import { ME_QUERY_KEY, TITLES_QUERY_KEY } from "./session";

function PageShell({ children }: { children: ReactNode }) {
  return (
    <main className="page app-enter">
      <div className="page-top">
        <p className="eyebrow">WatchNest CMS</p>
      </div>
      {children}
    </main>
  );
}

export function App() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const onPageShow = (event: PageTransitionEvent) => {
      if (event.persisted) {
        void queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY });
      }
    };
    window.addEventListener("pageshow", onPageShow);
    return () => window.removeEventListener("pageshow", onPageShow);
  }, [queryClient]);

  const meQuery = useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: fetchMe,
    retry: false,
  });

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
        <SignInScreen
          onAuthenticated={(nextUser) => {
            queryClient.setQueryData(ME_QUERY_KEY, nextUser);
            void queryClient.invalidateQueries({ queryKey: TITLES_QUERY_KEY });
          }}
        />
      </PageShell>
    );
  }

  return <CatalogEditor user={meQuery.data} />;
}
