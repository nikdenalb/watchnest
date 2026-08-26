import type { CatalogTitle } from "../types";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly existingTitle?: CatalogTitle;

  constructor(status: number, code: string, message: string, existingTitle?: CatalogTitle) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.existingTitle = existingTitle;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
