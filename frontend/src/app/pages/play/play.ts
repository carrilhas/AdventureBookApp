import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { GameApi, PlayState, apiError } from '../../api';

/**
 * Plays a book. The server holds all game rules; this page only shows the current section and sends choices.
 */
@Component({
  selector: 'app-play',
  imports: [RouterLink],
  templateUrl: './play.html',
})
export class Play {
  private readonly gameApi = inject(GameApi);
  private readonly router = inject(Router);

  /** Bound from the :bookId route parameter. */
  readonly bookId = input.required<string>();

  protected readonly game = rxResource({
    params: () => this.bookId(),
    stream: ({ params }) => this.gameApi.current(params),
  });

  /** An error from the last choice (e.g. a stale choice); the board is resynced from the server. */
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);

  protected readonly healthPercent = computed(() => {
    const s = this.game.value();
    return s ? Math.round((100 * s.health) / s.maxHealth) : 0;
  });

  protected readonly loadError = computed(() => {
    const err = this.game.error();
    if (!err) {
      return null;
    }
    const status = err instanceof HttpErrorResponse ? err.status : 0;
    return status === 404 ? 'That book could not be found.' : "Couldn't load the game. Is the server running?";
  });

  constructor() {
    // Not registered: send the player to the register page, then bring them back here.
    effect(() => {
      const err = this.game.error();
      if (err instanceof HttpErrorResponse && err.status === 401) {
        this.goRegister();
      }
    });
  }

  protected choose(state: PlayState, optionIndex: number): void {
    this.run(this.gameApi.choose(state.bookId, state.sectionId, optionIndex));
  }

  protected restart(): void {
    this.run(this.gameApi.restart(this.bookId()));
  }

  private run(request: Observable<PlayState>): void {
    this.busy.set(true);
    this.error.set(null);
    request.subscribe({
      next: (state) => {
        this.game.set(state);
        this.busy.set(false);
        window.scrollTo({ top: 0 });
      },
      error: (err) => {
        this.busy.set(false);
        if (err instanceof HttpErrorResponse && err.status === 401) {
          this.goRegister();
          return;
        }
        const body = apiError(err);
        this.error.set(body?.message ?? 'Something went wrong. Please try again.');
        if (body?.state) {
          this.game.set(body.state);
        }
      },
    });
  }

  private goRegister(): void {
    this.router.navigate(['/register'], { queryParams: { next: `/play/${this.bookId()}` } });
  }
}
