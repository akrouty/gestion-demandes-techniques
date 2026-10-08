import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  AgentAssignableResponse,
  ClientSummaryResponse,
  CreationDemandeRequest,
  DemandeDetailResponse,
  DemandeQuery,
  DemandeSummaryResponse,
  PageResponse,
  QualificationDemandeRequest,
  TraitementDemandeRequest,
} from './demande-models';
@Injectable({ providedIn: 'root' })
export class DemandesApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;
  list(query: DemandeQuery) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query))
      if (value !== undefined && value !== '') params = params.set(key, String(value));
    return this.http.get<PageResponse<DemandeSummaryResponse>>(this.base + '/demandes', { params });
  }
  detail(reference: string) {
    return this.http.get<DemandeDetailResponse>(this.url(reference));
  }
  create(body: CreationDemandeRequest) {
    return this.http.post<DemandeDetailResponse>(this.base + '/demandes', body);
  }
  clients(recherche: string, page = 0) {
    return this.http.get<PageResponse<ClientSummaryResponse>>(this.base + '/clients', {
      params: { recherche, page, size: 10, sort: 'nom,asc' },
    });
  }
  agents() {
    return this.http.get<AgentAssignableResponse[]>(this.base + '/agents');
  }
  qualify(reference: string, body: QualificationDemandeRequest) {
    return this.http.put<DemandeDetailResponse>(this.url(reference) + '/qualification', body);
  }
  assign(reference: string, agentId: number) {
    return this.http.put<DemandeDetailResponse>(this.url(reference) + '/affectation', { agentId });
  }
  treatment(reference: string, body: TraitementDemandeRequest) {
    return this.http.patch<DemandeDetailResponse>(this.url(reference) + '/traitement', body);
  }
  start(reference: string) {
    return this.http.post<DemandeDetailResponse>(
      this.url(reference) + '/demarrage-traitement',
      null,
    );
  }
  resolve(reference: string) {
    return this.http.post<DemandeDetailResponse>(this.url(reference) + '/resolution', null);
  }
  close(reference: string) {
    return this.http.post<DemandeDetailResponse>(this.url(reference) + '/cloture', null);
  }
  refuse(reference: string) {
    return this.http.post<DemandeDetailResponse>(this.url(reference) + '/refus-resolution', null);
  }
  cancel(reference: string, motif: string) {
    return this.http.post<DemandeDetailResponse>(this.url(reference) + '/annulation', { motif });
  }
  private url(reference: string) {
    return this.base + '/demandes/' + encodeURIComponent(reference);
  }
}
