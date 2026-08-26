import { FormEvent, useState } from "react";
import { isApiError } from "./api/errors";
import { TITLE_TYPES, type CatalogTitle, type TitleType, type TitleWrite } from "./types";

function emptyForm(title?: CatalogTitle): {
  type: TitleType;
  nameEn: string;
  nameOriginal: string;
  year: string;
  description: string;
  genres: string;
  countries: string;
} {
  return {
    type: title?.type ?? "FILM",
    nameEn: title?.nameEn ?? "",
    nameOriginal: title?.nameOriginal ?? "",
    year: title ? String(title.year) : "",
    description: title?.description ?? "",
    genres: title?.genres ?? "",
    countries: title?.countries ?? "",
  };
}

function optionalText(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function MutationAlert({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  if (isApiError(error) && error.code === "title_already_exists" && error.existingTitle) {
    const existing = error.existingTitle;
    return (
      <div className="status-note" role="alert">
        <p>{error.message}</p>
        <p>
          Existing title: {existing.nameEn} ({existing.year}, {existing.type}), id {existing.id}
          {existing.nameOriginal ? ` · ${existing.nameOriginal}` : ""}
          {existing.genres ? ` · ${existing.genres}` : ""}
          {existing.countries ? ` · ${existing.countries}` : ""}
        </p>
      </div>
    );
  }
  if (isApiError(error) && error.code === "validation_failed") {
    return (
      <p className="status-note" role="alert">
        {error.message || "Check the title fields and try again."}
      </p>
    );
  }
  if (isApiError(error)) {
    return (
      <p className="status-note" role="alert">
        {error.message || "Something went wrong. Try again."}
      </p>
    );
  }
  return (
    <p className="status-note" role="alert">
      Something went wrong. Try again.
    </p>
  );
}

export function TitleForm({
  title,
  pending,
  error,
  onSubmit,
  onDelete,
}: {
  title?: CatalogTitle;
  pending: boolean;
  error: unknown;
  onSubmit: (fields: TitleWrite) => void;
  onDelete?: () => void;
}) {
  const editing = title !== undefined;
  const [form, setForm] = useState(() => emptyForm(title));

  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit({
      type: form.type,
      nameEn: form.nameEn.trim(),
      nameOriginal: form.nameOriginal.trim(),
      year: Number(form.year),
      description: optionalText(form.description),
      genres: optionalText(form.genres),
      countries: optionalText(form.countries),
    });
  };

  return (
    <article className="card">
      <h2>{editing ? "Edit title" : "Create title"}</h2>
      <form onSubmit={submit}>
        <label htmlFor="titleType">Type</label>
        <select
          id="titleType"
          name="type"
          value={form.type}
          onChange={(event) => setForm({ ...form, type: event.target.value as TitleType })}
          required
          disabled={pending}
        >
          {TITLE_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>

        <label htmlFor="nameEn">English name</label>
        <input
          id="nameEn"
          name="nameEn"
          value={form.nameEn}
          onChange={(event) => setForm({ ...form, nameEn: event.target.value })}
          minLength={1}
          maxLength={255}
          required
          disabled={pending}
        />

        <label htmlFor="nameOriginal">Original name</label>
        <input
          id="nameOriginal"
          name="nameOriginal"
          value={form.nameOriginal}
          onChange={(event) => setForm({ ...form, nameOriginal: event.target.value })}
          minLength={1}
          maxLength={255}
          required
          disabled={pending}
        />

        <label htmlFor="titleYear">Year</label>
        <input
          id="titleYear"
          name="year"
          type="number"
          min={1000}
          max={9999}
          step={1}
          value={form.year}
          onChange={(event) => setForm({ ...form, year: event.target.value })}
          required
          disabled={pending}
        />

        <label htmlFor="titleDescription">Description</label>
        <textarea
          id="titleDescription"
          name="description"
          value={form.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })}
          maxLength={10000}
          disabled={pending}
        />

        <label htmlFor="titleGenres">Genres</label>
        <input
          id="titleGenres"
          name="genres"
          value={form.genres}
          onChange={(event) => setForm({ ...form, genres: event.target.value })}
          maxLength={1000}
          disabled={pending}
        />

        <label htmlFor="titleCountries">Countries</label>
        <input
          id="titleCountries"
          name="countries"
          value={form.countries}
          onChange={(event) => setForm({ ...form, countries: event.target.value })}
          maxLength={1000}
          disabled={pending}
        />

        <MutationAlert error={error} />

        <div className="form-actions">
          <button type="submit" disabled={pending}>
            {editing ? "Save" : "Create"}
          </button>
          {editing && onDelete ? (
            <button type="button" className="linkish" onClick={onDelete} disabled={pending}>
              Delete
            </button>
          ) : null}
        </div>
      </form>
    </article>
  );
}
