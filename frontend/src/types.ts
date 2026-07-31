export type DailyScreenTimeStatus = {
  date: string;
  episodeLimit: number;
  episodesWatched: number;
  episodesRemaining: number;
  overQuota: boolean;
  canWatchAnotherEpisode: boolean;
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

export type Dashboard = {
  displayName: string;
  today: string;
  status: DailyScreenTimeStatus;
  policy: ScreenTimePolicy;
  todayEvents: WatchEvent[];
};
