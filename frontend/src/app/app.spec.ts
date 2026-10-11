import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { Shell } from './core/layout/shell/shell';
import { SessionService } from './core/session/session';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of } from 'rxjs';
import { vi } from 'vitest';
describe('App', () => {
  it('affiche le point d’entrée du routeur', () => {
    TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('router-outlet')).not.toBeNull();
  });
});

describe('Shell F4 : navigation accessible', () => {
  function setup(
    mobile: boolean,
    roles: ('ADMINISTRATEUR' | 'RESPONSABLE_TECHNIQUE')[] = ['ADMINISTRATEUR'],
  ) {
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        {
          provide: BreakpointObserver,
          useValue: { observe: () => of({ matches: mobile, breakpoints: {} }) },
        },
      ],
    });
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nom: 'Administrateur', email: 'admin@example.test', roles },
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    return fixture;
  }
  it('le libellé du menu mobile suit son état ouvert et fermé', () => {
    const fixture = setup(true);
    const menu = fixture.nativeElement.querySelector('.menu-button') as HTMLButtonElement;
    expect(menu.getAttribute('aria-expanded')).toBe('false');
    expect(menu.getAttribute('aria-label')).toBe('Ouvrir le menu de navigation');
    menu.click();
    fixture.detectChanges();
    expect(menu.getAttribute('aria-expanded')).toBe('true');
    expect(menu.getAttribute('aria-label')).toBe('Fermer le menu de navigation');
    menu.click();
    fixture.detectChanges();
    expect(menu.getAttribute('aria-expanded')).toBe('false');
  });
  it('le lien de saut donne effectivement le focus au contenu', () => {
    const fixture = setup(false);
    const main = fixture.nativeElement.querySelector('#main-content') as HTMLElement;
    const focus = vi.spyOn(main, 'focus');
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });
    fixture.nativeElement.querySelector('.skip-link').dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    expect(focus).toHaveBeenCalled();
  });
  it('ADM seul affiche administration et le cumul conserve les deux sections', () => {
    const fixture = setup(false);
    expect(
      fixture.nativeElement.querySelector('a[href="/administration/utilisateurs"]'),
    ).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/demandes"]')).toBeNull();
    TestBed.inject(SessionService).establish({
      accessToken: 'test',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: {
        id: 1,
        nom: 'Admin RT',
        email: 'admin@example.test',
        roles: ['ADMINISTRATEUR', 'RESPONSABLE_TECHNIQUE'],
      },
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a[href="/demandes"]')).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector('a[href="/administration/utilisateurs"]'),
    ).not.toBeNull();
  });
});
