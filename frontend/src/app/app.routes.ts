import { Routes } from '@angular/router';
import { BookList } from './pages/book-list/book-list';
import { Register } from './pages/register/register';
import { Play } from './pages/play/play';

export const routes: Routes = [
  { path: '', component: BookList, title: 'Adventure Books' },
  { path: 'register', component: Register, title: 'Register' },
  { path: 'play/:bookId', component: Play, title: 'Play' },
  { path: '**', redirectTo: '' },
];
