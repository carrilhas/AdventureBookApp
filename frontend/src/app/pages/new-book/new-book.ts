import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, of, switchMap, tap } from 'rxjs';
import { BookValidation, BooksApi, DIFFICULTIES, NewBook } from '../../api';

export const SECTION_TYPES = ['BEGIN', 'NODE', 'END'] as const;
export const CONSEQUENCE_TYPES = ['LOSE_HEALTH', 'GAIN_HEALTH'] as const;

/** What the form edits. Kept loose (empty strings, null ids) and turned into a NewBook by toRequest(). */
interface DraftConsequence {
  type: string; // '' = no consequence
  value: number | null;
  text: string;
}

interface DraftOption {
  description: string;
  gotoId: number | null;
  consequence: DraftConsequence;
}

interface DraftSection {
  id: number | null;
  type: string;
  text: string;
  options: DraftOption[];
}

interface Draft {
  title: string;
  author: string;
  difficulty: string;
  sections: DraftSection[];
}

/**
 * Writes a new book. Nothing is checked in the browser: every edit is sent to POST /api/books/validate, which runs
 * the same rules BookLoader applies to the books folder, and Save stays disabled until the server says the book is
 * valid. POST /api/books checks again when saving.
 */
@Component({
  selector: 'app-new-book',
  imports: [FormsModule, RouterLink],
  templateUrl: './new-book.html',
})
export class NewBookPage {
  private readonly booksApi = inject(BooksApi);
  private readonly router = inject(Router);

  protected readonly difficulties = DIFFICULTIES;
  protected readonly sectionTypes = SECTION_TYPES;
  protected readonly consequenceTypes = CONSEQUENCE_TYPES;

  /** Starts as the smallest playable shape: a BEGIN section leading to an END section. */
  protected readonly draft: Draft = {
    title: '',
    author: '',
    difficulty: '',
    sections: [
      { id: 1, type: 'BEGIN', text: '', options: [newOption(2)] },
      { id: 2, type: 'END', text: '', options: [] },
    ],
  };

  /** The server's verdict on the current draft; null until the first check returns. */
  protected readonly validation = signal<BookValidation | null>(null);
  /** True from an edit until the server has checked it. */
  protected readonly checking = signal(true);
  protected readonly saving = signal(false);
  protected readonly canSave = computed(
    () => !this.checking() && !this.saving() && this.validation()?.valid === true,
  );

  private readonly edits = new Subject<void>();

  constructor() {
    this.edits
      .pipe(
        tap(() => this.checking.set(true)),
        debounceTime(400), // wait for a pause in typing
        switchMap(() =>
          this.booksApi.validate(this.toRequest()).pipe(
            catchError(() =>
              of<BookValidation>({ valid: false, problems: ["Couldn't reach the server to check the book."] }),
            ),
          ),
        ), // switchMap drops the answer for an older draft if a newer one is being checked
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        this.validation.set(result);
        this.checking.set(false);
      });
    this.changed();
  }

  /** Called on every edit, including adding and removing sections and options. */
  protected changed(): void {
    this.edits.next();
  }

  /** Section ids that options can point to. */
  protected sectionIds(): number[] {
    return this.draft.sections.map((s) => s.id).filter((id): id is number => id !== null);
  }

  protected addSection(): void {
    const next = Math.max(0, ...this.sectionIds()) + 1;
    this.draft.sections.push({ id: next, type: 'NODE', text: '', options: [newOption(null)] });
    this.changed();
  }

  protected removeSection(index: number): void {
    this.draft.sections.splice(index, 1);
    this.changed();
  }

  protected addOption(section: DraftSection): void {
    section.options.push(newOption(null));
    this.changed();
  }

  protected removeOption(section: DraftSection, index: number): void {
    section.options.splice(index, 1);
    this.changed();
  }

  protected save(): void {
    if (!this.canSave()) {
      return;
    }
    this.saving.set(true);
    this.booksApi.create(this.toRequest()).subscribe({
      next: (book) => this.router.navigate(['/play', book.id]),
      error: (err) => {
        this.saving.set(false);
        const body = err instanceof HttpErrorResponse ? err.error : null;
        this.validation.set(
          body && Array.isArray(body.problems)
            ? { valid: false, problems: body.problems }
            : { valid: false, problems: ['Saving failed. Please try again.'] },
        );
      },
    });
  }

  /** The draft in the API's shape. END sections never send options, and an unset consequence is left out. */
  private toRequest(): NewBook {
    const d = this.draft;
    return {
      title: d.title,
      author: d.author,
      difficulty: d.difficulty,
      sections: d.sections.map((s) => ({
        id: s.id,
        type: s.type,
        text: s.text,
        ...(s.type === 'END'
          ? {}
          : {
              options: s.options.map((o) => ({
                description: o.description,
                gotoId: o.gotoId,
                ...(o.consequence.type
                  ? {
                      consequence: {
                        type: o.consequence.type,
                        value: o.consequence.value === null ? '' : String(o.consequence.value),
                        text: o.consequence.text,
                      },
                    }
                  : {}),
              })),
            }),
      })),
    };
  }
}

function newOption(gotoId: number | null): DraftOption {
  return { description: '', gotoId, consequence: { type: '', value: null, text: '' } };
}
