import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="site-header">
      <a routerLink="/" class="brand">Adventure Books</a>
    </header>
    <main>
      <router-outlet />
    </main>
  `,
})
export class App {}
