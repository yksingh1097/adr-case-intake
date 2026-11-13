import type { Catalog, DocumentDetail, DocumentSummary } from "./types";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number | null,
  ) {
    super(message);
  }
}

async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, { signal });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    throw new ApiError("The server could not be reached. Check that the backend is running on port 8080.", null);
  }
  if (!response.ok) {
    throw new ApiError(`The server answered ${response.status} for ${path}.`, response.status);
  }
  return (await response.json()) as T;
}

export const fetchCatalog = (signal?: AbortSignal) => getJson<Catalog>("/api/catalog", signal);

export const fetchDocuments = (signal?: AbortSignal) => getJson<DocumentSummary[]>("/api/documents", signal);

export const fetchDocument = (id: string, signal?: AbortSignal) =>
  getJson<DocumentDetail>(`/api/documents/${encodeURIComponent(id)}`, signal);
