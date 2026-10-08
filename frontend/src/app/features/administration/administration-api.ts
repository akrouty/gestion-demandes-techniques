import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  CreationUtilisateurRequest,
  ModificationUtilisateurRequest,
  PageUtilisateursResponse,
  RolesMetierRequest,
  UtilisateurDetailResponse,
  UtilisateurQuery,
} from './utilisateur-models';
@Injectable({ providedIn: 'root' })
export class AdministrationApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl + '/utilisateurs';
  lister(query: UtilisateurQuery) {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.sort) params = params.set('sort', query.sort);
    return this.http.get<PageUtilisateursResponse>(this.base, { params });
  }
  obtenir(id: number) {
    return this.http.get<UtilisateurDetailResponse>(this.base + '/' + id);
  }
  creer(body: CreationUtilisateurRequest) {
    return this.http.post<UtilisateurDetailResponse>(this.base, body);
  }
  modifier(id: number, body: ModificationUtilisateurRequest) {
    return this.http.put<UtilisateurDetailResponse>(this.base + '/' + id, body);
  }
  activer(id: number) {
    return this.http.post<UtilisateurDetailResponse>(this.base + '/' + id + '/activation', null);
  }
  desactiver(id: number) {
    return this.http.post<UtilisateurDetailResponse>(this.base + '/' + id + '/desactivation', null);
  }
  remplacerRolesMetier(id: number, body: RolesMetierRequest) {
    return this.http.put<UtilisateurDetailResponse>(this.base + '/' + id + '/roles-metier', body);
  }
}
