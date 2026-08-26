import { apiRequest } from "./http";
import type { CatalogTitle, TitleList, TitleWrite } from "../types";

export async function fetchTitles(q = ""): Promise<TitleList> {
  const trimmed = q.trim();
  if (!trimmed) {
    return apiRequest<TitleList>("/cms/api/v1/titles");
  }
  const params = new URLSearchParams({ q: trimmed });
  return apiRequest<TitleList>(`/cms/api/v1/titles?${params.toString()}`);
}

export async function fetchTitle(id: string): Promise<CatalogTitle> {
  return apiRequest<CatalogTitle>(`/cms/api/v1/titles/${id}`);
}

export async function createTitle(fields: TitleWrite): Promise<CatalogTitle> {
  return apiRequest<CatalogTitle>("/cms/api/v1/titles", {
    method: "POST",
    body: JSON.stringify(fields),
  });
}

export async function updateTitle(id: string, fields: TitleWrite): Promise<CatalogTitle> {
  return apiRequest<CatalogTitle>(`/cms/api/v1/titles/${id}`, {
    method: "PUT",
    body: JSON.stringify(fields),
  });
}

export async function deleteTitle(id: string): Promise<void> {
  return apiRequest<void>(`/cms/api/v1/titles/${id}`, {
    method: "DELETE",
  });
}
