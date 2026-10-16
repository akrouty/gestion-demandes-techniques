import { AbstractControl } from '@angular/forms';

// Attendre le rendu des mat-error et la réactivation des contrôles après la réponse API.
export function focusFeedback(root: HTMLElement, selector: string): void {
  setTimeout(() => {
    if (root.isConnected) reveal(root.querySelector<HTMLElement>(selector));
  });
}

export function focusInvalid(
  root: HTMLElement,
  form: AbstractControl,
  selector = 'form',
  fallback?: string,
): void {
  setTimeout(() => {
    if (!root.isConnected) return;
    const scope = root.querySelector<HTMLElement>(selector);
    for (const element of scope?.querySelectorAll<HTMLElement>(
      '[formControlName], [data-error-control]',
    ) ?? []) {
      const path = [element.getAttribute('formControlName') ?? element.dataset['errorControl']!];
      let parent = element.parentElement;
      while (parent && parent !== scope) {
        const group = parent.getAttribute('formGroupName');
        if (group) path.unshift(group);
        parent = parent.parentElement;
      }
      if (!form.get(path)?.invalid) continue;
      const target = element.matches('mat-checkbox, [formGroupName]')
        ? element.querySelector<HTMLElement>('input:not(:disabled)')
        : element;
      if (reveal(target)) return;
    }
    if (fallback) reveal(root.querySelector<HTMLElement>(fallback));
  });
}

function reveal(element: HTMLElement | null): boolean {
  if (
    !element ||
    element.closest('[hidden], [inert]') ||
    element.matches(':disabled, [aria-disabled="true"]')
  )
    return false;
  const view = element.ownerDocument.defaultView;
  if (
    !view ||
    view.getComputedStyle(element).visibility === 'hidden' ||
    !element.getClientRects().length
  )
    return false;
  const rect = element.getBoundingClientRect();
  if (
    rect.top < 16 ||
    rect.bottom > view.innerHeight - 16 ||
    rect.left < 0 ||
    rect.right > view.innerWidth
  ) {
    element.scrollIntoView?.({
      behavior: view.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
      block: 'center',
      inline: 'nearest',
    });
  }
  element.focus({ preventScroll: true });
  return element.ownerDocument.activeElement === element;
}
