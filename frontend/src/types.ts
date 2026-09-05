export type WatchEvent = {
  id: string;
  ownerId: string;
  watchedOn: string;
  contentTitle: string;
};

export type WatchEventRange = {
  from: string;
  to: string;
  events: WatchEvent[];
};

export type CurrentUser = {
  id: string;
  username: string;
};

export type CsrfToken = {
  headerName: string;
  token: string;
};
