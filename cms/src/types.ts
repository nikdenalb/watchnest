export const TITLE_TYPES = ["FILM", "TV_SERIES", "MINI_SERIES", "TV_SHOW"] as const;

export type TitleType = (typeof TITLE_TYPES)[number];

export type CmsUser = {
  id: string;
  username: string;
};

export type CsrfToken = {
  headerName: string;
  token: string;
};

export type CatalogTitle = {
  id: string;
  type: TitleType;
  nameEn: string;
  nameOriginal: string;
  year: number;
  description: string | null;
  genres: string | null;
  countries: string | null;
};

export type TitleWrite = {
  type: TitleType;
  nameEn: string;
  nameOriginal: string;
  year: number;
  description: string | null;
  genres: string | null;
  countries: string | null;
};

export type TitleList = {
  titles: CatalogTitle[];
};
