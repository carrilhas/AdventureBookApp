import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BookValidation, BooksApi, NewBook } from '../../api';
import { NewBookPage } from './new-book';

describe('NewBookPage', () => {
  let verdict: BookValidation;
  let validate: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.useFakeTimers();
    verdict = { valid: false, problems: ['title is required'] };
    validate = vi.fn((_: NewBook) => of(verdict));
    create = vi.fn((_: NewBook) => of({ id: 'my-book', title: 'My Book', author: 'Me', difficulty: 'EASY', inProgress: false }));

    await TestBed.configureTestingModule({
      imports: [NewBookPage],
      providers: [provideRouter([]), { provide: BooksApi, useValue: { validate, create } }],
    }).compileComponents();
  });

  afterEach(() => vi.useRealTimers());

  async function render() {
    const fixture = TestBed.createComponent(NewBookPage);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(500); // past the debounce
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const saveButton = () => el.querySelector<HTMLButtonElement>('.save-bar button')!;
    return { fixture, el, saveButton };
  }

  it('asks the server to check the starting draft and shows its problems', async () => {
    const { el, saveButton } = await render();
    expect(validate).toHaveBeenCalledTimes(1);
    expect(el.querySelector('.problems')?.textContent).toContain('title is required');
    expect(saveButton().disabled).toBe(true);
  });

  it('enables Save only when the server says the book is valid, then saves and opens it', async () => {
    verdict = { valid: true, problems: [] };
    const { el, saveButton } = await render();
    expect(el.querySelector('.ok')).not.toBeNull();
    expect(saveButton().disabled).toBe(false);

    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    saveButton().click();
    expect(create).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/play', 'my-book']);
  });

  it('re-checks after an edit, once typing pauses, and disables Save meanwhile', async () => {
    verdict = { valid: true, problems: [] };
    const { fixture, el, saveButton } = await render();

    const title = el.querySelector<HTMLInputElement>('input[type=text]')!;
    for (const value of ['M', 'My', 'My Book']) {
      title.value = value;
      title.dispatchEvent(new Event('input', { bubbles: true }));
    }
    fixture.detectChanges();
    expect(saveButton().disabled).toBe(true); // checking

    await vi.advanceTimersByTimeAsync(500);
    fixture.detectChanges();
    expect(validate).toHaveBeenCalledTimes(2); // one check for the three keystrokes
    expect(validate.mock.lastCall![0].title).toBe('My Book');
    expect(saveButton().disabled).toBe(false);
  });

  it('sends no options for END sections and leaves out unset consequences', async () => {
    await render();
    const sent: NewBook = validate.mock.lastCall![0];
    expect(sent.sections[0].options).toEqual([{ description: '', gotoId: 2 }]);
    expect(sent.sections[1].type).toBe('END');
    expect(sent.sections[1].options).toBeUndefined();
  });
});
