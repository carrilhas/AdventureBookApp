import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { BooksApi, DIFFICULTIES, PlayerApi } from '../../api';

/**
 * Book list with search and filters. The filters live in the URL (?q=&author=&difficulty=), so a filtered
 * list can be bookmarked and the back button works.
 */
@Component({
  selector: 'app-book-list',
  imports: [RouterLink],
  templateUrl: './book-list.html',
})
export class BookList {
  private readonly booksApi = inject(BooksApi);
  private readonly playerApi = inject(PlayerApi);
  private readonly router = inject(Router);

  /** Bound from the query string by withComponentInputBinding(). */
  readonly q = input<string>();
  readonly author = input<string>();
  readonly difficulty = input<string>();

  protected readonly difficulties = DIFFICULTIES;
  protected readonly filtering = computed(() => !!(this.q() || this.author() || this.difficulty()));

  protected readonly player = rxResource({
    stream: () => this.playerApi.me().pipe(catchError(() => of(null))),
  });

  protected readonly authors = rxResource({
    stream: () => this.booksApi.authors(),
  });

  protected readonly books = rxResource({
    params: () => ({ q: this.q(), author: this.author(), difficulty: this.difficulty() }),
    stream: ({ params }) => this.booksApi.list(params),
  });

  protected applyFilters(event: SubmitEvent): void {
    event.preventDefault();
    const data = new FormData(event.target as HTMLFormElement);
    const value = (key: string) => (data.get(key) as string | null)?.trim() || null;
    this.router.navigate([], {
      queryParams: { q: value('q'), author: value('author'), difficulty: value('difficulty') },
    });
  }
}
