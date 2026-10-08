import { Component, input } from '@angular/core';
@Component({
  selector: 'app-page-state',
  templateUrl: './page-state.html',
  styleUrl: './page-state.scss',
})
export class PageState {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly kind = input<'empty' | 'loading' | 'error' | 'success'>('empty');
}
