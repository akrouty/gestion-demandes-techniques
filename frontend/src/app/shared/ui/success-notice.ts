import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class SuccessNotice {
  private readonly snackBar = inject(MatSnackBar);
  show(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 6000,
      politeness: 'polite',
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: 'success-notice',
    });
  }
}
