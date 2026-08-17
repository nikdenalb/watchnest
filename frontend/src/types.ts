export type PlanLineSource = "FORWARD" | "MANUAL";

export type PlanTodayLine = {
  id: string;
  contentTitle: string;
  checked: boolean;
  source: PlanLineSource;
};

export type PlanToday = {
  date: string;
  lines: PlanTodayLine[];
};

export type DailyScreenTimeStatus = {
  date: string;
  episodeLimit: number;
  episodesPlanned: number;
  episodesRemaining: number;
  overQuota: boolean;
  canAddAnotherEpisode: boolean;
};

export type ScreenTimePolicy = {
  weekdayEpisodeLimit: number;
  weekendEpisodeLimit: number;
};

export type WatchEvent = {
  id: string;
  ownerId: string;
  watchedOn: string;
  contentTitle: string;
};

export type WatchEventArchive = {
  from: string;
  to: string;
  events: WatchEvent[];
};

export type ForwardPlanItem = {
  id: string;
  plannedFor: string;
  contentTitle: string;
};

export type ForwardPlan = {
  from: string;
  to: string;
  items: ForwardPlanItem[];
};

export type Dashboard = {
  displayName: string;
  today: string;
  status: DailyScreenTimeStatus;
  policy: ScreenTimePolicy;
  planToday: PlanToday;
};

export type CurrentUser = {
  id: string;
  username: string;
};

export type CsrfToken = {
  headerName: string;
  token: string;
};
