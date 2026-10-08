import { DOCUMENT } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { map } from 'rxjs';
import { SessionService } from '../../session/session';
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatSidenavModule, MatButtonModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly document = inject(DOCUMENT);
  focusContent(event: Event): void {
    event.preventDefault();
    this.document.getElementById('main-content')?.focus();
  }
  readonly session = inject(SessionService);
  readonly mobile = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 767px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );
  readonly drawerOpen = signal(false);
  closeDrawer(): void {
    this.drawerOpen.set(false);
  }
}
