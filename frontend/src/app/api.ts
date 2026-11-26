import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** JSON shapes from the Spring API (see ApiDtos.java). */

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];

export interface BookSummary {
  id: string;
  title: string;
  author: string;
  difficulty: Difficulty | null;
  inProgress: boolean;
}

export interface Player {
  name: string;
}

export interface OptionView {
  index: number;
  description: string;
}

export type PlayStatus = 'PLAYING' | 'DEAD' | 'FINISHED';

export interface PlayState {
  bookId: string;
  bookTitle: string;
  sectionId: number;
  text: string;
  options: OptionView[];
  health: number;
  maxHealth: number;
  message: string | null;
  status: PlayStatus;
}

export interface ApiError {
  message: string;
  state: PlayState | null;
}

/** Body of POST /api/books and /api/books/validate: a book in the same shape as the files in the books folder. */
export interface NewBook {
  title: string;
  author: string;
  difficulty: string;
  sections: NewSection[];
}

export interface NewSection {
  id: number | null;
  text: string;
  type: string;
  options?: NewOption[];
}

export interface NewOption {
  description: string;
  gotoId: number | null;
  consequence?: NewConsequence;
}

export interface NewConsequence {
  type: string;
  value: string;
  text: string;
}

/** The server's verdict on a new book, using the same rules as when the books folder is loaded. */
export interface BookValidation {
  valid: boolean;
  problems: string[];
}

export interface BookFilters {
  q?: string | null;
  author?: string | null;
  difficulty?: string | null;
}

/** Pulls the server's ApiError out of an HTTP error, if it sent one. */
export function apiError(err: unknown): ApiError | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error.message === 'string') {
    return err.error as ApiError;
  }
  return null;
}

@Injectable({ providedIn: 'root' })
export class BooksApi {
  private readonly http = inject(HttpClient);

  list(filters: BookFilters): Observable<BookSummary[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<BookSummary[]>('/api/books', { params });
  }

  authors(): Observable<string[]> {
    return this.http.get<string[]>('/api/authors');
  }

  /** Checks a book without saving it. */
  validate(book: NewBook): Observable<BookValidation> {
    return this.http.post<BookValidation>('/api/books/validate', book);
  }

  /** Saves a book. Fails with 400 and a BookValidation body if the server rejects it. */
  create(book: NewBook): Observable<BookSummary> {
    return this.http.post<BookSummary>('/api/books', book);
  }
}

@Injectable({ providedIn: 'root' })
export class PlayerApi {
  private readonly http = inject(HttpClient);

  /** Errors with 401 if nobody has registered in this browser. */
  me(): Observable<Player> {
    return this.http.get<Player>('/api/players/me');
  }

  register(name: string): Observable<Player> {
    return this.http.post<Player>('/api/players', { name });
  }
}

@Injectable({ providedIn: 'root' })
export class GameApi {
  private readonly http = inject(HttpClient);

  current(bookId: string): Observable<PlayState> {
    return this.http.get<PlayState>(`/api/books/${encodeURIComponent(bookId)}/play`);
  }

  choose(bookId: string, fromSectionId: number, optionIndex: number): Observable<PlayState> {
    return this.http.post<PlayState>(`/api/books/${encodeURIComponent(bookId)}/play/choice`, {
      fromSectionId,
      optionIndex,
    });
  }

  restart(bookId: string): Observable<PlayState> {
    return this.http.post<PlayState>(`/api/books/${encodeURIComponent(bookId)}/play/restart`, {});
  }
}
