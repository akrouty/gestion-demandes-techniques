import { FormControl, FormGroup, Validators } from '@angular/forms';
import { vi } from 'vitest';
import { focusInvalid } from './feedback-focus';

describe('Focus des erreurs', () => {
  let root: HTMLElement;
  beforeEach(() => {
    root = document.createElement('div');
    document.body.append(root);
  });
  afterEach(() => {
    root.remove();
    vi.restoreAllMocks();
  });
  it('ignore les champs cachés et désactivés, cible un champ imbriqué hors écran', async () => {
    root.innerHTML =
      '<form><input formControlName="hidden" hidden><input formControlName="disabled" disabled><div formGroupName="client"><input formControlName="email"></div></form>';
    const input = root.querySelector('[formControlName="email"]') as HTMLInputElement;
    vi.spyOn(input, 'getClientRects').mockReturnValue([{}] as unknown as DOMRectList);
    vi.spyOn(input, 'getBoundingClientRect').mockReturnValue({
      top: 2000,
      bottom: 2040,
      left: 0,
      right: 200,
    } as DOMRect);
    input.scrollIntoView = vi.fn();
    const form = new FormGroup({
      hidden: new FormControl('', Validators.required),
      disabled: new FormControl({ value: '', disabled: true }, Validators.required),
      client: new FormGroup({ email: new FormControl('', Validators.required) }),
    });
    focusInvalid(root, form);
    await new Promise((resolve) => setTimeout(resolve, 10));
    expect(document.activeElement).toBe(input);
    expect(input.scrollIntoView).toHaveBeenCalledWith(expect.objectContaining({ block: 'center' }));
  });
  it('ne défile pas si le champ est déjà visible', async () => {
    root.innerHTML = '<form><input formControlName="email"></form>';
    const input = root.querySelector('input')!;
    vi.spyOn(input, 'getClientRects').mockReturnValue([{}] as unknown as DOMRectList);
    vi.spyOn(input, 'getBoundingClientRect').mockReturnValue({
      top: 100,
      bottom: 140,
      left: 0,
      right: 200,
    } as DOMRect);
    input.scrollIntoView = vi.fn();
    focusInvalid(root, new FormGroup({ email: new FormControl('', Validators.required) }));
    await new Promise((resolve) => setTimeout(resolve, 10));
    expect(document.activeElement).toBe(input);
    expect(input.scrollIntoView).not.toHaveBeenCalled();
  });
});
