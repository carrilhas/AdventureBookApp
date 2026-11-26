import { Component, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PlayerApi, apiError } from '../../api';

/** Registers (or signs back in) a player by name, then returns to ?next= or the book list. */
@Component({
  selector: 'app-register',
  imports: [RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private readonly playerApi = inject(PlayerApi);
  private readonly router = inject(Router);

  /** Where to go after registering; only app-internal paths are honoured. */
  readonly next = input<string>();

  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected submit(event: SubmitEvent): void {
    event.preventDefault();
    const name = (new FormData(event.target as HTMLFormElement).get('name') as string | null)?.trim() ?? '';
    if (!name) {
      this.error.set('Please enter a name.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.playerApi.register(name).subscribe({
      next: () => {
        const next = this.next();
        const target = next && next.startsWith('/') && !next.startsWith('//') ? next : '/';
        this.router.navigateByUrl(target);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(apiError(err)?.message ?? 'Registration failed. Please try again.');
      },
    });
  }
}
